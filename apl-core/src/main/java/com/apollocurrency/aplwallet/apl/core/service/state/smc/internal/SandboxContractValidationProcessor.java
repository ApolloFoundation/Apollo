/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.internal;

import com.apollocurrency.smc.blockchain.BlockchainIntegrator;
import com.apollocurrency.smc.contract.ContractStatus;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import lombok.extern.slf4j.Slf4j;

import static com.apollocurrency.aplwallet.apl.util.exception.ApiErrors.CONTRACT_VALIDATION_ERROR;

/**
 * Validate the smart contract - create and initialize the smart contract and manipulate balances in sandbox.
 * This validation process doesn't change the blockchain state.
 * This processor should be used during the state independent validation routine of the transaction
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class SandboxContractValidationProcessor extends AbstractContractTxProcessor {

    public SandboxContractValidationProcessor(SmartContract smartContract, BlockchainIntegrator processor) {
        super(processor, smartContract);
    }

    @Override
    public void executeContract(ExecutionLog executionLog) {
        boolean isValid;
        validateStatus(ContractStatus.CREATED, smartContract);
        isValid = smcMachine.validate(smartContract, smartContract);
        executionLog.join(smcMachine.getExecutionLog());
        smcMachine.resetExecutionLog();
        if (!isValid) {
            executionLog.setErrorCode(CONTRACT_VALIDATION_ERROR.getErrorCode());
        }
    }
}
