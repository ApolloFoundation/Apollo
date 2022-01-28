/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.service.tx;

import com.apollocurrency.aplwallet.apl.smc.SmcContext;
import com.apollocurrency.aplwallet.apl.smc.service.SmcContractTxBatchProcessor;
import com.apollocurrency.smc.contract.ContractStatus;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.SmartMethod;
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
    public List<ResultValue> batchProcess() {
        try {

            log.debug("Smart method={}", smartMethods);
            validateStatus(ContractStatus.ACTIVE);
            //call the method and charge the fuel
            return machine.callViewMethod(getSmartContract(), smartMethods);

        } catch (Exception e) {
            var msg = putExceptionToLog(getExecutionLog(), e);
            log.error(msg, e);
            return List.of();
        }
    }

    @Override
    public ResultValue process() {
        return ResultValue.UNDEFINED_RESULT;
    }
}
