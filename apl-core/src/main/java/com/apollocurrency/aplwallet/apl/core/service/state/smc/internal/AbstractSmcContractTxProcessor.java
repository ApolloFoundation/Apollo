/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.internal;

import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractTxProcessor;
import com.apollocurrency.smc.blockchain.BlockchainIntegrator;
import com.apollocurrency.smc.contract.ContractException;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.fuel.OutOfFuelException;
import com.apollocurrency.smc.contract.vm.ContractVirtualMachine;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.ResultValue;
import com.apollocurrency.smc.polyglot.PolyglotException;
import com.apollocurrency.smc.polyglot.engine.ExecutionEnv;
import lombok.extern.slf4j.Slf4j;

import static com.apollocurrency.aplwallet.apl.util.exception.ApiErrors.CONTRACT_PROCESSING_ERROR;

/**
 * Validate transaction, perform smart contract and manipulate balances
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public abstract class AbstractSmcContractTxProcessor implements SmcContractTxProcessor {
    protected final SmcConfig smcConfig;
    protected final ContractVirtualMachine smcMachine;
    private final SmartContract smartContract;
    private final ExecutionEnv executionEnv;
    private final BlockchainIntegrator integrator;

    protected AbstractSmcContractTxProcessor(SmcConfig smcConfig, BlockchainIntegrator integrator) {
        this(smcConfig, integrator, null);
    }

    protected AbstractSmcContractTxProcessor(SmcConfig smcConfig, BlockchainIntegrator integrator, SmartContract smartContract) {
        this.smartContract = smartContract;
        this.smcConfig = smcConfig;
        this.executionEnv = smcConfig.createExecutionEnv();
        this.integrator = integrator;
        this.smcMachine = new AplMachine(smcConfig.createLanguageContext(), executionEnv, integrator);
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
        return executionEnv;
    }

    @Override
    public ResultValue process(ExecutionLog executionLog) throws OutOfFuelException, PolyglotException {
        try {

            return executeContract(executionLog);

        } catch (OutOfFuelException | PolyglotException e) {
            putExceptionToLog(executionLog, e);
            throw e;
        }
    }

    @Override
    public void commit() {
        integrator.commit();
    }

    protected abstract ResultValue executeContract(ExecutionLog executionLog) throws OutOfFuelException, PolyglotException;

    protected String putExceptionToLog(ExecutionLog executionLog, Exception e) {
        var msg = String.format("Call method error %s:%s", e.getClass().getName(), e.getMessage());
        log.error(msg);
        ContractException smcException;
        if (e instanceof ContractException) {
            smcException = (ContractException) e;
        } else {
            smcException = new ContractException(e);
        }
        executionLog.add("Abstract processor", smcException);
        executionLog.setErrorCode(CONTRACT_PROCESSING_ERROR.getErrorCode());
        return msg;
    }

}
