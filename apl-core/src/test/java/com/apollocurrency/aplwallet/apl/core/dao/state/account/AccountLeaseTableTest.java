/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountLease;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@Slf4j
//
@Tag("slow")
@EnableWeld
class AccountLeaseTableTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(mariaDBContainer, DbTestData.getInMemDbConfig(), "db/schema.sql", "db/acc-data.sql");
    @Inject
    AccountLeaseTable table;
    AccountTestData testData = new AccountTestData();
    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, AccountLeaseTable.class
    )
        .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(blockchainProcessor, BlockchainProcessor.class, BlockchainProcessorImpl.class))
        .addBeans(MockBean.of(mock(FullTextConfig.class), FullTextConfig.class, FullTextConfigImpl.class))
        .addBeans(MockBean.of(mock(DerivedTablesRegistry.class), DerivedTablesRegistry.class, DerivedDbTablesRegistryImpl.class))
        .build();

    @Test
    void testSave_insert_new_entity() {//SQL MERGE -> INSERT
        AccountLease previous = table.get(table.getDbKeyFactory().newKey(testData.newLease));
        assertNull(previous);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(testData.newLease));
        AccountLease actual = table.get(table.getDbKeyFactory().newKey(testData.newLease));

        assertNotNull(actual);
        assertTrue(actual.getDbId() != 0);
        assertEquals(testData.newLease.getLessorId(), actual.getLessorId());
        assertEquals(testData.newLease.getCurrentLesseeId(), actual.getCurrentLesseeId());
    }

    @Test
    void testSave_update_existing_entity() {//SQL MERGE -> UPDATE
        AccountLease previous = table.get(table.getDbKeyFactory().newKey(testData.ACC_LEAS_0));
        assertNotNull(previous);
        previous.setCurrentLeasingHeightFrom(previous.getCurrentLeasingHeightFrom() + 50000);
        previous.setCurrentLeasingHeightTo(previous.getCurrentLeasingHeightFrom() + 10000);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(previous));
        AccountLease actual = table.get(table.getDbKeyFactory().newKey(previous));

        assertNotNull(actual);
        assertTrue(actual.getCurrentLeasingHeightFrom() - testData.ACC_LEAS_0.getCurrentLeasingHeightFrom() == 50000);
        assertTrue(actual.getCurrentLeasingHeightTo() - actual.getCurrentLeasingHeightFrom() == 10000);
        assertEquals(previous.getLessorId(), actual.getLessorId());
        assertEquals(previous.getCurrentLesseeId(), actual.getCurrentLesseeId());
    }


    @Test
    void getAccountLeaseCount() {
        dbExtension.cleanAndPopulateDb();

        int expected = testData.ALL_LEASE.size();
        expected--; //one record doesn't have 'latest' indicator;
        int actual = table.getAccountLeaseCount();
        assertEquals(expected, actual);
    }

    @Test
    void getLeaseByLessor() {
        AccountLease actual = table.get(new LongKey(testData.ACC_LEAS_4.getLessorId()));
        assertEquals(testData.ACC_LEAS_5, actual);
    }

    @Test
    void getLeaseChangingAccounts() {
        dbExtension.cleanAndPopulateDb();

        List<AccountLease> accounts = table.getLeaseChangingAccountsAtHeight(testData.ACC_LEAS_0.getHeight());
        List<AccountLease> expected = testData.ALL_LEASE.stream()
            .filter(accountLease -> accountLease.getCurrentLeasingHeightFrom() == testData.ACC_LEAS_0.getHeight()
                || accountLease.getCurrentLeasingHeightTo() == testData.ACC_LEAS_0.getHeight())
            .sorted(Comparator.comparing(AccountLease::getCurrentLesseeId).thenComparing(AccountLease::getLessorId))
            .collect(Collectors.toList());
        assertEquals(expected, accounts);
    }
}