/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.internal;

import com.apollocurrency.smc.contract.ContractState;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.SMCMachine;
import lombok.extern.slf4j.Slf4j;

/**
 * Validate transaction, perform smart contract and manipulate balances
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class PublishContractTxProcessor implements ContractTxProcessor {
    private final SMCMachine smcMachine;
    private final SmartContract smartContract;

    public PublishContractTxProcessor(SMCMachine smcMachine, SmartContract smartContract) {
        this.smcMachine = smcMachine;
        this.smartContract = smartContract;
    }

    @Override
    public SmartContract smartContract() {
        return smartContract;
    }

    @Override
    public ExecutionLog process() {
        ExecutionLog executionLog = new ExecutionLog();
        validateState(ContractState.CREATED, smartContract);
        smartContract.setState(ContractState.PUBLISHED);

        //call smart contract constructor, charge the fuel
        smcMachine.callConstructor(smartContract);
        executionLog.join(smcMachine.getExecutionLog());
        validateState(ContractState.ACTIVE, smartContract);

        smcMachine.resetExecutionLog();

        return executionLog;
    }

}
