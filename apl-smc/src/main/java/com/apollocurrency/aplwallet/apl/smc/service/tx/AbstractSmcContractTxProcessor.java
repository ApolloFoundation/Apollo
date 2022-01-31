/*
 * Copyright (c) 2021-2022. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.service.tx;

import com.apollocurrency.aplwallet.apl.smc.SmcContext;
import com.apollocurrency.aplwallet.apl.smc.service.SmcContractTxProcessor;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.vm.BaseContractMachine;
import com.apollocurrency.smc.contract.vm.ContractVirtualMachine;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.polyglot.engine.ExecutionEnv;
import com.apollocurrency.smc.polyglot.engine.ExecutionException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static com.apollocurrency.aplwallet.apl.util.exception.ApiErrors.CONTRACT_PROCESSING_ERROR;

/**
 * Validate transaction, perform smart contract and manipulate balances
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public abstract class AbstractSmcContractTxProcessor implements SmcContractTxProcessor {
    protected final ContractVirtualMachine machine;
    private final SmartContract smartContract;
    private final SmcContext context;
    @Getter
    private final ExecutionLog executionLog;

    protected AbstractSmcContractTxProcessor(SmcContext context) {
        this(null, context);
    }

    protected AbstractSmcContractTxProcessor(SmartContract smartContract, SmcContext context) {
        this.smartContract = smartContract;
        this.context = context;
        this.executionLog = new ExecutionLog();
        this.machine = new BaseContractMachine(context.getLanguageContext(), context.getExecutionEnv(), executionLog, context.getIntegratorFactory());
    }

    @Override
    public SmartContract getSmartContract() {
        if (smartContract == null) {
            throw new IllegalStateException("SmartContract is null");
        }
        return smartContract;
    }

    @Override
    public ExecutionEnv getExecutionEnv() {
        return context.getExecutionEnv();
    }

    @Override
    public void commit() {
        log.trace("SmcContractTxProcessor commits data.");
        machine.shutdown();
    }

    protected String putExceptionToLog(ExecutionLog executionLog, Exception e) {
        var msg = String.format("Call method error %s:%s", e.getClass().getName(), e.getMessage());
        ExecutionException smcException;
        if (e instanceof ExecutionException) {
            smcException = (ExecutionException) e;
        } else {
            smcException = new ExecutionException(msg, e);
        }
        executionLog.add("Abstract processor", smcException);
        executionLog.setErrorCode(CONTRACT_PROCESSING_ERROR.getErrorCode());
        return msg;
    }

}
