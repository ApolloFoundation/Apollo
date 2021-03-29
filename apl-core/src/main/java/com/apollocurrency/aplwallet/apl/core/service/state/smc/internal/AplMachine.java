package com.apollocurrency.aplwallet.apl.core.service.state.smc.internal;

import com.apollocurrency.smc.blockchain.BlockchainIntegrator;
import com.apollocurrency.smc.contract.vm.BaseContractMachine;
import com.apollocurrency.smc.polyglot.LanguageContext;
import com.apollocurrency.smc.polyglot.config.JsLimitsConfig;
import com.apollocurrency.smc.polyglot.engine.ExecutionEnv;
import com.apollocurrency.smc.polyglot.engine.ExecutionModeHelper;

import javax.inject.Inject;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class AplMachine extends BaseContractMachine {

    @Inject
    public AplMachine(LanguageContext languageContext, BlockchainIntegrator blockchainIntegrator) {
        super(languageContext,
            ExecutionEnv.builder()
                .mode(ExecutionModeHelper.createProdExecutionMode())
                .config(new JsLimitsConfig())
                .build(),
            blockchainIntegrator);
    }

    public AplMachine(LanguageContext languageContext, ExecutionEnv env, BlockchainIntegrator blockchainIntegrator) {
        super(languageContext, env, blockchainIntegrator);
    }
}
