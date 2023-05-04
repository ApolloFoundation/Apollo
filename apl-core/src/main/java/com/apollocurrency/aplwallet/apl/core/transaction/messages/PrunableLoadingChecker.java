/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class PrunableLoadingChecker {
    private final BlockchainConfig blockchainConfig;
    private final TimeService timeService;
    private final PropertiesHolder propertiesHolder;

    @Inject
    public PrunableLoadingChecker(BlockchainConfig blockchainConfig, TimeService timeService, PropertiesHolder propertiesHolder) {
        this.blockchainConfig = blockchainConfig;
        this.timeService = timeService;
        this.propertiesHolder = propertiesHolder;

    }

    public boolean shouldLoadPrunable(Transaction transaction, boolean includeExpiredPrunable) {
        return timeService.getEpochTime() - transaction.getTimestamp() <
            (includeExpiredPrunable && propertiesHolder.INCLUDE_EXPIRED_PRUNABLE() ?
                blockchainConfig.getMaxPrunableLifetime() :
                blockchainConfig.getMinPrunableLifetime());
    }
}
