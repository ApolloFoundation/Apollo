/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.util.cdi.config.Property;
import lombok.Getter;
import lombok.ToString;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.UUID;

/**
 * Configuration for the {@link FailedTransactionVerificationService} consisting of user-defined properties typically
 * loaded from the apl-blockchain.properties files using {@link com.apollocurrency.aplwallet.apl.util.env.config.PropertiesConfigLoader}
 * and chain configuration from {@link BlockchainConfig}
 * @author Andrii Boiarskyi
 * @see FailedTransactionVerificationService
 * @since 1.48.4
 */
@Singleton
@Getter
@ToString
public class FailedTransactionVerificationConfig {
    private final int confirmations;
    private final int threads;
    private final UUID chainId;
    private final Integer failedTxsActivationHeight;

    @Inject
    public FailedTransactionVerificationConfig(
        BlockchainConfig blockchainConfig,
        @Property(name = "apl.numberOfFailedTransactionConfirmations", defaultValue = "3")
            int confirmations,
        @Property(name = "apl.numberOfFailedTransactionsProcessingThreads", defaultValue = "10")
            int threads) {
        this(confirmations, threads, blockchainConfig.getChain().getChainId(), blockchainConfig.getFailedTransactionsAcceptanceActivationHeight().orElse(null));
    }

    public FailedTransactionVerificationConfig(int confirmations, int threads, UUID chainId, Integer failedTxsActivationHeight) {
        if (threads < 1) {
            throw new IllegalArgumentException("Required at least 1 processing thread for failed transactions verification, got " + threads);
        }
        this.confirmations = confirmations;
        this.threads = threads;
        this.chainId = chainId;
        this.failedTxsActivationHeight = failedTxsActivationHeight;
    }

    public Optional<Integer> getFailedTxsActivationHeight() {
        return Optional.ofNullable(failedTxsActivationHeight);
    }

    public boolean isEnabled() {
        return confirmations > 0 && failedTxsActivationHeight != null;
    }
}
