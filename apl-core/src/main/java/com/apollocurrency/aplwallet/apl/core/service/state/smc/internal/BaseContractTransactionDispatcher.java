/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.internal;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractCmdProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractTransactionDispatcher;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.smc.IllegalStateException;
import com.apollocurrency.smc.SMCException;
import com.apollocurrency.smc.blockchain.crypt.CryptoLibProvider;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.SMCMachine;
import com.apollocurrency.smc.contract.vm.SMCMachineFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class BaseContractTransactionDispatcher implements ContractTransactionDispatcher {
    protected final CryptoLibProvider cryptoLibProvider;
    private final SMCMachineFactory machineFactory;
    private final Map<TransactionTypes.TransactionTypeSpec, ContractCmdProcessor> registeredProcessors;

    public BaseContractTransactionDispatcher(CryptoLibProvider cryptoLibProvider, SMCMachineFactory machineFactory) {
        this(cryptoLibProvider, machineFactory, new EnumMap<>(TransactionTypes.TransactionTypeSpec.class));
    }

    public BaseContractTransactionDispatcher(CryptoLibProvider cryptoLibProvider,
                                             SMCMachineFactory machineFactory,
                                             Map<TransactionTypes.TransactionTypeSpec, ContractCmdProcessor> registeredProcessors) {
        this.cryptoLibProvider = Objects.requireNonNull(cryptoLibProvider);
        this.registeredProcessors = Objects.requireNonNull(registeredProcessors);
        this.machineFactory = Objects.requireNonNull(machineFactory);
    }

    @Override
    public ContractCmdProcessor registerProcessor(TransactionTypes.TransactionTypeSpec transactionSpec,
                                                  ContractCmdProcessor processor) {
        return registeredProcessors.put(transactionSpec, processor);
    }

    @Override
    public ExecutionLog dispatch(Transaction smcTransaction) {
        ContractCmdProcessor proc = registeredProcessors.get(smcTransaction.getType().getSpec());
        if (proc != null) {
            try {
                SMCMachine machine = machineFactory.createNewInstance();
                return proc.process(machine, smcTransaction);
            } catch (SMCException e) {
                return new ExecutionLog("dispatch", e);
            } catch (Exception e) {
                log.error("Dispatch error: {}", e.getMessage());
                throw e;
            }
        } else {
            throw new IllegalStateException("Error: suitable processor not found, tx type=" + smcTransaction.getType());
        }
    }
}
