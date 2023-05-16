/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.cache;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.util.cache.CacheConfigurator;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;

import static com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager.MemoryUsageCalculator.LONG_SIZE;

public class PublicKeyCacheConfig extends CacheConfigurator<LongKey, PublicKey> {

    public static final String PUBLIC_KEY_CACHE_NAME = "PUBLIC_KEY_CACHE";

    public PublicKeyCacheConfig(int priority) {
        super(PUBLIC_KEY_CACHE_NAME,
            getPublicKeySize(),
            priority, null, true);
    }

    static int getPublicKeySize() {
        return InMemoryCacheManager.newCalc()
            .addLongPrimitive() // accountId
            .addArrayExtra(32) //publickey byte array
            .addBooleanPrimitive() //latest
            .addLongPrimitive() //dbId
            .addInt() //height
            .addAggregation(LONG_SIZE) //dbKey object
            .calc();
    }
}
