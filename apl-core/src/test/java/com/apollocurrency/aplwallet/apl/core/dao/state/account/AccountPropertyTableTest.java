/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.JdbiConfiguration;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountProperty;
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
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

@Slf4j

@Tag("slow")
@EnableWeld
class AccountPropertyTableTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(mariaDBContainer, DbTestData.getInMemDbProps(), "db/acc-data.sql", "db/schema.sql");
    @Inject
    AccountPropertyTable table;
    AccountTestData testData;
    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, AccountPropertyTable.class, JdbiHandleFactory.class, JdbiConfiguration.class
    )
        .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
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