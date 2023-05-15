/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.smc;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.SmcContractEventLogRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.smc.model.AplAddress;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.smc.data.type.Address;
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
import org.junit.jupiter.params.provider.CsvSource;

import jakarta.inject.Inject;

import static com.apollocurrency.smc.util.HexUtils.parseHex;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Tag("slow")
@EnableWeld
class SmcContractEventLogTableTest extends DbContainerBaseTest {
    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();
    @RegisterExtension
    static DbExtension dbExtension = dbExtension = DbTestData.getSmcDbExtension(mariaDBContainer
        , "db/schema.sql"
        , "db/smc_event-data.sql");

    BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    Chain chain = mock(Chain.class);

    {
        doReturn(chain).when(blockchainConfig).getChain();
    }

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from()
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(mock(Blockchain.class), Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(mock(PropertiesHolder.class), PropertiesHolder.class))
        .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(mock(TimeService.class), TimeService.class))
        .build();

    long contractId = 1872215471858864549L;
    Address contractAddress = new AplAddress(contractId);

    @Inject
    private PropertiesHolder propertiesHolder;

    private SmcContractEventLogTable table;
    private SmcContractEventLogRowMapper mapper;
    private TransactionTestData td;

    @BeforeEach
    void setUp() {

        table = new SmcContractEventLogTable(propertiesHolder, dbExtension.getDatabaseManager(), null);

        mapper = new SmcContractEventLogRowMapper();
    }

    @AfterEach
    void tearDown() {
        dbExtension.cleanAndPopulateDb();
    }


    @Test
    void getEntry() {
        long logId = 1l;

        var actual = table.getEntry(logId);
        assertNotNull(actual);
        assertNotEquals(0, actual.getDbId());
        assertEquals(8641622137343210570L, actual.getEventId());
        assertArrayEquals(parseHex("0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D"), actual.getSignature());
    }

    @Test
    void getEntries() {
        //GIVEN
        long eventId = 8641622137343210570L;
        byte[] signature = parseHex("0xBA5BDB95AD9FED090C0D01F6EE3EE6BC75B30740C6F7D367344EAA4C2E2E294D");
        //WHEN
        var actual = table.getEntries(eventId, signature, 0, -1);
        //THEN
        assertEquals(10, actual.size());
        //WHEN
        actual = table.getEntries(eventId, signature, 6, 10);
        //THEN
        assertEquals(4, actual.size());
    }

    @CsvSource(delimiterString = ":", value = {
        "Buy:0:-1:4",
        "Transfer:0:-1:10",
        "Transfer:100:-1:9",
        "Transfer:100:10000:8"
    })
    @ParameterizedTest
    void getEventsByFilter(String name, Integer heightFrom, Integer heightTo, Integer num) {
        var actual = table.getEventsByFilter(
            contractId, name, heightFrom, heightTo, 0, -1, "ASC"
        );
        assertEquals(num, actual.size());
    }
}