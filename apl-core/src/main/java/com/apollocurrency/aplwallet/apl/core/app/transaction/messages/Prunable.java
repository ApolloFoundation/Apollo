package com.apollocurrency.aplwallet.apl.core.app.transaction.messages;

import javax.enterprise.inject.spi.CDI;

import com.apollocurrency.aplwallet.apl.core.app.Constants;
import com.apollocurrency.aplwallet.apl.core.app.Time;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;

public interface Prunable {

    byte[] getHash();

    boolean hasPrunableData();

    void restorePrunableData(Transaction transaction, int blockTimestamp, int height);

    default boolean shouldLoadPrunable(Transaction transaction, boolean includeExpiredPrunable) {
        BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
        Time.EpochTime timeService = CDI.current().select(Time.EpochTime.class).get();
        return timeService.getEpochTime() - transaction.getTimestamp() <
                (includeExpiredPrunable && Constants.INCLUDE_EXPIRED_PRUNABLE ?
                        blockchainConfig.getMaxPrunableLifetime() :
                        blockchainConfig.getMinPrunableLifetime());
    }
}
