/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.chainid;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.config.ChainsConfigLoader;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Slf4j
class HeightConfigFeeSettingsTest {

    // config is used for simulating complex shard settings
    private static final String CONFIG_NAME = "chains-fee-config.json";
    private static final UUID CHAIN_UUID = UUID.fromString("3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6");
    private PropertiesHolder holder = mock(PropertiesHolder.class);
    private ChainsConfigLoader chainsConfigLoader;
    private Map<UUID, Chain> loadedChains;
    private BlockchainConfig blockchainConfig;
    private Chain chain;

    @BeforeEach
    void setup() {
        chainsConfigLoader = new ChainsConfigLoader(CONFIG_NAME);
        loadedChains = chainsConfigLoader.load();
        assertEquals(1, loadedChains.size());
        assertNotNull(loadedChains.get(CHAIN_UUID));
        doReturn(5000).when(holder).getIntProperty("apl.maxPrunableLifetime");
        doReturn(5000).when(holder).getIntProperty("apl.minPrunableLifetime");

        prepareAndInitComponents();
    }

    @ParameterizedTest
    @MethodSource("provideHeightAndFeeRate")
    void test_atHeight_AllCorrectConfigs(int height, int rate) {

        HeightConfig  result = blockchainConfig.getConfigAtHeight(height);  // checked method
        log.trace("result = {}", result);
        assertNotNull(result);
        assertEquals(rate, result.getFeeRate(TransactionTypes.TransactionTypeSpec.ORDINARY_PAYMENT));
        assertEquals(rate, result.getFeeRate(TransactionTypes.TransactionTypeSpec.PRIVATE_PAYMENT));
        assertEquals(rate, result.getFeeRate(TransactionTypes.TransactionTypeSpec.ALIAS_ASSIGNMENT));
        assertEquals(100, result.getFeeRate(TransactionTypes.TransactionTypeSpec.UPDATE_V2));//default rate for other type and subType

    }

    @Test
    void test_atHeight_sizeBasedConfig() {
        HeightConfig  result = blockchainConfig.getConfigAtHeight(50);  // checked method
        assertNotNull(result);
        assertEquals(new BigDecimal("1.5"), result.getSizeBasedFee(TransactionTypes.TransactionTypeSpec.PRIVATE_PAYMENT, BigDecimal.valueOf(-2)));
        assertEquals(new BigDecimal("0.1"), result.getBaseFee(TransactionTypes.TransactionTypeSpec.PRIVATE_PAYMENT, BigDecimal.valueOf(-2)));
        BigDecimal[] defaultPrivatePaymentAdditionalFees = {BigDecimal.valueOf(-2), BigDecimal.valueOf(-2)};
        assertArrayEquals(defaultPrivatePaymentAdditionalFees, result.getAdditionalFees(TransactionTypes.TransactionTypeSpec.PRIVATE_PAYMENT, defaultPrivatePaymentAdditionalFees));


        assertEquals(new BigDecimal("1"), result.getSizeBasedFee(TransactionTypes.TransactionTypeSpec.ALIAS_ASSIGNMENT, BigDecimal.valueOf(-2)));
        assertEquals(new BigDecimal("-2"), result.getBaseFee(TransactionTypes.TransactionTypeSpec.ALIAS_ASSIGNMENT, BigDecimal.valueOf(-2)));
        assertArrayEquals(new BigDecimal[] {new BigDecimal("10"),new BigDecimal("10000000000000"), new BigDecimal("0.333"), null}, result.getAdditionalFees(TransactionTypes.TransactionTypeSpec.ALIAS_ASSIGNMENT, new BigDecimal[4]));
        assertArrayEquals(new BigDecimal[] {new BigDecimal("10"),new BigDecimal("10000000000000")}, result.getAdditionalFees(TransactionTypes.TransactionTypeSpec.ALIAS_ASSIGNMENT, new BigDecimal[2]));


        assertEquals(new BigDecimal("11"), result.getBaseFee(TransactionTypes.TransactionTypeSpec.UPDATE_V2, BigDecimal.valueOf(11)));
        assertEquals(new BigDecimal("11"), result.getSizeBasedFee(TransactionTypes.TransactionTypeSpec.UPDATE_V2, BigDecimal.valueOf(11)));
        assertArrayEquals(new BigDecimal[2], result.getAdditionalFees(TransactionTypes.TransactionTypeSpec.UPDATE_V2, new BigDecimal[] {null, null}));
        assertEquals(100, result.getFeeRate(TransactionTypes.TransactionTypeSpec.UPDATE_V2));//default rate for other type and subType

    }

    /**
     * Height and target Fee Rate are supplied into unit test method
     * @return height + fee rate value for test
     */
    static Stream<Arguments> provideHeightAndFeeRate() {
        return Stream.of(
            arguments(1, 100),//default value
            arguments(2, 100),//default value
            arguments(10,100),//default value
            arguments(50, 50),//set value
            arguments(55, 50),//set value
            arguments(70, 100),//default value
            arguments(100, 0),//set value
            arguments(110, 0),//set value
            arguments(120, 0),//set value
            arguments(200, 100),//default value
            arguments(210, 100),//default value
            arguments(Integer.MAX_VALUE, 100)//default value
        );
    }

    private void prepareAndInitComponents() {
        chain = loadedChains.get(CHAIN_UUID);
        assertNotNull(chain);
        assertNotNull(chain.getBlockchainProperties());
        blockchainConfig = new BlockchainConfig(chain, holder);
    }

}