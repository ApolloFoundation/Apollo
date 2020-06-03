/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountProperty;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apl.core.app.CollectionUtil.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

@EnableWeld
class AccountPropertyTableTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(DbTestData.getInMemDbProps(), "db/acc-data.sql", "db/schema.sql");
    @Inject
    AccountPropertyTable table;
    AccountTestData testData;
    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, AccountPropertyTable.class
    )
        .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(dbExtension.getDatabaseManager().getJdbi(), Jdbi.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(blockchainProcessor, BlockchainProcessor.class, BlockchainProcessorImpl.class))
        .addBeans(MockBean.of(mock(FullTextConfig.class), FullTextConfig.class, FullTextConfigImpl.class))
        .addBeans(MockBean.of(mock(DerivedTablesRegistry.class), DerivedTablesRegistry.class, DerivedDbTablesRegistryImpl.class))
        .build();

    @BeforeEach
    void setUp() {
        testData = new AccountTestData();
    }

    @Test
    void testLoad() {
        AccountProperty accountProperty = table.get(table.getDbKeyFactory().newKey(testData.ACC_PROP_2));
        assertNotNull(accountProperty);
        assertEquals(testData.ACC_PROP_2, accountProperty);
    }

    @Test
    void testSave_insert_new_entity() {//SQL MERGE -> INSERT
        AccountProperty previous = table.get(table.getDbKeyFactory().newKey(testData.newProperty));
        assertNull(previous);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(testData.newProperty));
        AccountProperty actual = table.get(table.getDbKeyFactory().newKey(testData.newProperty));
        testData.newProperty.setDbId(actual.getDbId());

        assertNotNull(actual);
        assertEquals(testData.newProperty, actual);
    }

    @Test
    void testSave_update_existing_entity() {//SQL MERGE -> UPDATE
        AccountProperty previous = table.get(table.getDbKeyFactory().newKey(testData.ACC_PROP_0));
        assertNotNull(previous);
        String value = "GrandRotana";
        previous.setValue(value);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(previous));
        AccountProperty actual = table.get(table.getDbKeyFactory().newKey(testData.ACC_PROP_0));

        assertNotNull(actual);
        assertEquals(value, actual.getValue());
        assertEquals(previous.getProperty(), actual.getProperty());
    }


    @Test
    void getProperties() {
        List<AccountProperty> expected = List.of(testData.ACC_PROP_0, testData.ACC_PROP_4, testData.ACC_PROP_8)
            .stream().sorted(Comparator.comparing(AccountProperty::getProperty)).collect(Collectors.toList());
        List<AccountProperty> actual = toList(table.getProperties(testData.ACC_PROP_0.getRecipientId(), 0, null, 0, Integer.MAX_VALUE));
        assertEquals(expected, actual);
    }

    @Test
    void getProperty() {
        AccountProperty expected = testData.ACC_PROP_6;
        AccountProperty actual = table.getProperty(testData.ACC_PROP_6.getRecipientId(),
            testData.ACC_PROP_6.getProperty(),
            testData.ACC_PROP_6.getSetterId());
        assertEquals(expected, actual);
    }
}