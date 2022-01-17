/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.smc;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractMappingEntity;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Tag("slow")
@EnableWeld
class SmcContractMappingTableTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension dbExtension = DbTestData.getSmcDbExtension(mariaDBContainer
        , "db/schema.sql"
        , "db/smc_mapping-data.sql");

    @Inject
    SmcContractMappingTable table;

    long contractAddress = 7307657537262705518L;
    byte[] key = Convert.parseHexString("8F3F13CDBC4C2A8B668BB8C0ABE09B668F851F10FA39A49535F777919086D618");
    String mappingName = "balances";

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, EntityProducer.class, SmcContractMappingTable.class
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
        SmcContractMappingEntity entity = table.get(SmcContractMappingTable.KEY_FACTORY.newKey(contractAddress, mappingName, key));
        assertNotNull(entity);
        assertEquals(contractAddress, entity.getAddress());
        assertEquals("1234567890", entity.getSerializedObject());
        assertEquals("balances", entity.getName());
    }

    @Test
    void loadWithWrongName() {
        SmcContractMappingEntity entity = table.get(SmcContractMappingTable.KEY_FACTORY.newKey(contractAddress, "unknownName", key));
        assertNull(entity);
    }

    @Test
    void save() {
        long contractAddr = 123L;
        SmcContractMappingEntity entity = SmcContractMappingEntity.builder()
            .address(contractAddr)
            .key(key)
            .name(mappingName)
            .serializedObject("{\"value\":1590}")
            .height(12)
            .build();
        DbUtils.inTransaction(dbExtension, (con) -> table.insert(entity));

        SmcContractMappingEntity actual = table.get(SmcContractMappingTable.KEY_FACTORY.newKey(contractAddr, mappingName, key));
        assertNotNull(actual);
        assertNotEquals(0, actual.getDbId());
        assertEquals(contractAddr, actual.getAddress());
    }

}