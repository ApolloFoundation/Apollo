/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.internal;

import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractTxProcessor;
import com.apollocurrency.smc.SMCException;
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
public class CallMethodContractTxProcessor implements ContractTxProcessor {
    private final SMCMachine smcMachine;
    private final SmartContract smartContract;
    private final SmartMethod smartMethod;

    public CallMethodContractTxProcessor(SMCMachine smcMachine, SmartContract smartContract, SmartMethod smartMethod) {
        this.smcMachine = smcMachine;
        this.smartContract = smartContract;
        this.smartMethod = smartMethod;
    }

    @Override
    public SmartContract smartContract() {
        return smartContract;
    }

    @Override
    public ExecutionLog process() {
        log.debug("Smart method={}", smartMethod);
        ExecutionLog executionLog = new ExecutionLog();
        validateStatus(ContractStatus.ACTIVE, smartContract);

        try {
            //call the method and charge the fuel
            smcMachine.callMethod(smartContract, smartMethod);
            executionLog.join(smcMachine.getExecutionLog());
            smcMachine.resetExecutionLog();
        } catch (Exception e) {
            SMCException smcException;
            if (e instanceof SMCException) {
                smcException = (SMCException) e;
            } else {
                smcException = new SMCException(e);
            }
            executionLog.add("callMethod", smcException);
            executionLog.setErrorCode(1L);
        }

        return executionLog;
    }

}
