/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.MinMaxDbId;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPollVoter;
import com.apollocurrency.aplwallet.apl.data.PhasingTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.sql.SQLException;
import java.util.List;
import javax.inject.Inject;

@EnableWeld
@Execution(ExecutionMode.CONCURRENT)
public class PhasingPollVoterTableTest {
    @RegisterExtension
    DbExtension extension = new DbExtension();
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
            JdbiHandleFactory.class,
            GlobalSyncImpl.class,
            PhasingPollVoterTable.class,
            FullTextConfigImpl.class,
            DerivedDbTablesRegistryImpl.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class)
            .addBeans(MockBean.of(extension.getDatabaseManger(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManger().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
            .build();
    @Inject
    PhasingPollVoterTable table;
    PhasingTestData ptd;
    TransactionTestData ttd;

    @Inject
    JdbiHandleFactory jdbiHandleFactory;

    @AfterEach
    void cleanup() {
        jdbiHandleFactory.close();
    }

    @BeforeEach
    void setUp() {
        ptd = new PhasingTestData();
        ttd = new TransactionTestData();
    }

    @Test
    void testGetVotersForPoll() {
        List<PhasingPollVoter> pollVoters = table.get(ptd.POLL_1.getId());

        assertEquals(List.of(ptd.POLL_1_VOTER_0, ptd.POLL_1_VOTER_1), pollVoters);
    }

    @Test
    void testGetVotersForPollWithoutVoters() {
        List<PhasingPollVoter> pollVoters = table.get(ptd.POLL_2.getId());

        assertTrue(pollVoters.isEmpty(), "Poll voters should not exist for poll2");
    }

    @Test
    void testGetAll() throws SQLException {
        List<PhasingPollVoter> all = table.getAllByDbId(new MinMaxDbId(0, Long.MAX_VALUE), 100).getValues();

        assertEquals(List.of(ptd.POLL_1_VOTER_0, ptd.POLL_1_VOTER_1, ptd.POLL_4_VOTER_0), all);
    }

    @Test
    void testInsert() throws SQLException {
        DbUtils.inTransaction(extension, (con) -> table.insert(List.of(ptd.NEW_VOTER_0, ptd.NEW_VOTER_1)));
        List<PhasingPollVoter> all = table.getAllByDbId(new MinMaxDbId(0, Long.MAX_VALUE), 100).getValues();
        assertEquals(List.of(ptd.POLL_1_VOTER_0, ptd.POLL_1_VOTER_1, ptd.POLL_4_VOTER_0, ptd.NEW_VOTER_0, ptd.NEW_VOTER_1), all);
        List<PhasingPollVoter> poll4Voters = table.get(ptd.POLL_4.getId());
        assertEquals(List.of(ptd.POLL_4_VOTER_0, ptd.NEW_VOTER_0, ptd.NEW_VOTER_1), poll4Voters);
    }

    @Test
    void testGetByDbKey() {
        List<PhasingPollVoter> pollVoters = table.get(new LongKey(ptd.POLL_4.getId()));

        assertEquals(List.of(ptd.POLL_4_VOTER_0), pollVoters);
    }

    @Test
    void testTruncate() throws SQLException {
        DbUtils.inTransaction(extension, (con)-> table.truncate());

        assertTrue(table.getAllByDbId(new MinMaxDbId(0, Long.MAX_VALUE), 100).getValues().isEmpty(), "Table should not have any entries after truncating");
    }

    @Test
    void testRollback() throws SQLException {
        DbUtils.inTransaction(extension, (con) -> table.rollback(ptd.POLL_1.getHeight()));

        assertEquals(List.of(ptd.POLL_1_VOTER_0, ptd.POLL_1_VOTER_1), table.getAllByDbId(new MinMaxDbId(0, Long.MAX_VALUE), 100).getValues());
    }

    @Test
    void testRollbackNothing() throws SQLException {
        DbUtils.inTransaction(extension, (con) -> table.rollback(ptd.POLL_4.getHeight()));

        assertEquals(List.of(ptd.POLL_1_VOTER_0, ptd.POLL_1_VOTER_1,  ptd.POLL_4_VOTER_0), table.getAllByDbId(new MinMaxDbId(0, Long.MAX_VALUE), 100).getValues());
    }

    @Test
    void testDeleteNotSupported() {
        assertThrows(UnsupportedOperationException.class, () -> table.delete(ptd.POLL_1_VOTER_1));
    }

    @Test
    void testInsertWithDifferentKeys() {
        assertThrows(IllegalArgumentException.class, () -> DbUtils.inTransaction(extension, (con) -> table.insert(List.of(ptd.NEW_VOTER_2, ptd.NEW_VOTER_0))));
    }

    @Test
    void testTrimNothing() throws SQLException {
        DbUtils.inTransaction(extension, (con) -> table.trim(ptd.POLL_4.getHeight()));

        assertEquals(List.of(ptd.POLL_1_VOTER_0, ptd.POLL_1_VOTER_1, ptd.POLL_4_VOTER_0), table.getAllByDbId(new MinMaxDbId(0, Long.MAX_VALUE), 100).getValues());
    }


}
