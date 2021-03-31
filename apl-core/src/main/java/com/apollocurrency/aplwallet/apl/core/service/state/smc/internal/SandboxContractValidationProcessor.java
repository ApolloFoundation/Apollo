/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.internal;

import com.apollocurrency.smc.contract.ContractStatus;
import com.apollocurrency.smc.contract.SmartContract;
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
public class SandboxContractValidationProcessor implements ContractTxProcessor {
    private final SMCMachine smcMachine;
    private final SmartContract smartContract;

    public SandboxContractValidationProcessor(SMCMachine smcMachine, SmartContract smartContract) {
        this.smcMachine = smcMachine;
        this.smartContract = smartContract;
    }

    @Override
    public SmartContract smartContract() {
        return smartContract;
    }

    @Override
    public ExecutionLog process() {
        boolean isValid;
        validateStatus(ContractStatus.CREATED, smartContract);

        isValid = smcMachine.validate(smartContract);
        ExecutionLog executionLog = new ExecutionLog(smcMachine.getExecutionLog());
        if (!isValid) {
            //TODO: Update the Error code
            executionLog.setErrorCode(1L);
        }
        return executionLog;
    }
}
