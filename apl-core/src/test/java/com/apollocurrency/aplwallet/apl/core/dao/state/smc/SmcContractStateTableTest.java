/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.smc;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractStateEntity;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.testutil.EntityProducer;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@EnableWeld
class SmcContractStateTableTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(mariaDBContainer, DbTestData.getInMemDbProps(), "db/smc-data.sql", "db/schema.sql");

    @Inject
    SmcContractStateTable table;

    long contractAddress = 7307657537262705518L;

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, EntityProducer.class, SmcContractStateTable.class
    )
        .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))

        .addBeans(MockBean.of(mock(FullTextConfig.class), FullTextConfig.class, FullTextConfigImpl.class))
        .addBeans(MockBean.of(mock(DerivedTablesRegistry.class), DerivedTablesRegistry.class, DerivedDbTablesRegistryImpl.class))
        .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessor.class, BlockchainProcessorImpl.class))
        .build();

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
        dbExtension.cleanAndPopulateDb();
    }


    @Test
    void load() {
        SmcContractStateEntity entity = table.get(SmcContractStateTable.KEY_FACTORY.newKey(contractAddress));
        assertNotNull(entity);
        assertEquals(contractAddress, entity.getAddress());
        assertEquals("{\"value\":1400000000,\"vendor\":\"APL-X5JH-TJKJ-DVGC-5T2V8\",\"customer\":\"\",\"paid\":false,\"accepted\":false}", entity.getSerializedObject());
    }

    @Test
    void save() {
        long contractAddress = 123L;
        SmcContractStateEntity entity = SmcContractStateEntity.builder()
            .address(contractAddress)
            .serializedObject("{\"value\":1590}")
            .status("ACTIVE")
            .height(12)
            .build();
        DbUtils.inTransaction(dbExtension, (con) -> table.insert(entity));

        SmcContractStateEntity actual = table.get(SmcContractStateTable.KEY_FACTORY.newKey(contractAddress));
        assertNotNull(actual);
        assertTrue(actual.getDbId() != 0);
        assertEquals(contractAddress, actual.getAddress());
    }

    @Test
    void update() {
        SmcContractStateEntity entity = table.get(SmcContractStateTable.KEY_FACTORY.newKey(contractAddress));
        assertNotNull(entity);
        assertEquals(contractAddress, entity.getAddress());

        String updatedSerializedObject = "{\"value\":1590}";
        entity.setSerializedObject(updatedSerializedObject);
        entity.setHeight(15);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(entity));

        SmcContractStateEntity actual = table.get(SmcContractStateTable.KEY_FACTORY.newKey(contractAddress));
        assertNotNull(actual);
        assertTrue(actual.getDbId() != 0);
        assertEquals(contractAddress, actual.getAddress());
        assertEquals(updatedSerializedObject, actual.getSerializedObject());

        assertEquals(1, table.getCount());
        assertEquals(2, table.getRowCount());
    }
}