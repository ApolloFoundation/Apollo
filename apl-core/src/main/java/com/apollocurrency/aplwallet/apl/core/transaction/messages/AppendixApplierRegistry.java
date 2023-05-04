/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class AppendixApplierRegistry {
    private final Map<Class<?>, AppendixApplier<?>> appliers = new HashMap<>();

    public AppendixApplierRegistry() {
    }

    void init(Instance<AppendixApplier<?>> instances) {
        instances.iterator().forEachRemaining(e -> appliers.put(e.forClass(), e));
    }

    public AppendixApplierRegistry(Collection<AppendixApplier<?>> appliers) {
        appliers.iterator().forEachRemaining(e -> this.appliers.put(e.forClass(), e));
    }

    public <T extends Appendix> AppendixApplier<T> getFor(T t) {
        return (AppendixApplier<T>) appliers.get(t.getClass());
    }
}
