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
 * Validate transaction, perform smart contract and manipulate balances
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class PublishContractTxProcessor extends AbstractContractTxProcessor {

    public PublishContractTxProcessor(SMCMachine smcMachine, SmartContract smartContract) {
        super(smcMachine, smartContract);
    }

    @Override
    protected void executeContract(ExecutionLog executionLog) {
        validateStatus(ContractStatus.CREATED, smartContract);
        smartContract.setStatus(ContractStatus.PUBLISHED);
        //call smart contract constructor, charge the fuel
        smcMachine.publishContract(smartContract, smartContract);
        executionLog.join(smcMachine.getExecutionLog());
        validateStatus(ContractStatus.ACTIVE, smartContract);
        smcMachine.resetExecutionLog();
    }

}
