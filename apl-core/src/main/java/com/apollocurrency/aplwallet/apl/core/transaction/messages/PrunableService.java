/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class PrunableService {
    private final BlockchainConfig blockchainConfig;
    private final TimeService timeService;
    private final PropertiesHolder propertiesHolder;
    private final Map<Class<?>, PrunableLoader<?>> loaders = new HashMap<>();

    @Inject
    public PrunableService(Instance<PrunableLoader<?>> prunableLoaderInstances, BlockchainConfig blockchainConfig, TimeService timeService, PropertiesHolder propertiesHolder) {
        this.blockchainConfig = blockchainConfig;
        this.timeService = timeService;
        this.propertiesHolder = propertiesHolder;
        prunableLoaderInstances.iterator().forEachRemaining(e-> {
            loaders.put((Class<?>) ((ParameterizedType)e.getClass().getGenericSuperclass()).getActualTypeArguments()[0], e);
        });
    }

    public boolean shouldLoadPrunable(Transaction transaction, boolean includeExpiredPrunable) {
        return timeService.getEpochTime() - transaction.getTimestamp() <
            (includeExpiredPrunable && propertiesHolder.INCLUDE_EXPIRED_PRUNABLE() ?
                blockchainConfig.getMaxPrunableLifetime() :
                blockchainConfig.getMinPrunableLifetime());
    }

    public <T extends Appendix> void loadPrunable(Transaction transaction, T prunable, boolean includeExpiredPrunable) {
        PrunableLoader<T> prunableLoader = (PrunableLoader<T>) loaders.get(prunable.getClass());
        if (prunableLoader != null) {
            prunableLoader.loadPrunable(transaction, prunable, includeExpiredPrunable);
        }
    }

    public <T extends Appendix> void restorePrunable(Transaction transaction, T prunable, int blockTimestamp, int height) {
        PrunableLoader<T> prunableLoader = (PrunableLoader<T>) loaders.get(prunable.getClass());
        if (prunableLoader != null) {
            prunableLoader.restorePrunableData(transaction, prunable, blockTimestamp, height);
        }
    }
}
