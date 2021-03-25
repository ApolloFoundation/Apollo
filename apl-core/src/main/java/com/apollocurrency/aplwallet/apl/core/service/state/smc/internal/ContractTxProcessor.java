/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.internal;

import com.apollocurrency.smc.contract.ContractState;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.vm.ExecutionLog;

/**
 * @author andrew.zinchenko@gmail.com
 */

public interface ContractTxProcessor {
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
    SmartContract smartContract();

    default void validateState(ContractState expected, SmartContract smartContract) {
        if (expected != smartContract.getState()) {
            throw new IllegalStateException("Expected " + expected.name() +
                " actually " + smartContract.getState().name());
        }
    }

}
