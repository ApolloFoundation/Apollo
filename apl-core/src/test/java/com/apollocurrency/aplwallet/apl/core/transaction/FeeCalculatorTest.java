/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.util.env.config.FeeRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeCalculatorTest {
    static final int CURRENT_HEIGHT = 100;
    TransactionTestData td = new TransactionTestData();
    BlockchainConfig blockchainConfig = td.getBlockchainConfig();

    @Mock
    HeightConfig heightConfig;
    @Mock
    PrunableLoadingService prunableLoadingService;

    FeeCalculator feeCalculator;

    @BeforeEach
    void setUp() {
        feeCalculator = new FeeCalculator(prunableLoadingService, blockchainConfig);
    }

    @Test
    void getMinimumFeeATM_throwIllegalArgumentException() {
        //GIVEN
        doReturn(null).when(blockchainConfig).getConfigAtHeight(CURRENT_HEIGHT);
        //THEN
        assertThrows(IllegalArgumentException.class,
            //WHEN
            () -> feeCalculator.getMinimumFeeATM(td.TRANSACTION_1, CURRENT_HEIGHT));
    }

    @Test
    void getMinimumFeeATM_withDefaultRate_100() {
        long fee;
        //GIVEN
        when(blockchainConfig.getOneAPL()).thenReturn(100000000L);
        doReturn(heightConfig).when(blockchainConfig).getConfigAtHeight(CURRENT_HEIGHT);
        doReturn(FeeRate.DEFAULT_RATE).when(heightConfig).getFeeRate(td.TRANSACTION_1.getType().getSpec().getType(), td.TRANSACTION_1.getType().getSpec().getSubtype());
        //WHEN
        fee = feeCalculator.getMinimumFeeATM(td.TRANSACTION_1, CURRENT_HEIGHT);
        //THEN
        assertEquals(100100000000L, fee);

        //GIVEN
        doReturn(FeeRate.DEFAULT_RATE).when(heightConfig).getFeeRate(td.TRANSACTION_2.getType().getSpec().getType(), td.TRANSACTION_2.getType().getSpec().getSubtype());
        //WHEN
        fee = feeCalculator.getMinimumFeeATM(td.TRANSACTION_2, CURRENT_HEIGHT);
        //THEN
        assertEquals(td.TRANSACTION_2.getFeeATM(), fee);
    }

    @Test
    void getMinimumFeeATM_withRate_0() {
        long fee;
        //GIVEN
        doReturn(heightConfig).when(blockchainConfig).getConfigAtHeight(CURRENT_HEIGHT);
        doReturn((short)0).when(heightConfig).getFeeRate(td.TRANSACTION_1.getType().getSpec().getType(), td.TRANSACTION_1.getType().getSpec().getSubtype());
        //WHEN
        fee = feeCalculator.getMinimumFeeATM(td.TRANSACTION_1, CURRENT_HEIGHT);
        //THEN
        assertEquals(0L, fee);

        //GIVEN
        doReturn((short)0).when(heightConfig).getFeeRate(td.TRANSACTION_2.getType().getSpec().getType(), td.TRANSACTION_2.getType().getSpec().getSubtype());
        //WHEN
        fee = feeCalculator.getMinimumFeeATM(td.TRANSACTION_2, CURRENT_HEIGHT);
        //THEN
        assertEquals(0L, fee);
    }

    @Test
    void getMinimumFeeATM_withRate_50() {
        long fee;
        //GIVEN
        when(blockchainConfig.getOneAPL()).thenReturn(100000000L);
        doReturn(heightConfig).when(blockchainConfig).getConfigAtHeight(CURRENT_HEIGHT);
        doReturn((short)(FeeRate.DEFAULT_RATE/2)).when(heightConfig).getFeeRate(td.TRANSACTION_1.getType().getSpec().getType(), td.TRANSACTION_1.getType().getSpec().getSubtype());
        //WHEN
        fee = feeCalculator.getMinimumFeeATM(td.TRANSACTION_1, CURRENT_HEIGHT);
        //THEN
        assertEquals(100100000000L/2, fee);

        //GIVEN
        doReturn((short)(FeeRate.DEFAULT_RATE/2)).when(heightConfig).getFeeRate(td.TRANSACTION_2.getType().getSpec().getType(), td.TRANSACTION_2.getType().getSpec().getSubtype());
        //WHEN
        fee = feeCalculator.getMinimumFeeATM(td.TRANSACTION_2, CURRENT_HEIGHT);
        //THEN
        assertEquals(td.TRANSACTION_2.getFeeATM() / 2, fee);
    }

}