/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.smc;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractEntity;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
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
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@EnableWeld
class SmcContractTableTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(mariaDBContainer, DbTestData.getInMemDbProps(), "db/smc-data.sql", "db/schema.sql");

    @Inject
    SmcContractTable table;

    long contractAddress = 7307657537262705518L;

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, EntityProducer.class, SmcContractTable.class
    )
        .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(dbExtension.getDatabaseManager().getJdbi(), Jdbi.class))
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
        SmcContractEntity entity = table.get(SmcContractTable.KEY_FACTORY.newKey(contractAddress));
        assertNotNull(entity);
        assertEquals(contractAddress, entity.getAddress());
        assertEquals("Deal", entity.getContractName());
    }

    @Test
    void save() {
        long contractAddress = 123L;
        SmcContractEntity entity = SmcContractEntity.builder()
            .address(contractAddress)
            .owner(5678L)
            .transactionId(-123L)
            .contractName("NewDeal2")
            .data("class NewDeal2 extends Contract {}")
            .args("123, \"aaa\"")
            .languageName("javascript")
            .languageVersion("0.1.1")
            .status("ACTIVE")
            .height(12)
            .build();
        DbUtils.inTransaction(dbExtension, (con) -> table.insert(entity));

        SmcContractEntity actual = table.get(SmcContractTable.KEY_FACTORY.newKey(contractAddress));
        assertNotNull(actual);
        assertTrue(actual.getDbId() != 0);
        assertEquals(contractAddress, actual.getAddress());
        assertEquals("NewDeal2", actual.getContractName());
    }

    @Test
    void update() {
        SmcContractEntity entity = table.get(SmcContractTable.KEY_FACTORY.newKey(contractAddress));
        assertNotNull(entity);
        assertEquals(contractAddress, entity.getAddress());
        assertEquals("Deal", entity.getContractName());
        entity.setData("Class Stub {}");
        entity.setHeight(15);

        TransactionalDataSource dataSource = dbExtension.getDatabaseManager().getDataSource();
        try (Connection con = dataSource.begin()) { // start new transaction
            table.insert(entity);
            dataSource.commit();
            fail("Unexpected flow.");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Duplicate entry '" + entity.getAddress() + "'"));
        } catch (Throwable e) {
            fail("Unexpected flow.");
        }
    }
}
