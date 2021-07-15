/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.util.cdi.config.Property;
import lombok.Getter;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Configuration for the {@link FailedTransactionVerificationService} consisting of user-defined properties typically
 * loaded from the apl-blockchain.properties files using {@link com.apollocurrency.aplwallet.apl.util.env.config.PropertiesConfigLoader}
 * @author Andrii Boiarskyi
 * @see FailedTransactionVerificationService
 * @since 1.48.4
 */
@Singleton
@Getter
public class FailedTransactionVerificationConfig {
    private final int confirmations;
    private final int threads;

    @Inject
    public FailedTransactionVerificationConfig(
        @Property(name = "apl.numberOfFailedTransactionConfirmations", defaultValue = "3")
            int confirmations,
        @Property(name = "apl.numberOfFailedTransactionsProcessingThreads", defaultValue = "10")
            int threads) {
        if (threads < 1) {
            throw new IllegalArgumentException("Required at least 1 processing thread for failed transactions verification, got " + threads);
        }
        this.confirmations = confirmations;
        this.threads = threads;
    }

    public boolean isEnabled() {
        return confirmations > 0;
    }
}
