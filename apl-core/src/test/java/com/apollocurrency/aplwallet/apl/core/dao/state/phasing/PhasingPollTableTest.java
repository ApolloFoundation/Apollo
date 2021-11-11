/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.phasing;

import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTableTest;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.TransactionEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.model.TransactionDbInfo;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.PhasingTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.enterprise.event.Event;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;


@Tag("slow")
public class PhasingPollTableTest extends EntityDbTableTest<PhasingPoll> {

    @RegisterExtension
    DbExtension extension = new DbExtension(mariaDBContainer, DbTestData.getDbUrlProps(), "db/phasing-poll-data.sql", "db/schema.sql");

    private final TransactionModelToEntityConverter toEntityConverter = new TransactionModelToEntityConverter();
    PhasingPollTable table;

    PhasingTestData ptd;
    TransactionTestData ttd;

    public PhasingPollTableTest() {
        super(PhasingPoll.class);
    }

    @BeforeEach
    @Override
    public void setUp() {
        table = new PhasingPollTable(getDatabaseManager(), new TransactionEntityRowMapper(), mock(Event.class));
        ptd = new PhasingTestData();
        ttd = new TransactionTestData();
        super.setUp();
    }

    @Override
    public DerivedDbTable<PhasingPoll> getDerivedDbTable() {
        return table;
    }

    @Override
    public DatabaseManager getDatabaseManager() {
        return extension.getDatabaseManager();
    }

    @Override
    protected List<PhasingPoll> getAll() {
        return List.of(ptd.POLL_0, ptd.POLL_1, ptd.POLL_2, ptd.POLL_35, ptd.POLL_3, ptd.POLL_4, ptd.POLL_55, ptd.POLL_5);
    }


    @Override
    public PhasingPoll valueToInsert() {
        return ptd.NEW_POLL;
    }

    @Test
    @Override
    public void testTrimForZeroHeight() {
        testTrim(0, Integer.MAX_VALUE);
    }

    @Override
    public void testTrim(int height, int blockchainHeight) {
        table.trim(height);
        List<PhasingPoll> actual = CollectionUtil.toList(table.getAll(Integer.MIN_VALUE, Integer.MAX_VALUE));
        List<PhasingPoll> expected = sortByHeightDesc(getAll().stream().filter(p -> {
            try {
                return p.getFinishHeight() >= height || p.getFinishTime() > table.blockTimestamp(height - 1);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }).collect(Collectors.toList()));
        assertEquals(expected, actual);
    }

    @Test
    void testTrimOnNextBlockAfterPoll4Finished() {
        testTrim(ptd.POLL_4.getFinishHeight() + 1, ptd.POLL_5.getFinishHeight() + 1);

        String condition = "transaction_id IN " + String.format("(%d,%d,%d)", ptd.POLL_0.getId(), ptd.POLL_1.getId(), ptd.POLL_2.getId());
        assertNotExistEntriesInTableForCondition("phasing_poll_voter", condition);
        assertNotExistEntriesInTableForCondition("phasing_vote", condition);
        assertNotExistEntriesInTableForCondition("phasing_poll_linked_transaction", condition);
    }

    @Test
    void testGetFinishingTransactions() {
        List<TransactionEntity> finishingTransactions = table.getFinishingTransactions(ptd.POLL_2.getFinishHeight());

        TransactionEntity expectedEntity = toEntityConverter.apply(ttd.TRANSACTION_7);
        expectedEntity.setDbId(2500L);
        assertEquals(Collections.singletonList(expectedEntity), finishingTransactions);
    }


    @Test
    void testGetFinishingTransactionsWhenNoTransactionsAtHeight() {
        List<TransactionEntity> finishingTransactions = table.getFinishingTransactions(ttd.TRANSACTION_0.getHeight() - 1);

        assertTrue(finishingTransactions.isEmpty(), "No transactions should be found at height");
    }

    @Test
    void testGetActivePhasingDbIds() throws SQLException {
        List<TransactionDbInfo> transactionDbInfoList = table.getActivePhasedTransactionDbIds(ttd.TRANSACTION_8.getHeight() + 1);

        assertEquals(Arrays.asList(new TransactionDbInfo(ttd.DB_ID_8, ttd.TRANSACTION_8.getId()), new TransactionDbInfo(ttd.DB_ID_7, ttd.TRANSACTION_7.getId())), transactionDbInfoList);
    }

    @Test
    void testGetActivePhasingDbIdWhenHeightIsMax() throws SQLException {
        List<TransactionDbInfo> transactionDbInfoList = table.getActivePhasedTransactionDbIds(553327);

        assertEquals(Collections.emptyList(), transactionDbInfoList);
    }

    @Test
    void testGetActivePhasingDbIdAllPollsFinished() throws SQLException {
        List<TransactionDbInfo> transactionDbInfoList = table.getActivePhasedTransactionDbIds(ptd.POLL_0.getHeight() - 1);

        assertEquals(Collections.emptyList(), transactionDbInfoList);
    }

    @Test
    void testGetActivePhasingDbIdsWhenNoPollsAtHeight() throws SQLException {
        List<TransactionDbInfo> transactionDbInfoList = table.getActivePhasedTransactionDbIds(ttd.TRANSACTION_0.getHeight());

        assertEquals(Collections.emptyList(), transactionDbInfoList);
    }

    @Test
    void testGetAllPhasedTransactionsCount() throws SQLException {
        int count = table.getAllPhasedTransactionsCount();

        assertEquals(ptd.NUMBER_OF_PHASED_TRANSACTIONS, count);
    }

    @Test
    void testGetAccountPhasedTransactionsCountAtLastBlockHeight() throws SQLException {
        int count = table.getAccountPhasedTransactionCount(ttd.TRANSACTION_0.getSenderId(), ptd.POLL_5.getHeight());

        assertEquals(1, count);
    }

    @Test
    void testGetAccountPhasedTransactionsCountAtGenesisBlockHeight() throws SQLException {
        int count = table.getAccountPhasedTransactionCount(ttd.TRANSACTION_0.getSenderId(), 0);

        assertEquals(3, count);
    }

    @Test
    void testGetNonExistentAccountPhasedTransactionCount() throws SQLException {
        int count = table.getAccountPhasedTransactionCount(ttd.TRANSACTION_0.getSenderId() + 1, ptd.POLL_5.getHeight());

        assertEquals(0, count);
    }

    @Test
    void testGetByHoldingId() throws SQLException {
        List<TransactionEntity> transactions = table.getHoldingPhasedTransactions(ptd.POLL_5.getVoteWeighting().getHoldingId(), VoteWeighting.VotingModel.ASSET, 0, false, 0, 100, ptd.POLL_5.getHeight());

        TransactionEntity expectedEntity = toEntityConverter.apply(ttd.TRANSACTION_13);
        expectedEntity.setDbId(6000);
        assertEquals(List.of(expectedEntity), transactions);
    }

    @Test
    void testGetByHoldingIdNotExist() throws SQLException {
        List<TransactionEntity> transactions = table.getHoldingPhasedTransactions(ptd.POLL_4.getVoteWeighting().getHoldingId(), VoteWeighting.VotingModel.ACCOUNT, 0, false, 0, 100, ptd.POLL_5.getHeight());

        assertTrue(transactions.isEmpty());
    }

    @Test
    void testIsTransactionPhasedForPollId() throws SQLException {
        boolean phased = table.isTransactionPhased(ptd.POLL_0.getId());

        assertTrue(phased);
    }

    @Test
    void testIsTransactionPhasedForPollResult() throws SQLException {
        boolean phased = table.isTransactionPhased(ptd.RESULT_0.getId());

        assertTrue(phased);
    }

    @Test
    void testIsTransactionPhasedForOrdinaryTransaction() throws SQLException {
        boolean phased = table.isTransactionPhased(ttd.TRANSACTION_1.getId());

        assertFalse(phased);
    }

    @Test
    void testGetAccountPhasedTransactionsWithPaginationSkipFirstAtLastBlockHeight() {
        List<TransactionEntity> transactions = table.getAccountPhasedTransactions(ptd.POLL_0.getAccountId(), 1, 2, ptd.POLL_5.getHeight() - 1);

        assertTrue(transactions.isEmpty());
    }

    @Test
    void testGetAccountPhasedTransactionsWithPaginationSkipFirstAtGenesisBlockHeight() {
        List<TransactionEntity> transactions = table.getAccountPhasedTransactions(ptd.POLL_0.getAccountId(), 1, 2, 0);
        List<TransactionEntity> expectedEntities = toEntityConverter.convert(List.of(ttd.TRANSACTION_12, ttd.TRANSACTION_11));
        expectedEntities.get(0).setDbId(5000L);
        expectedEntities.get(1).setDbId(4500L);
        assertEquals(expectedEntities, transactions);
    }

    @Test
    void testGetAllAccountPhasedTransactionsWithPagination() {
        List<TransactionEntity> transactions = table.getAccountPhasedTransactions(ptd.POLL_0.getAccountId(), 0, 100, 0);
        List<TransactionEntity> expectedEntities = toEntityConverter.convert(List.of(ttd.TRANSACTION_13, ttd.TRANSACTION_12, ttd.TRANSACTION_11));
        expectedEntities.get(0).setDbId(6000);
        expectedEntities.get(1).setDbId(5000);
        expectedEntities.get(2).setDbId(4500);
        assertEquals(expectedEntities, transactions);
    }

    @Test
    void testGetSenderPhasedTransactionFees() throws SQLException {
        long actualFee = table.getSenderPhasedTransactionFees(ptd.POLL_0.getAccountId(), 0);
        long expectedFee = ttd.TRANSACTION_13.getFeeATM() + ttd.TRANSACTION_12.getFeeATM() + ttd.TRANSACTION_11.getFeeATM();
        assertEquals(expectedFee, actualFee);
    }

    @Test
    void testGetSenderPhasedTransactionFeesAtLastPollHeight() throws SQLException {
        long actualFee = table.getSenderPhasedTransactionFees(ptd.POLL_0.getAccountId(), ptd.POLL_5.getHeight());
        long expectedFee = ttd.TRANSACTION_13.getFeeATM();
        assertEquals(expectedFee, actualFee);
    }

    @Test
    void testGetSenderPhasedTransactionFeesForNonExistentAccount() throws SQLException {
        long actualFee = table.getSenderPhasedTransactionFees(1, 0);
        assertEquals(0, actualFee);
    }

    @Test
    void testGetById() {
        PhasingPoll phasingPoll = table.get(ptd.POLL_2.getId());
        assertEquals(ptd.POLL_2, phasingPoll);
    }

    @Test
    void testGetByIdNotExist() {
        PhasingPoll phasingPoll = table.get(1);
        assertNull(phasingPoll);
    }

    @Override
    @Test
    public void testInsertAlreadyExist() {
        PhasingPoll value = ptd.POLL_1;
        Assertions.assertThrows(RuntimeException.class, () -> DbUtils.inTransaction(getDatabaseManager(), (con) -> {
            table.insert(value);
        }));
    }

    private void assertNotExistEntriesInTableForCondition(String tableName, String condition) {
        DbUtils.inTransaction(getDatabaseManager(), (con) -> {
            try (Statement stmt = con.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT 1 FROM " + tableName + " where  " + condition)) {
                    Assertions.assertFalse(rs.next());
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        });
    }

}
