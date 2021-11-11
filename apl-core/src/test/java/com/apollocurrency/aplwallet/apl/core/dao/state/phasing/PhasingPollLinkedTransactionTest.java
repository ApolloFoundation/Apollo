/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.phasing;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.ValuesDbTableTest;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPollLinkedTransaction;
import com.apollocurrency.aplwallet.apl.data.PhasingTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import javax.enterprise.event.Event;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@Tag("slow")
public class PhasingPollLinkedTransactionTest extends ValuesDbTableTest<PhasingPollLinkedTransaction> {

    TransactionTestData td = new TransactionTestData();
    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer);


    PhasingPollLinkedTransactionTable table;
    PhasingTestData ptd;
    TransactionTestData ttd;

    public PhasingPollLinkedTransactionTest() {
        super(PhasingPollLinkedTransaction.class);
    }

    @BeforeEach
    @Override
    public void setUp() {
        table = new PhasingPollLinkedTransactionTable(extension.getDatabaseManager(), Mockito.mock(Event.class));
        ptd = new PhasingTestData();
        ttd = new TransactionTestData();
        super.setUp();
    }

    @Override
    public DerivedDbTable<PhasingPollLinkedTransaction> getDerivedDbTable() {
        return table;
    }

    @Override
    public DatabaseManager getDatabaseManager() {
        return extension.getDatabaseManager();
    }

    @Test
    void testGetAllForPollWithLinkedTransactions() {
        List<PhasingPollLinkedTransaction> linkedTransactions = table.get(ptd.POLL_3.getId());

        assertEquals(Arrays.asList(ptd.LINKED_TRANSACTION_0, ptd.LINKED_TRANSACTION_1, ptd.LINKED_TRANSACTION_2), linkedTransactions);
    }

    @Test
    void testGetAllForPollWithoutLinkedTransactions() {
        List<PhasingPollLinkedTransaction> linkedTransactions = table.get(ptd.POLL_1.getId());

        assertTrue(linkedTransactions.isEmpty(), "Linked transactions should not exist for poll2");
    }

    @Test
    void testGetByDbKeyForPollWithLinkedTransactions() {
        List<PhasingPollLinkedTransaction> linkedTransactions = table.get(new LongKey(ptd.POLL_3.getId()));

        assertEquals(Arrays.asList(ptd.LINKED_TRANSACTION_0, ptd.LINKED_TRANSACTION_1, ptd.LINKED_TRANSACTION_2), linkedTransactions);
    }

    @Test
    void testGetByDbKeyForPollWithoutLinkedTransactions() {
        List<PhasingPollLinkedTransaction> linkedTransactions = table.get(new LongKey(ptd.POLL_1.getId()));

        assertTrue(linkedTransactions.isEmpty(), "Linked transactions should not exist for poll2");
    }

    @Test
    void testGetLinkedPhasedTransactions() throws SQLException {
        List<Long> transactions = table.getLinkedPhasedTransactionIds(ptd.LINKED_TRANSACTION_1_HASH);

        assertEquals(List.of(ttd.TRANSACTION_12.getId()), transactions);
    }

    @Test
    void testGetLinkedPhasedTransactionsForNonLinkedTransaction() throws SQLException {
        List<Long> transactions = table.getLinkedPhasedTransactionIds(ttd.TRANSACTION_12.getFullHash());

        assertTrue(transactions.isEmpty(), "Linked transactions should not exist for transaction #12");
    }


    @Override
    protected List<PhasingPollLinkedTransaction> getAll() {
        return List.of(ptd.LINKED_TRANSACTION_0, ptd.LINKED_TRANSACTION_1, ptd.LINKED_TRANSACTION_2, ptd.FAKE_LINKED_TRANSACTION_0, ptd.FAKE_LINKED_TRANSACTION_1, ptd.FAKE_LINKED_TRANSACTION_2);
    }


    @Override
    protected List<PhasingPollLinkedTransaction> dataToInsert() {
        return List.of(ptd.NEW_LINKED_TRANSACTION_1, ptd.NEW_LINKED_TRANSACTION_2);
    }

}
