/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.internal;

import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.smc.blockchain.BlockchainIntegrator;
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
public class CallViewMethodTxProcessor extends AbstractSmcContractTxProcessor {
    private final List<SmartMethod> smartMethods;

    public CallViewMethodTxProcessor(SmartContract smartContract, List<SmartMethod> smartMethods, BlockchainIntegrator processor, SmcConfig smcConfig) {
        super(smcConfig, processor, smartContract);
        this.smartMethods = smartMethods;
    }

    @Override
    public List<ResultValue> batchExecuteContract(ExecutionLog executionLog) {
        log.debug("Smart method={}", smartMethods);
        validateStatus(ContractStatus.ACTIVE);
        //call the method and charge the fuel
        var result = smcMachine.callMethod(getSmartContract(), smartMethods);
        executionLog.join(smcMachine.getExecutionLog());
        smcMachine.resetExecutionLog();
        return result;
    }

}
