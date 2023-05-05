/*
 * Copyright © 2018-2022 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.MinMaxValue;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import jakarta.enterprise.event.Event;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@Slf4j

@Tag("slow")
class AccountTableTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(mariaDBContainer, DbTestData.getInMemDbProps(), "db/acc-data.sql", "db/schema.sql");

    AccountTable table;
    AccountTestData td;

    @BeforeEach
    void setUp() {
        td = new AccountTestData();
        table = new AccountTable(dbExtension.getDatabaseManager(), mock(Event.class));
    }

    @AfterEach
    void tearDown() {
        dbExtension.cleanAndPopulateDb();
    }

    @Test
    void testLoad() {
        Account account = table.get(table.getDbKeyFactory().newKey(td.ACC_0));
        assertNotNull(account);
        assertEquals(td.ACC_0, account);
    }

    @Test
    void testLoad_ifNotExist_thenReturnNull() {
        Account account = table.get(table.getDbKeyFactory().newKey(td.newAccount));
        assertNull(account);
    }

    @Test
    void testSave() {
        DbUtils.inTransaction(dbExtension, (con) -> table.insert(td.newAccount));

        Account actual = table.get(table.getDbKeyFactory().newKey(td.newAccount));
        assertNotNull(actual);
        assertTrue(actual.getDbId() != 0);
        assertEquals(td.newAccount.getId(), actual.getId());
        assertEquals(td.newAccount.getBalanceATM(), actual.getBalanceATM());
    }

    @Test
    void save_existing() {
        td.ACC_14.setBalanceATM(td.ACC_14.getBalanceATM() + 100);
        td.ACC_14.setHeight(td.ACC_14.getHeight() + 1);
        long prevDbId = td.ACC_14.getDbId();

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(td.ACC_14));

        Account actual = table.get(table.getDbKeyFactory().newKey(td.ACC_14));
        assertNotNull(actual);
        assertEquals(td.ACC_14.getId(), actual.getId());
        assertEquals(td.ACC_14.getBalanceATM(), actual.getBalanceATM());
        assertEquals(td.ACC_14.getHeight(), actual.getHeight());
        Account prev = table.get(table.getDbKeyFactory().newKey(td.ACC_14), td.ACC_14.getHeight() - 1);
        // get back old data
        td.ACC_14.setHeight(td.ACC_14.getHeight() - 1);
        td.ACC_14.setBalanceATM(td.ACC_14.getBalanceATM() - 100);
        td.ACC_14.setDbId(prevDbId);
        assertEquals(td.ACC_14, prev);
    }

    @Test
    void testMerge() {
        td.ACC_14.setBalanceATM(1);
        td.ACC_14.setHeight(td.ACC_14.getHeight()); // set the same height to trigger merge

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(td.ACC_14));

        Account actual = table.get(table.getDbKeyFactory().newKey(td.ACC_14));
        assertNotNull(actual);
        assertTrue(actual.getDbId() != 0);
        assertEquals(td.ACC_14.getId(), actual.getId());
        assertEquals(td.ACC_14.getBalanceATM(), actual.getBalanceATM());
        assertEquals(td.ACC_14.getHeight(), actual.getHeight());
    }

    @Test
    void testTrim_on_0_height() throws SQLException {
        DbUtils.inTransaction(dbExtension, (con) -> table.trim(0));

        List<Account> expected = td.ALL_ACCOUNTS;
        List<Account> all = table.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();

        assertEquals(expected, all);
    }

    @Test
    void testTrim_on_MAX_height() throws SQLException {
        DbUtils.inTransaction(dbExtension, (con) -> table.trim(Integer.MAX_VALUE));

        List<Account> expected = td.ALL_ACCOUNTS.stream().filter(VersionedDerivedEntity::isLatest).collect(Collectors.toList());
        List<Account> all = table.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();

        assertEquals(expected, all);
    }

    @Test
    void getTotalSupply() {
        long total = table.getTotalSupply(td.CREATOR_ID);
        assertEquals(999990000000000L, total);
    }

    @Test
    void testDeleteAndTrim() throws SQLException {
        DbUtils.inTransaction(dbExtension, (con) -> {
            td.ACC_10.setHeight(td.ACC_10.getHeight() + 1);
            boolean deleted = table.deleteAtHeight(td.ACC_10, td.ACC_10.getHeight() + 1);
            assertTrue(deleted);
        });

        List<Account> accounts = table.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        int numberOfAccounts = accounts.size();
        assertEquals(17, numberOfAccounts);
        Account account = accounts.get(16);

        assertEquals(td.ACC_10.getId(), account.getId());
        assertTrue(account.isDeleted());
        assertFalse(account.isLatest());
        Account deletedPreviousAcc = accounts.get(11);
        assertEquals(deletedPreviousAcc.getId(), td.ACC_10.getId());
        assertTrue(deletedPreviousAcc.isDeleted());
        assertFalse(deletedPreviousAcc.isLatest());

        // Trim latest=false none of deleted record
        DbUtils.inTransaction(dbExtension, (con) -> table.trim(td.ACC_10.getHeight()));

        int afterTrimSize = table.getRowCount();
        assertEquals(14, afterTrimSize); // 1 updated id=700, 2 updated for id=500

        // Trim another deleted record for ACC_10
        DbUtils.inTransaction(dbExtension, (con) -> {
            table.trim(td.ACC_10.getHeight() + 1); // delete 'deleted' record
        });

        int afterDeleteTrimSize = table.getRowCount();
        assertEquals(12, afterDeleteTrimSize);
    }

    @Test
    void testRollbackDeleted() {
        DbUtils.inTransaction(dbExtension, (con) -> table.rollback(td.ACC_13.getHeight()));

        Account account = table.get(new LongKey(td.ACC_13.getId()));
        td.ACC_13.setLatest(true);
        td.ACC_13.setDeleted(false);
        assertEquals(td.ACC_13, account);
    }

    @Test
    void testRollback_deleted_no_updated() throws SQLException {
        td.ACC_14.setHeight(td.ACC_14.getHeight() + 1);
        td.ACC_14.setBalanceATM(td.ACC_14.getBalanceATM() - 100);
        long prevDbId = td.ACC_14.getDbId();

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(td.ACC_14));
        DbUtils.inTransaction(dbExtension, (con) -> table.rollback(td.ACC_14.getHeight() - 1));

        Account account = table.get(new LongKey(td.ACC_14.getId()));
        assertNull(account);
        List<Account> existing = table.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        td.ACC_14.setHeight(td.ACC_14.getHeight() - 1);
        td.ACC_14.setBalanceATM(td.ACC_14.getBalanceATM() + 100);
        td.ACC_14.setDbId(prevDbId);
        assertEquals(td.ALL_ACCOUNTS, existing);
    }

    @Test
    void testRollback_update_latest_for_prev_not_deleted() throws SQLException {
        Account newAcc1 = new Account(td.ACC_14.getId(), td.ACC_14.getBalanceATM() - 100, td.ACC_14.getUnconfirmedBalanceATM(), 0, 0, td.ACC_14.getHeight() + 1);
        DbUtils.inTransaction(dbExtension, (con) -> table.insert(newAcc1));

        Account newAcc2 = new Account(td.ACC_14.getId(), td.ACC_14.getBalanceATM() - 100, td.ACC_14.getUnconfirmedBalanceATM(), 0, 0, td.ACC_14.getHeight() + 2);
        newAcc2.setDbId(td.ACC_14.getDbId() + 2);
        DbUtils.inTransaction(dbExtension, (con) -> table.insert(newAcc2));

        DbUtils.inTransaction(dbExtension, (con) -> table.rollback(newAcc2.getHeight() - 1));

        Account account = table.get(new LongKey(td.ACC_14.getId()));

        assertEquals(newAcc1, account);
        List<Account> existing = table.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        ArrayList<Account> expected = new ArrayList<>(td.ALL_ACCOUNTS);
        expected.add(newAcc1);
        assertEquals(expected, existing);
    }


    @Test
    void getTopHolders() {
        List<Account> expected = td.ALL_ACCOUNTS.stream().filter(VersionedDerivedEntity::isLatest).collect(Collectors.toList());
        List<Account> result = table.getTopHolders(100);
        assertEquals(expected.size(), result.size());
        assertEquals(expected, result.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList()));
    }

    @Test
    void getTotalAmountOnTopAccounts() {
        long expected = td.ALL_ACCOUNTS.stream().filter(VersionedDerivedEntity::isLatest).mapToLong(Account::getBalanceATM).sum();
        long result = table.getTotalAmountOnTopAccounts(100);
        assertEquals(expected, result);
    }

    @Test
    void getTotalNumberOfAccounts() throws SQLException {
        long expected = td.ALL_ACCOUNTS.stream().filter(VersionedDerivedEntity::isLatest).count();
        long result = table.getTotalNumberOfAccounts();
        assertEquals(expected, result);
    }

    @Test
    void getRecent() {
        List<Account> recentAccounts = table.getRecentAccounts(2);

        assertEquals(List.of(td.ACC_10, td.ACC_9), recentAccounts);
    }

    @Test
    void selectForKey() throws SQLException {
        List<Account> accounts = table.selectAllForKey(td.ACC_11.getId());

        assertEquals(List.of(td.ACC_14, td.ACC_13, td.ACC_12, td.ACC_11), accounts);
    }

    @Test
    void getMinMaxValues_allValues() {
        MinMaxValue minMaxValue = table.getMinMaxValue(Integer.MAX_VALUE);

        assertEquals(new MinMaxValue(BigDecimal.valueOf(50), BigDecimal.valueOf(9211698109297098287L), "id", 16, 141868), minMaxValue);
    }

    @Test
    void getMinMaxValues_limitedByHeight() {
        MinMaxValue minMaxValue = table.getMinMaxValue(140000);

        assertEquals(new MinMaxValue(BigDecimal.valueOf(50), BigDecimal.valueOf(9211698109297098287L), "id", 6, 106000), minMaxValue);
    }
}