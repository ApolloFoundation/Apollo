/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.smc;

import com.apollocurrency.aplwallet.api.v2.model.ContractDetails;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.SmcContractDetailsRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractEntity;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractQuery;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcPublishContractAttachment;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.testutil.EntityProducer;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.api.PositiveRange;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.inject.Inject;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Tag("slow")
@EnableWeld
class SmcContractTableTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension dbExtension = DbTestData.getSmcDbExtension(mariaDBContainer
        , "db/schema.sql"
        , "db/smc-data.sql");

    static {
        Convert2.init("APL", 1739068987193023818L);
    }

    @Inject
    SmcContractTable table;

    long contractAddress = 832074176060907552L;

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);

    TransactionTypeFactory transactionTypeFactory = mock(TransactionTypeFactory.class);
    TransactionType transactionType = mock(TransactionType.class);
    SmcPublishContractAttachment attachment = mock(SmcPublishContractAttachment.class);

    SmcContractDetailsRowMapper smcContractDetailsRowMapper = new SmcContractDetailsRowMapper(transactionTypeFactory);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, EntityProducer.class, SmcContractTable.class
    )
        .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(smcContractDetailsRowMapper, SmcContractDetailsRowMapper.class))
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
        assertEquals("MyAPL20PersonalLockable", entity.getContractName());
    }

    @Test
    void save() {
        long contractAddress = 123L;
        SmcContractEntity entity = SmcContractEntity.builder()
            .address(contractAddress)
            .owner(5678L)
            .transactionId(-123L)
            .blockTimestamp(123)
            .transactionHash(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9})
            .contractName("NewDeal2")
            .data("class NewDeal2 extends Contract {}")
            .baseContract("Contract")
            .args("123, \"aaa\"")
            .languageName("javascript")
            .languageVersion("0.1.1")
            .status("ACTIVE")
            .height(12)
            .build();
        DbUtils.inTransaction(dbExtension, (con) -> table.insert(entity));

        SmcContractEntity actual = table.get(SmcContractTable.KEY_FACTORY.newKey(contractAddress));
        assertNotNull(actual);
        assertNotEquals(0, actual.getDbId());
        assertEquals(contractAddress, actual.getAddress());
        assertEquals("NewDeal2", actual.getContractName());
    }

    @Test
    void update() {
        SmcContractEntity entity = table.get(SmcContractTable.KEY_FACTORY.newKey(contractAddress));
        assertNotNull(entity);
        assertEquals(contractAddress, entity.getAddress());
        assertEquals("MyAPL20PersonalLockable", entity.getContractName());
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

    private List<ContractDetails> getMockContractDetailsList() throws AplException.NotValidException {
        return getMockContractDetailsList(null, null, null, null, 100, 0, -1);
    }

    private List<ContractDetails> getMockContractDetailsList(Long address, Long owner, String name, String status, int height) throws AplException.NotValidException {
        return getMockContractDetailsList(address, owner, name, status, height, 0, -1);
    }

    private List<ContractDetails> getMockContractDetailsList(Long address, Long owner, String name, String status, int height, int from, int to) throws AplException.NotValidException {
        //GIVEN
        when(transactionTypeFactory.findTransactionType(any(byte.class), any(byte.class))).thenReturn(transactionType);
        when(transactionType.parseAttachment(any(ByteBuffer.class))).thenReturn(attachment);
        when(attachment.getFuelLimit()).thenReturn(BigInteger.TEN);
        when(attachment.getFuelPrice()).thenReturn(BigInteger.ONE);

        //WHEN
        return table.getContractsByFilter(ContractQuery.builder()
            .address(address)
            .owner(owner)
            .name(name)
            .status(status)
            .height(height)
            .paging(new PositiveRange(from, to))
            .build());
    }

    @Test
    void getContractsByEmptyFilter() throws AplException.NotValidException {
        //WHEN
        List<ContractDetails> result = getMockContractDetailsList();

        //THEN
        assertNotNull(result);
        assertEquals(1, result.size());
        var value = result.get(0);
        assertEquals(Convert2.rsAccount(contractAddress), value.getAddress());
        assertEquals("MyAPL20PersonalLockable", value.getName());
        assertEquals(Convert2.fromEpochTime(123723548), value.getTimestamp());
    }

    @Test
    void getContractsByEmptyFilterWrongHeight() throws AplException.NotValidException {
        //WHEN
        List<ContractDetails> result = getMockContractDetailsList(null, null, null, null, 1);

        //THEN
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "MyAPL20PersonalLockable", "MyAPL"})
    @NullSource
    void getContractsByName(String contractName) throws AplException.NotValidException {
        //WHEN
        List<ContractDetails> result = getMockContractDetailsList(null, null, contractName, null, 100);

        //THEN
        assertNotNull(result);
        assertEquals(1, result.size());
        var value = result.get(0);
        assertEquals(Convert2.rsAccount(contractAddress), value.getAddress());
        assertEquals("MyAPL20PersonalLockable", value.getName());
        assertEquals(Convert2.fromEpochTime(123723548), value.getTimestamp());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2", "Deal2"})
    void getContractsByNameNoResult(String contractName) throws AplException.NotValidException {
        //WHEN
        List<ContractDetails> result = getMockContractDetailsList(null, null, contractName, null, 100);

        //THEN
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void getContractsByAddress() throws AplException.NotValidException {
        //WHEN
        List<ContractDetails> result = getMockContractDetailsList(contractAddress, null, "MyAPL20PersonalLockable", null, 100);

        //THEN
        assertNotNull(result);
        assertEquals(1, result.size());
        var value = result.get(0);
        assertEquals(Convert2.rsAccount(contractAddress), value.getAddress());
        assertEquals("MyAPL20PersonalLockable", value.getName());
        assertEquals(Convert2.fromEpochTime(123723548), value.getTimestamp());
    }

    @Test
    void getContractsByAddressNoResult() throws AplException.NotValidException {
        //WHEN
        List<ContractDetails> result = getMockContractDetailsList(0L, null, "Deal", null, 100);

        //THEN
        assertNotNull(result);
        assertEquals(0, result.size());
    }

}
