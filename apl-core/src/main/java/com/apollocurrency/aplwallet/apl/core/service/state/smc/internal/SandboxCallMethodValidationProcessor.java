/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.internal;

import com.apollocurrency.smc.contract.ContractStatus;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.SmartMethod;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.SMCMachine;
import lombok.extern.slf4j.Slf4j;

import static com.apollocurrency.aplwallet.apl.util.exception.ApiErrors.CONTRACT_METHOD_VALIDATION_ERROR;

/**
 * Validate the smart contract - create and initialize the smart contract and manipulate balances in sandbox.
 * This validation process doesn't change the blockchain state.
 * This processor should be used during the state independent validation routine of the transaction
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class SandboxCallMethodValidationProcessor extends AbstractContractTxProcessor {
    private final SmartMethod smartMethod;

    public SandboxCallMethodValidationProcessor(SMCMachine smcMachine, SmartContract smartContract, SmartMethod smartMethod) {
        super(smcMachine, smartContract);
        this.smartMethod = smartMethod;
    }

    public SmartMethod smartMethod() {
        return smartMethod;
    }

    @Override
    public void executeContract(ExecutionLog executionLog) {
        boolean isValid;
        validateStatus(ContractStatus.ACTIVE, smartContract);
        isValid = smcMachine.validateMethod(smartContract, smartMethod, smartContract);
        executionLog.join(smcMachine.getExecutionLog());
        smcMachine.resetExecutionLog();
        if (!isValid) {
            executionLog.setErrorCode(CONTRACT_METHOD_VALIDATION_ERROR.getErrorCode());
        }
    }
}
