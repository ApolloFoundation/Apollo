package com.apollocurrency.aplwallet.apl.core.service.state.smc.internal;

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
}
