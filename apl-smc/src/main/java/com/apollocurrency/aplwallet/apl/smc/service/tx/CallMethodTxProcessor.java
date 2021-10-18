/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.service.tx;

import com.apollocurrency.aplwallet.apl.smc.SmcContext;
import com.apollocurrency.smc.contract.ContractStatus;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.SmartMethod;
import com.apollocurrency.smc.contract.fuel.OutOfFuelException;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.ResultValue;
import com.apollocurrency.smc.polyglot.PolyglotException;
import lombok.extern.slf4j.Slf4j;

/**
 * Validate transaction, perform smart contract and manipulate balances
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class CallMethodTxProcessor extends AbstractSmcContractTxProcessor {
    private final SmartMethod smartMethod;

    public CallMethodTxProcessor(SmartContract smartContract, SmartMethod smartMethod, SmcContext context) {
        super(smartContract, context);
        this.smartMethod = smartMethod;
    }

    @Override
    public ResultValue executeContract(ExecutionLog executionLog) throws OutOfFuelException, PolyglotException {
        log.debug("Smart method={}", smartMethod);
        validateStatus(ContractStatus.ACTIVE);
        //call the method and charge the fuel
        var result = smcMachine.evalPayableMethod(getSmartContract(), smartMethod);
        executionLog.join(smcMachine.getExecutionLog());
        smcMachine.resetExecutionLog();
        return result;
    }

}
