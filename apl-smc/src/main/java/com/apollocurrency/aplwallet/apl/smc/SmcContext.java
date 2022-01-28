/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc;

import com.apollocurrency.smc.blockchain.BlockchainIntegratorFactory;
import com.apollocurrency.smc.contract.fuel.OperationPrice;
import com.apollocurrency.smc.polyglot.engine.ExecutionEnv;
import com.apollocurrency.smc.polyglot.language.LanguageContext;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface SmcContext {
    BlockchainIntegratorFactory getIntegratorFactory();

    ExecutionEnv getExecutionEnv();

    LanguageContext getLanguageContext();

    OperationPrice getPrice();
}
