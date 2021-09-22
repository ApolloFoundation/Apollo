/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.service.tx;

import com.apollocurrency.aplwallet.apl.smc.SmcContext;
import com.apollocurrency.aplwallet.apl.smc.service.SmcContractTxBatchProcessor;
import com.apollocurrency.smc.contract.ContractStatus;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.SmartMethod;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.ResultValue;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Call @view method of the given smart contract
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class CallViewMethodTxProcessor extends AbstractSmcContractTxProcessor implements SmcContractTxBatchProcessor {
    private final List<SmartMethod> smartMethods;

    public CallViewMethodTxProcessor(SmartContract smartContract, List<SmartMethod> smartMethods, SmcContext context) {
        super(smartContract, context);
        this.smartMethods = smartMethods;
    }

    @Override
    public List<ResultValue> batchProcess(ExecutionLog executionLog) {
        try {

            return batchExecuteContract(executionLog);

        } catch (Exception e) {
            var msg = putExceptionToLog(executionLog, e);
            log.error(msg, e);
            return List.of();
        }
    }

    private List<ResultValue> batchExecuteContract(ExecutionLog executionLog) {
        log.debug("Smart method={}", smartMethods);
        validateStatus(ContractStatus.ACTIVE);
        //call the method and charge the fuel
        var result = smcMachine.callViewMethod(getSmartContract(), smartMethods);
        executionLog.join(smcMachine.getExecutionLog());
        smcMachine.resetExecutionLog();
        return result;
    }

    @Override
    protected ResultValue executeContract(ExecutionLog executionLog) {
        return ResultValue.UNDEFINED_RESULT;
    }
}
