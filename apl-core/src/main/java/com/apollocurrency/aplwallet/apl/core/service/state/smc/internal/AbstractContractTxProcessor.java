/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.internal;

import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractTxProcessor;
import com.apollocurrency.smc.SMCException;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.SMCMachine;
import com.apollocurrency.smc.contract.vm.operation.OperationProcessor;
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
    protected final OperationProcessor processor;
    protected final SMCMachine smcMachine;
    protected final SmartContract smartContract;

    protected AbstractContractTxProcessor(OperationProcessor processor, SmartContract smartContract) {
        this.processor = processor;
        this.smartContract = smartContract;
        this.smcMachine = new AplMachine(LanguageContextFactory.createDefaultLanguageContext(), processor);
    }

    @Override
    public SmartContract smartContract() {
        return smartContract;
    }

    @Override
    public ExecutionLog process() {
        ExecutionLog executionLog = new ExecutionLog();
        try {

            executeContract(executionLog);

        } catch (Exception e) {
            log.error("Contract processing error {}:{}", e.getClass().getName(), e.getMessage());
            SMCException smcException;
            if (e instanceof SMCException) {
                smcException = (SMCException) e;
            } else {
                smcException = new SMCException(e);
            }
            executionLog.add("Abstract processor", smcException);
            executionLog.setErrorCode(CONTRACT_PROCESSING_ERROR.getErrorCode());
        }

        return executionLog;
    }

    protected abstract void executeContract(ExecutionLog executionLog);

}
