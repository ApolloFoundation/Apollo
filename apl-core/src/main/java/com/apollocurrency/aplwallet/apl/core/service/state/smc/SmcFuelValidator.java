/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.exception.AplUnacceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractSmcAttachment;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.function.Function;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class SmcFuelValidator {
    private final BlockchainConfig blockchainConfig;

    @Inject
    public SmcFuelValidator(BlockchainConfig blockchainConfig) {
        this.blockchainConfig = blockchainConfig;
    }

    public void validate(Transaction transaction) {
        AbstractSmcAttachment attachment = (AbstractSmcAttachment) transaction.getAttachment();
        validate(attachment, s -> new AplUnacceptableTransactionValidationException(s, transaction));
    }

    public void validate(AbstractSmcAttachment attachment) {
        validate(attachment, IllegalArgumentException::new);
    }

    public void validate(AbstractSmcAttachment attachment, Function<String, RuntimeException> exceptionSupplier) {
        if (attachment.getFuelLimit().compareTo(blockchainConfig.getCurrentConfig().getSmcFuelLimitMinValue()) < 0) {
            var msg = "Fuel limit value less than MIN value, expected at least " + blockchainConfig.getCurrentConfig().getSmcFuelLimitMinValue() + " ATM";
            throw exceptionSupplier.apply(msg);
        }
        if (attachment.getFuelLimit().compareTo(blockchainConfig.getCurrentConfig().getSmcFuelLimitMaxValue()) > 0) {
            var msg = "Fuel limit value greater than MAX value, expected up to " + blockchainConfig.getCurrentConfig().getSmcFuelLimitMaxValue() + " ATM";
            throw exceptionSupplier.apply(msg);
        }
        if (attachment.getFuelPrice().compareTo(blockchainConfig.getCurrentConfig().getSmcFuelPriceMinValue()) < 0) {
            var msg = "Fuel price value less than MIN value, expected at least " + blockchainConfig.getCurrentConfig().getSmcFuelPriceMinValue() + " ATM";
            throw exceptionSupplier.apply(msg);
        }
        if (attachment.getFuelPrice().compareTo(blockchainConfig.getCurrentConfig().getSmcFuelPriceMaxValue()) > 0) {
            var msg = "Fuel price value greater than MAX value, expected up to " + blockchainConfig.getCurrentConfig().getSmcFuelPriceMaxValue() + " ATM";
            throw exceptionSupplier.apply(msg);
        }
    }
}
