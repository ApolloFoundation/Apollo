/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.internal;

import com.apollocurrency.smc.SMCException;
import com.apollocurrency.smc.contract.ContractStatus;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.SmartMethod;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.SMCMachine;
import lombok.extern.slf4j.Slf4j;

/**
 * Validate the smart contract - create and initialize the smart contract and manipulate balances in sandbox.
 * This validation process doesn't change the blockchain state.
 * This processor should be used during the state independent validation routine of the transaction
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class SandboxCallMethodValidationProcessor implements ContractTxProcessor {
    private final SMCMachine smcMachine;
    private final SmartContract smartContract;
    private final SmartMethod smartMethod;

    public SandboxCallMethodValidationProcessor(SMCMachine smcMachine, SmartContract smartContract, SmartMethod smartMethod) {
        this.smcMachine = smcMachine;
        this.smartContract = smartContract;
        this.smartMethod = smartMethod;
    }

    @Override
    public SmartContract smartContract() {
        return smartContract;
    }

    public SmartMethod smartMethod() {
        return smartMethod;
    }

    @Override
    public ExecutionLog process() {
        boolean isValid;
        ExecutionLog executionLog = new ExecutionLog();
        validateStatus(ContractStatus.ACTIVE, smartContract);

        try {
            isValid = smcMachine.validateMethod(smartContract, smartMethod);
            executionLog.join(smcMachine.getExecutionLog());
            smcMachine.resetExecutionLog();
            if (!isValid) {
                //TODO: Update the Error code
                executionLog.setErrorCode(1L);
            }
        } catch (Exception e) {
            SMCException smcException;
            if (e instanceof SMCException) {
                smcException = (SMCException) e;
            } else {
                smcException = new SMCException(e);
            }
            executionLog.add("validateCallingMethod", smcException);
            executionLog.setErrorCode(1L);
        }

        return executionLog;
    }
}
