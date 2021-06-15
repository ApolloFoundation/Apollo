/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.smc.contract.ContractStatus;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.polyglot.engine.ExecutionEnv;

/**
 * @author andrew.zinchenko@gmail.com
 */

public interface SmcContractTxProcessor {
    /**
     * Validate transaction, perform smart contract and manipulate balances
     *
     * @return detailed execution log
     */
    ExecutionLog process();

    /**
     * Returns the smart contract that is processing
     *
     * @return smart contract
     */
    SmartContract getSmartContract();

    /**
     * Returns the environment to execute the given contract
     *
     * @return the environment to execute the given contract
     */
    ExecutionEnv getExecutionEnv();

    default void validateStatus(ContractStatus expected) {
        if (expected != getSmartContract().getStatus()) {
            throw new IllegalStateException("Expected " + expected.name() +
                " actually " + getSmartContract().getStatus().name());
        }
    }

}
