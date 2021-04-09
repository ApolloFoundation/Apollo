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

/**
 * Validate transaction, perform smart contract and manipulate balances
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class CallMethodContractTxProcessor extends AbstractContractTxProcessor {
    private final SmartMethod smartMethod;

    public CallMethodContractTxProcessor(SMCMachine smcMachine, SmartContract smartContract, SmartMethod smartMethod) {
        super(smcMachine, smartContract);
        this.smartMethod = smartMethod;
    }

    @Override
    public void executeContract(ExecutionLog executionLog) {
        log.debug("Smart method={}", smartMethod);
        validateStatus(ContractStatus.ACTIVE, smartContract);
        //call the method and charge the fuel
        smcMachine.callMethod(smartContract, smartMethod, smartContract);
        executionLog.join(smcMachine.getExecutionLog());
        smcMachine.resetExecutionLog();
    }

}
