/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.internal;

import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractTxProcessor;
import com.apollocurrency.smc.blockchain.BlockchainIntegrator;
import com.apollocurrency.smc.contract.ContractException;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.vm.ContractVirtualMachine;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.polyglot.LanguageContextFactory;
import lombok.extern.slf4j.Slf4j;

import static com.apollocurrency.aplwallet.apl.util.exception.ApiErrors.CONTRACT_PROCESSING_ERROR;

/**
 * Validate transaction, perform smart contract and manipulate balances
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public abstract class AbstractContractTxProcessor implements ContractTxProcessor {
    protected final ContractVirtualMachine smcMachine;
    private final SmartContract smartContract;

    protected AbstractContractTxProcessor(BlockchainIntegrator integrator) {
        this(integrator, null);
    }

    protected AbstractContractTxProcessor(BlockchainIntegrator integrator, SmartContract smartContract) {
        this.smcMachine = new AplMachine(LanguageContextFactory.createDefaultLanguageContext(), integrator);
        this.smartContract = smartContract;
    }

    @Override
    public SmartContract getSmartContract() {
        if (smartContract == null) {
            throw new IllegalStateException("SmartContract is null");
        }
        return smartContract;
    }

    @Override
    public ExecutionLog process() {
        ExecutionLog executionLog = new ExecutionLog();
        try {

            executeContract(executionLog);

        } catch (Exception e) {
            log.error("Contract processing error {}:{}", e.getClass().getName(), e.getMessage());
            ContractException smcException;
            if (e instanceof ContractException) {
                smcException = (ContractException) e;
            } else {
                smcException = new ContractException(e);
            }
            executionLog.add("Abstract processor", smcException);
            executionLog.setErrorCode(CONTRACT_PROCESSING_ERROR.getErrorCode());
        }

        return executionLog;
    }

    protected abstract void executeContract(ExecutionLog executionLog);

}
