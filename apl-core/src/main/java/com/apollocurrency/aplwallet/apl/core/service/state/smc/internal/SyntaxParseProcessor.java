/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.internal;

import com.apollocurrency.smc.blockchain.BlockchainIntegrator;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

import static com.apollocurrency.aplwallet.apl.util.exception.ApiErrors.CONTRACT_METHOD_VALIDATION_ERROR;

/**
 * Validate the smart contract - create and initialize the smart contract and manipulate balances in sandbox.
 * This validation process doesn't change the blockchain state.
 * This processor should be used during the state independent validation routine of the transaction
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class SyntaxParseProcessor extends AbstractContractTxProcessor {
    private final String script;

    public SyntaxParseProcessor(String script, BlockchainIntegrator processor) {
        super(processor);
        this.script = Objects.requireNonNull(script);
    }

    @Override
    public void executeContract(ExecutionLog executionLog) {
        boolean isValid = smcMachine.parse(script);
        executionLog.join(smcMachine.getExecutionLog());
        smcMachine.resetExecutionLog();
        if (!isValid) {
            executionLog.setErrorCode(CONTRACT_METHOD_VALIDATION_ERROR.getErrorCode());
        }
    }
}
