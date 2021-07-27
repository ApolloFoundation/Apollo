/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.internal;

import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.smc.blockchain.BlockchainIntegrator;
import com.apollocurrency.smc.contract.ContractStatus;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.ResultValue;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.apollocurrency.aplwallet.apl.util.exception.ApiErrors.CONTRACT_VALIDATION_ERROR;

/**
 * Validate the smart contract - create and initialize the smart contract and manipulate balances in sandbox.
 * This validation process doesn't change the blockchain state.
 * This processor should be used during the state independent validation routine of the transaction
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class PublishContractTxValidator extends AbstractSmcContractTxProcessor {

    public PublishContractTxValidator(SmartContract smartContract, BlockchainIntegrator processor, SmcConfig smcConfig) {
        super(smcConfig, processor, smartContract);
    }

    @Override
    public ResultValue executeContract(ExecutionLog executionLog) {
        validateStatus(ContractStatus.CREATED);
        var result = ResultValue.from(getSmartContract());
        var isValid = smcMachine.validateContract(getSmartContract());
        if (!isValid) {
            executionLog.setErrorCode(CONTRACT_VALIDATION_ERROR.getErrorCode());
            result.setErrorCode(CONTRACT_VALIDATION_ERROR.getErrorCode());
            result.setOutput(List.of(false));
            result.setErrorDescription(smcMachine.getExecutionLog().toJsonString());
        }
        executionLog.join(smcMachine.getExecutionLog());
        smcMachine.resetExecutionLog();
        return result;
    }
}
