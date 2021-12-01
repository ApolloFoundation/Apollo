/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.exception.AplUnacceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractSmcAttachment;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class SmcFuelValidator {
    private static final BigInteger LIMIT_MIN_VALUE = BigInteger.valueOf(100_000);//in Fuel unit
    private static final BigInteger LIMIT_MAX_VALUE = BigInteger.valueOf(Integer.MAX_VALUE);

    private static final BigInteger PRICE_MIN_VALUE = BigInteger.valueOf(10_000);//in ATM
    private static final BigInteger PRICE_MAX_VALUE = BigInteger.valueOf(1_000_000);//in ATM
    private final BlockchainConfig blockchainConfig;

    @Inject
    public SmcFuelValidator(BlockchainConfig blockchainConfig) {
        this.blockchainConfig = blockchainConfig;
    }

    public void validate(Transaction transaction) {
        //TODO add boundary values into blockchain config
        //blockchainConfig.getCurrentConfig().getMaxBalanceATM();
        AbstractSmcAttachment attachment = (AbstractSmcAttachment) transaction.getAttachment();
        if (attachment.getFuelLimit().compareTo(LIMIT_MIN_VALUE) < 0) {
            throw new AplUnacceptableTransactionValidationException("Fuel limit value less than MIN value, expected at least " + LIMIT_MIN_VALUE, transaction);
        }
        if (attachment.getFuelPrice().compareTo(PRICE_MIN_VALUE) < 0) {
            throw new AplUnacceptableTransactionValidationException("Fuel price value less than MIN value, expected at least " + PRICE_MIN_VALUE, transaction);
        }
    }
}
