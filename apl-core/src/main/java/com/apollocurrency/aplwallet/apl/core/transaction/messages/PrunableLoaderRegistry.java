/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import javax.enterprise.inject.Instance;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;

public class PrunableLoaderRegistry {
    private final Map<Class<?>, PrunableLoader<?>> loaders = new HashMap<>();

    public PrunableLoaderRegistry(Instance<PrunableLoader<?>> prunableLoaderInstances) {
        prunableLoaderInstances.iterator().forEachRemaining(e-> {
            loaders.put((Class<?>) ((ParameterizedType)e.getClass().getGenericSuperclass()).getActualTypeArguments()[0], e);
        });

    }
}
