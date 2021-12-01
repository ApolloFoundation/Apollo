/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.smc.contract.fuel.FuelValidator;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class SmcFuelValidator implements FuelValidator {
    private static final BigInteger LIMIT_MIN_VALUE = BigInteger.valueOf(100_000);//in Fuel unit
    private static final BigInteger LIMIT_MAX_VALUE = BigInteger.valueOf(Integer.MAX_VALUE);

    private static final BigInteger PRICE_MIN_VALUE = BigInteger.valueOf(10_000);//in ATM
    private static final BigInteger PRICE_MAX_VALUE = BigInteger.valueOf(1_000_000);//in ATM
    private final BlockchainConfig blockchainConfig;

    @Inject
    public SmcFuelValidator(BlockchainConfig blockchainConfig) {
        this.blockchainConfig = blockchainConfig;
    }

    @Override
    public boolean validateLimitValue(BigInteger limit) {
        //TODO add boundary values into blockchain config
        //blockchainConfig.getCurrentConfig().getMaxBalanceATM();
        return limit.compareTo(LIMIT_MIN_VALUE) >= 0
            && limit.compareTo(LIMIT_MAX_VALUE) < 0;
    }

    @Override
    public boolean validatePriceValue(BigInteger price) {
        //TODO add boundary values into blockchain config
        return price.compareTo(PRICE_MIN_VALUE) >= 0 &&
            price.compareTo(PRICE_MAX_VALUE) <= 0;
    }
}
