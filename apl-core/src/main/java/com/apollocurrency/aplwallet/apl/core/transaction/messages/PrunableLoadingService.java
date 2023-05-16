/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Singleton
public class PrunableLoadingService {
    private final Map<Class<?>, PrunableLoader<?>> loaders = new HashMap<>();

    public PrunableLoadingService() {

    }

    public void initLoaders(Instance<PrunableLoader<?>> prunableLoaderInstances) {
        prunableLoaderInstances.iterator().forEachRemaining(e-> {
            loaders.put(e.forClass(), e);
        });
    }

    public <T extends Appendix> void loadPrunable(Transaction transaction, T prunable, boolean includeExpiredPrunable) {
        if (transaction.isFailed()) {
            return;
        }
        doForExistingLoader(prunable, loader -> loader.loadPrunable(transaction, prunable, includeExpiredPrunable));
    }

    private <T extends Appendix> void doForExistingLoader(T prunable, Consumer<PrunableLoader<T>> consumer) {
        PrunableLoader<?> loader = loaders.get(prunable.getClass());
        if (loader != null) {
            PrunableLoader<T> prunableLoader = (PrunableLoader<T>) loader;
            consumer.accept(prunableLoader);
        }
    }

    public void loadTransactionPrunables(Transaction transaction) {
        if (transaction.isFailed()) {
            return;
        }
        for (AbstractAppendix appendage : transaction.getAppendages()) {
            loadPrunable(transaction, appendage, false);
        }
    }

    public <T extends Appendix> void restorePrunable(Transaction transaction, T prunable, int blockTimestamp, int height) {
        if (transaction.isFailed()) {
            return;
        }
        doForExistingLoader(prunable, (loader) -> loader.restorePrunableData(transaction, prunable, blockTimestamp, height));
    }
}
