/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.internal;

import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractCmdProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractTransactionDispatcher;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.CallMethodContractCmdProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.PublishContractCmdProcessor;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.smc.blockchain.crypt.CryptoLibProvider;
import com.apollocurrency.smc.contract.vm.SMCMachineFactory;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import java.util.EnumMap;
import java.util.Map;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class ContractTransactionDispatcherFactory {
    @Produces
    @Named("aplProcessors")
    public Map<TransactionTypes.TransactionTypeSpec, ContractCmdProcessor> getRegisteredProcessors(ContractService contractManager) {

        Map<TransactionTypes.TransactionTypeSpec, ContractCmdProcessor> registeredProcessors = new EnumMap<>(TransactionTypes.TransactionTypeSpec.class);
        registeredProcessors.put(TransactionTypes.TransactionTypeSpec.SMC_PUBLISH,
            new PublishContractCmdProcessor(contractManager)
        );

        registeredProcessors.put(TransactionTypes.TransactionTypeSpec.SMC_CALL_METHOD,
            new CallMethodContractCmdProcessor(contractManager)
        );

        return registeredProcessors;
    }

    @Produces
    public ContractTransactionDispatcher createDispatcher(CryptoLibProvider cryptoLibProvider,
                                                          SMCMachineFactory machineFactory,
                                                          @Named("aplProcessors") Map<TransactionTypes.TransactionTypeSpec, ContractCmdProcessor> registeredProcessors) {
        return new BaseContractTransactionDispatcher(cryptoLibProvider, machineFactory, registeredProcessors);
    }
}
