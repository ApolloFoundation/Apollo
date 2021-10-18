/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.service.tx;

import com.apollocurrency.aplwallet.apl.smc.SmcContext;
import com.apollocurrency.smc.blockchain.BlockchainIntegrator;
import com.apollocurrency.smc.contract.vm.BaseContractMachine;
import com.apollocurrency.smc.polyglot.engine.ExecutionEnv;
import com.apollocurrency.smc.polyglot.language.LanguageContext;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class AplMachine extends BaseContractMachine {

    AplMachine(LanguageContext languageContext, ExecutionEnv env, BlockchainIntegrator integrator) {
        super(languageContext, env, integrator);
    }

    AplMachine(SmcContext context) {
        super(context.getLanguageContext(), context.getExecutionEnv(), context.getIntegrator());
    }
}
