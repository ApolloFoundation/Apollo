/*
 * Copyright © 2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.DbConfig;
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

@EnableWeld
class CreateTransactionTest {

    private BlockchainImpl blockchain = Mockito.mock(BlockchainImpl.class);
    private EpochTime timeService = Mockito.mock(EpochTime.class);
    private Block block = Mockito.mock(Block.class);

    private int lastBlockHeight = 1000;
    private int currentTime = 11000;


    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(DbProperties.class, NtpTime.class,
            PropertiesHolder.class, BlockchainConfig.class, DbConfig.class,
            TransactionDaoImpl.class, TransactionProcessor.class,
            TransactionalDataSource.class)
            .addBeans(MockBean.of(blockchain, BlockchainImpl.class))
            .addBeans(MockBean.of(timeService, EpochTime.class))
            .build();

    @BeforeEach
    void setUp() {
        //bug with CDI
        try {CDI.current().select(BlockchainImpl.class).get();}catch (Exception e){}

        Mockito.doReturn(lastBlockHeight).when(blockchain).getHeight();
        Mockito.doReturn(lastBlockHeight).when(block).getHeight();
        Mockito.doReturn(currentTime).when(timeService).getEpochTime();
        Mockito.doReturn(block).when(blockchain).getLastBlock();
    }

    @Test
    void parsePhasingWhenFinishTimeNotFilled() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        Mockito.when(request.getParameter("phasingFinishHeight")).thenReturn(String.valueOf(lastBlockHeight + 300));
        Mockito.when(request.getParameter("phasingFinishTime")).thenReturn("-1");
        Mockito.when(request.getParameter("phasingVotingModel")).thenReturn("-1");

        SendMoney createTransaction = new SendMoney();

        PhasingAppendix phasingAppendix = createTransaction.parsePhasing(request);

        assertEquals(lastBlockHeight + 300, phasingAppendix.getFinishHeight());
        assertEquals(-1, phasingAppendix.getFinishTime());

    }

    @Test
    void parsePhasingWhenFinishTimeZero() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        Mockito.when(request.getParameter("phasingFinishHeight")).thenReturn(String.valueOf(lastBlockHeight + 300));
        Mockito.when(request.getParameter("phasingFinishTime")).thenReturn("0");
        Mockito.when(request.getParameter("phasingVotingModel")).thenReturn("-1");

        SendMoney createTransaction = new SendMoney();

        assertThrows(ParameterException.class,()-> createTransaction.parsePhasing(request));
    }

    @Test
    void parsePhasingWhenFinishNullZero() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        Mockito.when(request.getParameter("phasingFinishHeight")).thenReturn(String.valueOf(lastBlockHeight + 300));
        Mockito.when(request.getParameter("phasingFinishTime")).thenReturn(null);
        Mockito.when(request.getParameter("phasingVotingModel")).thenReturn("-1");

        SendMoney createTransaction = new SendMoney();

        assertThrows(ParameterException.class,()-> createTransaction.parsePhasing(request));
    }

    @Test
    void parsePhasingWhenFinishHeightMoreThenMax() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        Mockito.when(request.getParameter("phasingFinishHeight")).thenReturn(String.valueOf(lastBlockHeight + Constants.MAX_PHASING_DURATION + 2));
        Mockito.when(request.getParameter("phasingFinishTime")).thenReturn("-1");
        Mockito.when(request.getParameter("phasingVotingModel")).thenReturn("-1");

        SendMoney createTransaction = new SendMoney();

        assertThrows(ParameterException.class,()-> createTransaction.parsePhasing(request));
    }

    @Test
    void parsePhasingWhenFinishHeightLessThenMin() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        Mockito.when(request.getParameter("phasingFinishHeight")).thenReturn(String.valueOf(lastBlockHeight));
        Mockito.when(request.getParameter("phasingFinishTime")).thenReturn("-1");
        Mockito.when(request.getParameter("phasingVotingModel")).thenReturn("-1");

        SendMoney createTransaction = new SendMoney();

        assertThrows(ParameterException.class,()-> createTransaction.parsePhasing(request));
    }

    @Test
    void parsePhasingWhenFinishHeightNotFilled() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        Mockito.when(request.getParameter("phasingFinishHeight")).thenReturn("-1");
        Mockito.when(request.getParameter("phasingFinishTime")).thenReturn("360");
        Mockito.when(request.getParameter("phasingVotingModel")).thenReturn("-1");

        SendMoney createTransaction = new SendMoney();

        PhasingAppendix phasingAppendix = createTransaction.parsePhasing(request);

        assertEquals(-1, phasingAppendix.getFinishHeight());
        assertEquals(currentTime + 360, phasingAppendix.getFinishTime());
    }

    @Test
    void parsePhasingWhenFinishHeightZero() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        Mockito.when(request.getParameter("phasingFinishHeight")).thenReturn("0");
        Mockito.when(request.getParameter("phasingFinishTime")).thenReturn("360");
        Mockito.when(request.getParameter("phasingVotingModel")).thenReturn("-1");

        SendMoney createTransaction = new SendMoney();

        assertThrows(ParameterException.class,()-> createTransaction.parsePhasing(request));
    }

    @Test
    void parsePhasingWhenFinishHeightNull() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        Mockito.when(request.getParameter("phasingFinishHeight")).thenReturn(null);
        Mockito.when(request.getParameter("phasingFinishTime")).thenReturn("360");
        Mockito.when(request.getParameter("phasingVotingModel")).thenReturn("-1");

        SendMoney createTransaction = new SendMoney();

        assertThrows(ParameterException.class,()-> createTransaction.parsePhasing(request));
    }

    @Test
    void parsePhasingWhenFinishTimeMoreThenMax() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        Mockito.when(request.getParameter("phasingFinishHeight")).thenReturn("-1");
        Mockito.when(request.getParameter("phasingFinishTime")).thenReturn(String.valueOf(Constants.MAX_PHASING_TIME_DURATION_SEC + 1));
        Mockito.when(request.getParameter("phasingVotingModel")).thenReturn("-1");

        SendMoney createTransaction =new SendMoney();

        assertThrows(ParameterException.class,()-> createTransaction.parsePhasing(request));
    }

    @Test
    void parsePhasingWhenFinishTimeMaxValue() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        Mockito.when(request.getParameter("phasingFinishHeight")).thenReturn("-1");
        Mockito.when(request.getParameter("phasingFinishTime")).thenReturn(String.valueOf(Constants.MAX_PHASING_TIME_DURATION_SEC));
        Mockito.when(request.getParameter("phasingVotingModel")).thenReturn("-1");

        SendMoney createTransaction = new SendMoney();
        PhasingAppendix phasingAppendix = createTransaction.parsePhasing(request);

        assertEquals(-1, phasingAppendix.getFinishHeight());
        assertEquals(currentTime + Constants.MAX_PHASING_TIME_DURATION_SEC, phasingAppendix.getFinishTime());
    }

    @Test
    void parsePhasingWhenFinishTimeMinValue() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        Mockito.when(request.getParameter("phasingFinishHeight")).thenReturn("-1");
        Mockito.when(request.getParameter("phasingFinishTime")).thenReturn(String.valueOf("0"));
        Mockito.when(request.getParameter("phasingVotingModel")).thenReturn("-1");


        SendMoney createTransaction = new SendMoney();
        PhasingAppendix phasingAppendix = createTransaction.parsePhasing(request);

        assertEquals(-1, phasingAppendix.getFinishHeight());
        assertEquals(currentTime, phasingAppendix.getFinishTime());
    }

}