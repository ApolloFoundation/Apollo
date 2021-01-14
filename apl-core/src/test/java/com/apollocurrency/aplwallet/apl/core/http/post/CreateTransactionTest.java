/*
 * Copyright © 2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionBuilder;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendixV2;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@EnableWeld
class CreateTransactionTest {

    BlockchainConfig blockchainConfig = Mockito.mock(BlockchainConfig.class);
    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    private Blockchain blockchain = Mockito.mock(Blockchain.class);
    private TimeService timeService = Mockito.mock(TimeService.class);
    private DatabaseManager databaseManager = Mockito.mock(DatabaseManager.class);
    TransactionTestData td = new TransactionTestData();
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(DbProperties.class, NtpTime.class,
        PropertiesHolder.class, DaoConfig.class, JdbiHandleFactory.class,
        TransactionDaoImpl.class, TransactionProcessor.class,
        TransactionRowMapper.class,
        TransactionBuilder.class,
        TransactionalDataSource.class)
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class))
        .addBeans(MockBean.of(timeService, TimeService.class))
        .addBeans(MockBean.of(databaseManager, DatabaseManager.class))
        .addBeans(MockBean.of(mock(PrunableLoadingService.class), PrunableLoadingService.class))
        .addBeans(MockBean.of(td.getTransactionTypeFactory(), TransactionTypeFactory.class))
        .build();
    private Block block = Mockito.mock(Block.class);
    private int lastBlockHeight = 1000;
    private int currentTime = 11000;

    @BeforeEach
    void setUp() {
        //bug with CDI
        try {
            CDI.current().select(BlockchainImpl.class).get();
        } catch (Exception e) {
        }

        Mockito.doReturn(lastBlockHeight).when(blockchain).getHeight();
        Mockito.doReturn(lastBlockHeight).when(block).getHeight();
        Mockito.doReturn(currentTime).when(timeService).getEpochTime();
        Mockito.doReturn(block).when(blockchain).getLastBlock();
    }

    public HttpServletRequest initRequest(String phasingFinishHeight, String phasingFinishTime, String phasingVotingModel) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getParameter("phasingFinishHeight")).thenReturn(phasingFinishHeight);
        Mockito.when(request.getParameter("phasingFinishTime")).thenReturn(phasingFinishTime);
        Mockito.when(request.getParameter("phasingVotingModel")).thenReturn(phasingVotingModel);

        return request;
    }

    @Test
    void parsePhasingWhenFinishTimeNotFilled() throws Exception {
        HttpServletRequest request = initRequest(String.valueOf(lastBlockHeight + 300), "-1", "-1");

        PhasingAppendixV2 phasingAppendix = HttpParameterParserUtil.parsePhasing(request);

        assertEquals(lastBlockHeight + 300, phasingAppendix.getFinishHeight());
        assertEquals(-1, phasingAppendix.getFinishTime());
    }

    @Test
    void parsePhasingWhenFinishTimeZero() {
        HttpServletRequest request = initRequest(String.valueOf(lastBlockHeight + 300), "0", "-1");

        assertThrows(ParameterException.class, () -> HttpParameterParserUtil.parsePhasing(request));
    }

    @Test
    void parsePhasingWhenFinishNullZero() throws Exception {
        HttpServletRequest request = initRequest(String.valueOf(lastBlockHeight + 300), null, "-1");

        PhasingAppendixV2 phasingAppendix = HttpParameterParserUtil.parsePhasing(request);
        assertEquals(-1, phasingAppendix.getFinishTime());
    }

    @Test
    void parsePhasingWhenFinishHeightMoreThenMax() throws Exception {
        HttpServletRequest request = initRequest(String.valueOf(lastBlockHeight + Constants.MAX_PHASING_DURATION + 2), "-1", "-1");

        assertThrows(ParameterException.class, () -> HttpParameterParserUtil.parsePhasing(request));
    }

    @Test
    void parsePhasingWhenFinishHeightLessThenMin() throws Exception {
        HttpServletRequest request = initRequest(String.valueOf(lastBlockHeight), "-1", "-1");

        assertThrows(ParameterException.class, () -> HttpParameterParserUtil.parsePhasing(request));
    }

    @Test
    void parsePhasingWhenFinishHeightNotFilled() throws Exception {
        HttpServletRequest request = initRequest("-1", "360", "-1");

        PhasingAppendixV2 phasingAppendix = HttpParameterParserUtil.parsePhasing(request);

        assertEquals(-1, phasingAppendix.getFinishHeight());
        assertEquals(currentTime + 360, phasingAppendix.getFinishTime());
    }

    @Test
    void parsePhasingWhenFinishHeightZero() throws Exception {
        HttpServletRequest request = initRequest("0", "360", "-1");

        assertThrows(ParameterException.class, () -> HttpParameterParserUtil.parsePhasing(request));
    }

    @Test
    void parsePhasingWhenFinishHeightNull() throws Exception {
        HttpServletRequest request = initRequest(null, "360", "-1");

        assertThrows(ParameterException.class, () -> HttpParameterParserUtil.parsePhasing(request));
    }

    @Test
    void parsePhasingWhenFinishTimeMoreThenMax() throws Exception {
        HttpServletRequest request = initRequest("-1", String.valueOf(Constants.MAX_PHASING_TIME_DURATION_SEC + 1), "-1");

        assertThrows(ParameterException.class, () -> HttpParameterParserUtil.parsePhasing(request));
    }

    @Test
    void parsePhasingWhenFinishTimeMaxValue() throws Exception {
        HttpServletRequest request = initRequest("-1", String.valueOf(Constants.MAX_PHASING_TIME_DURATION_SEC), "-1");

        PhasingAppendixV2 phasingAppendix = HttpParameterParserUtil.parsePhasing(request);

        assertEquals(-1, phasingAppendix.getFinishHeight());
        assertEquals(currentTime + Constants.MAX_PHASING_TIME_DURATION_SEC, phasingAppendix.getFinishTime());
    }

    @Test
    void parsePhasingWhenFinishTimeMinValue() throws Exception {
        HttpServletRequest request = initRequest("-1", "0", "-1");

        PhasingAppendixV2 phasingAppendix = HttpParameterParserUtil.parsePhasing(request);

        assertEquals(-1, phasingAppendix.getFinishHeight());
        assertEquals(currentTime, phasingAppendix.getFinishTime());
    }

}