/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixApplierRegistryInitializer;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixValidatorRegistryInitializer;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadersInitializer;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TxInitializer {
    @Inject
    TransactionTypesInitializer initializer;
    @Inject
    PrunableLoadersInitializer prunableLoadersInitializer;
    @Inject
    AppendixApplierRegistryInitializer registryInitializer;
    @Inject
    AppendixValidatorRegistryInitializer validatorRegistryInitializer;
}
