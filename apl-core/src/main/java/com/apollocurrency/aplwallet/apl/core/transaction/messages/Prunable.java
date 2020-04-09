package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import javax.enterprise.inject.spi.CDI;

public interface Prunable {
    PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();

    byte[] getHash();

    boolean hasPrunableData();

    void restorePrunableData(Transaction transaction, int blockTimestamp, int height);

    default boolean shouldLoadPrunable(Transaction transaction, boolean includeExpiredPrunable) {
        BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
        TimeService timeService = CDI.current().select(TimeService.class).get();
        return timeService.getEpochTime() - transaction.getTimestamp() <
            (includeExpiredPrunable && propertiesHolder.INCLUDE_EXPIRED_PRUNABLE() ?
                blockchainConfig.getMaxPrunableLifetime() :
                blockchainConfig.getMinPrunableLifetime());
    }
}
