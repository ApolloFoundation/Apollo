/*
 * Copyright © 2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.model.PhasingParams;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

//TODO fix several methods
@EnableWeld
class PhasingAppendixTest {

    private BlockchainImpl blockchain = Mockito.mock(BlockchainImpl.class);
    private TimeService timeService = Mockito.mock(TimeService.class);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(DbProperties.class, NtpTime.class,
        PropertiesHolder.class,
        TransactionDaoImpl.class, TransactionProcessor.class,
        TransactionalDataSource.class)
        .addBeans(MockBean.of(mock(DatabaseManager.class), DatabaseManager.class))
        .addBeans(MockBean.of(blockchain, BlockchainImpl.class))
        .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
        .addBeans(MockBean.of(mock(BlockchainConfig.class), BlockchainConfig.class))
        .addBeans(MockBean.of(timeService, TimeService.class))
        .build();
    private Block block = Mockito.mock(Block.class);
    private int lastBlockHeight = 1000;
    private int currentTime = 11000;
    private PhasingAppendix phasingAppendix;

    @BeforeEach
    void setUp() {
        Mockito.doReturn(lastBlockHeight).when(blockchain).getHeight();
        Mockito.doReturn(lastBlockHeight).when(block).getHeight();
        Mockito.doReturn(currentTime).when(timeService).getEpochTime();
        Mockito.doReturn(block).when(blockchain).getLastBlock();
        phasingAppendix = new PhasingAppendixV2(-1, 360, new PhasingParams((byte) 0, 0, 3, 0, (byte) 0, new long[]{1, 2, 3}), null, null, Byte.MIN_VALUE);
    }

    @Test
    void validateFinishHeightAndTimeWhenBothNotFilled() {
        assertThrows(AplException.NotCurrentlyValidException.class, () -> phasingAppendix.validateFinishHeightAndTime(-1, -1));
    }

    @Disabled
    void validateFinishHeightAndTimeWhenBothFilled() {
        assertThrows(AplException.NotCurrentlyValidException.class, () -> phasingAppendix.validateFinishHeightAndTime(500, 360));
    }

    @Disabled
    void validateFinishHeightAndTimeWhenTimeNull() {
        assertThrows(AplException.NotCurrentlyValidException.class, () -> phasingAppendix.validateFinishHeightAndTime(500, null));
    }

    @Disabled
    void validateFinishHeightAndTimeWhenHeightNull() {
        assertThrows(AplException.NotCurrentlyValidException.class, () -> phasingAppendix.validateFinishHeightAndTime(null, 300));
    }

    @Test
    void validateFinishHeightAndTimeWhenHeightNotFilledAndTimeMoreThenMax() {
        assertThrows(AplException.NotCurrentlyValidException.class, () -> phasingAppendix.validateFinishHeightAndTime(-1, currentTime + Constants.MAX_PHASING_TIME_DURATION_SEC));
    }

    @Test
    void validateFinishHeightAndTimeWhenHeightLessThenMin() {
        assertThrows(AplException.NotCurrentlyValidException.class, () -> phasingAppendix.validateFinishHeightAndTime(lastBlockHeight, -1));
    }

    @Test
    void validateFinishHeightAndTimeWhenHeightMoreThenMax() {
        assertThrows(AplException.NotCurrentlyValidException.class, () -> phasingAppendix.validateFinishHeightAndTime(lastBlockHeight + Constants.MAX_PHASING_DURATION, -1));
    }


}