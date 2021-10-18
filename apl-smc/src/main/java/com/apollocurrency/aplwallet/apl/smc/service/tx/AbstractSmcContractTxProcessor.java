/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.service.tx;

import com.apollocurrency.aplwallet.apl.smc.SmcContext;
import com.apollocurrency.aplwallet.apl.smc.service.SmcContractTxProcessor;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.fuel.OutOfFuelException;
import com.apollocurrency.smc.contract.vm.ContractVirtualMachine;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.ResultValue;
import com.apollocurrency.smc.polyglot.PolyglotException;
import com.apollocurrency.smc.polyglot.engine.ExecutionEnv;
import com.apollocurrency.smc.polyglot.engine.ExecutionException;
import lombok.extern.slf4j.Slf4j;

import static com.apollocurrency.aplwallet.apl.util.exception.ApiErrors.CONTRACT_PROCESSING_ERROR;

/**
 * Validate transaction, perform smart contract and manipulate balances
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public abstract class AbstractSmcContractTxProcessor implements SmcContractTxProcessor {
    protected final ContractVirtualMachine smcMachine;
    private final SmartContract smartContract;
    private final SmcContext context;

    protected AbstractSmcContractTxProcessor(SmcContext context) {
        this(null, context);
    }

    protected AbstractSmcContractTxProcessor(SmartContract smartContract, SmcContext context) {
        this.smartContract = smartContract;
        this.context = context;
        this.smcMachine = new AplMachine(context);
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
    public ResultValue process(ExecutionLog executionLog) throws OutOfFuelException, PolyglotException {
        try {

            return executeContract(executionLog);

        } catch (PolyglotException e) {
            var msg = putExceptionToLog(executionLog, e);
            log.error(msg);
            throw e;
        }
    }

    @Override
    public void commit() {
        context.getIntegrator().commit();
    }

    protected abstract ResultValue executeContract(ExecutionLog executionLog) throws OutOfFuelException, PolyglotException;

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
