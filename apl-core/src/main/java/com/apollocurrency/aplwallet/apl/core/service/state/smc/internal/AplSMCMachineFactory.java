package com.apollocurrency.aplwallet.apl.core.service.state.smc.internal;

import com.apollocurrency.smc.blockchain.BlockchainIntegrator;
import com.apollocurrency.smc.contract.vm.SMCMachine;
import com.apollocurrency.smc.contract.vm.SMCMachineFactory;
import com.apollocurrency.smc.polyglot.LanguageContext;
import com.apollocurrency.smc.polyglot.engine.ExecutionEnv;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class AplSMCMachineFactory implements SMCMachineFactory {

    private final LanguageContext languageContext;
    private final BlockchainIntegrator blockchainIntegrator;

    @Inject
    public AplSMCMachineFactory(LanguageContext languageContext, BlockchainIntegrator blockchainIntegrator) {
        this.languageContext = Objects.requireNonNull(languageContext);
        this.blockchainIntegrator = Objects.requireNonNull(blockchainIntegrator);
    }

    public SMCMachine createNewInstance() {
        return new AplMachine(languageContext, blockchainIntegrator);
    }

    public SMCMachine createNewInstance(ExecutionEnv env) {
        return new AplMachine(languageContext, env, blockchainIntegrator);
    }
}
