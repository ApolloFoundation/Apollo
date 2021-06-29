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
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Validate transaction, perform smart contract and manipulate balances
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class CallPayableMethodTxProcessor extends AbstractSmcContractTxProcessor {
    private final SmartMethod smartMethod;

    public CallPayableMethodTxProcessor(SmartContract smartContract, SmartMethod smartMethod, BlockchainIntegrator processor, SmcConfig smcConfig) {
        super(smcConfig, processor, smartContract);
        this.smartMethod = smartMethod;
    }

    @Override
    public Optional<Object> executeContract(ExecutionLog executionLog) {
        log.debug("Smart method={}", smartMethod);
        validateStatus(ContractStatus.ACTIVE);
        //call the method and charge the fuel
        var result = smcMachine.evalPayableMethod(getSmartContract(), smartMethod);
        executionLog.join(smcMachine.getExecutionLog());
        smcMachine.resetExecutionLog();
        return result;
    }

}
