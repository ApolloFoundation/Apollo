/*
 * Copyright © 2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.model.PhasingParams;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class PhasingAppendixTest {

    @Mock
    Blockchain blockchain;
    @Mock
    TimeService timeService;
    @Mock
    PhasingPollService phasingPollService;
    @Mock
    BlockchainConfig blockchainConfig;
    PhasingAppendixV2Validator validatorv2;


    private Block block = Mockito.mock(Block.class);
    private int lastBlockHeight = 1000;
    private int currentTime = 11000;
    private PhasingAppendix phasingAppendix;

    @BeforeEach
    void setUp() {
        Mockito.doReturn(lastBlockHeight).when(block).getHeight();
        phasingAppendix = new PhasingAppendixV2(-1, 360, new PhasingParams((byte) 0, 0, 3, 0, (byte) 0, new long[]{1, 2, 3}), null, null, Byte.MIN_VALUE);
        validatorv2 = new PhasingAppendixV2Validator(new PhasingAppendixValidator(blockchain, phasingPollService, blockchainConfig), blockchain, timeService);
    }

    @Test
    void validateFinishHeightAndTimeWhenBothNotFilled() {
        PhasingAppendixV2Validator validatorv2 = new PhasingAppendixV2Validator(new PhasingAppendixValidator(blockchain, phasingPollService, blockchainConfig), blockchain, timeService);
        assertThrows(AplException.NotCurrentlyValidException.class, () -> validatorv2.validateFinishHeightAndTime(-1, -1, phasingAppendix));
    }

    @SneakyThrows
    @Test
    void validate_HeightAndTimeWhenBothFilled() {
        Mockito.doReturn(block).when(blockchain).getLastBlock();
        validatorv2.validateFinishHeightAndTime(500, 360, phasingAppendix);
    }

    @Test
    void validateFinishHeightAndTimeWhenTimeNull() {
        assertThrows(AplException.NotCurrentlyValidException.class, () -> validatorv2.validateFinishHeightAndTime(500, null, phasingAppendix));
    }

    @Test
    void validateFinishHeightAndTimeWhenHeightNull() {
        assertThrows(AplException.NotCurrentlyValidException.class, () -> validatorv2.validateFinishHeightAndTime(null, 300, phasingAppendix));
    }

    @Test
    void validateFinishHeightAndTimeWhenHeightNotFilledAndTimeMoreThenMax() {
        Mockito.doReturn(block).when(blockchain).getLastBlock();
        assertThrows(AplException.NotCurrentlyValidException.class, () -> validatorv2.validateFinishHeightAndTime(-1, currentTime + Constants.MAX_PHASING_TIME_DURATION_SEC, phasingAppendix));
    }

    @Test
    void validateFinishHeightAndTimeWhenHeightLessThenMin() {
        assertThrows(AplException.NotCurrentlyValidException.class, () -> validatorv2.validateFinishHeightAndTime(lastBlockHeight, -1, phasingAppendix));
    }

    @Test
    void validateFinishHeightAndTimeWhenHeightMoreThenMax() {
        assertThrows(AplException.NotCurrentlyValidException.class, () -> validatorv2.validateFinishHeightAndTime(lastBlockHeight + Constants.MAX_PHASING_DURATION, -1, phasingAppendix));
    }


}