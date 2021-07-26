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
import com.apollocurrency.smc.persistence.txlog.TxLog;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Validate transaction, perform smart contract and manipulate balances
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class CallMethodTxProcessor extends AbstractSmcContractTxProcessor {
    private final SmartMethod smartMethod;

    public CallMethodTxProcessor(SmartContract smartContract, SmartMethod smartMethod, BlockchainIntegrator processor, SmcConfig smcConfig) {
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

    @Override
    public boolean commit(@NonNull TxLog txLog) {
        boolean rc = false;
        var iterator = txLog.read(0);
        long id = 0;
        while (iterator.next()) {
            var h = iterator.getHeader();
            if (h.getId() <= id) {
                log.error("Not incremental sequence of the record id; id={} next_id={}", id, h.getId());
                throw new IllegalStateException("Not incremental sequence of the record id.");
            }
            var r = iterator.getRecord();
            switch (r.type()) {
                case TRANSFER:
                case REMOTE_CALL:
                case WRITE_MAPPING:
                    break;
                case FIRE_EVENT:
                case DELEGATE_METHOD:
                    throw new IllegalStateException("Not implemented yet.");
            }

        }
        return rc;
    }
}
