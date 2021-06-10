/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.cache;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlType;
import com.apollocurrency.aplwallet.apl.util.cache.CacheConfigurator;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;

import static com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager.MemoryUsageCalculator.INT_SIZE;
import static com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager.MemoryUsageCalculator.LONG_SIZE;

public class AccountCacheConfig extends CacheConfigurator {

    public static final String CACHE_NAME = "ACCOUNT_CACHE";

    public AccountCacheConfig(int priority) {
        super(CACHE_NAME,
            getAccountSize(),
            priority, null, true);
    }

    static int getAccountSize() {
        return InMemoryCacheManager.newCalc()
            .addLongPrimitive() //dbId
            .addLongPrimitive() // accountId
            .addLongPrimitive() // parentId
            .addBooleanPrimitive() //multisig
            .addAggregation(INT_SIZE) // addrScope
            .addAggregation(PublicKeyCacheConfig.getPublicKeySize()) // add size of public key object
            .addLongPrimitive() //  balanceATM
            .addLongPrimitive() // unconfirmedBalanceATM
            .addLongPrimitive() // forgedBalanceATM
            .addLongPrimitive() // activeLesseeId
            .addAggregation( // accountControls
                InMemoryCacheManager.newCalc() // inside the unmodifiable collection
                    .addAggregation( // reference to enumset
                        InMemoryCacheManager.newCalc()
                            .addLongPrimitive()  // number of elements
                            .addArrayExtra(AccountControlType.values().length * 8) // array elements inside the EnumSet only referenced to enum accounted
                            .addReference() // enum  class reference
                            .calc()
                    ).calc())
            .addBooleanPrimitive() //latest
            .addInt() //height
            .addAggregation(LONG_SIZE) //dbKey object
            .addBooleanPrimitive() // deleted
            .calc();
    }
}
