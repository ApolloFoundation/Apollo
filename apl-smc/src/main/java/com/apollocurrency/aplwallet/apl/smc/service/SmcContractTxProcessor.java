/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.service;

import com.apollocurrency.smc.contract.ContractStatus;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.fuel.OutOfFuelException;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.ResultValue;
import com.apollocurrency.smc.polyglot.engine.ExecutionEnv;

/**
 * @author andrew.zinchenko@gmail.com
 */

public interface SmcContractTxProcessor {
    /**
     * Validate, perform the smart contract and manipulate balances
     * Call the function/method of the smart contract.
     *
     * @param executionLog execution log
     * @return result of the contract execution
     * @throws OutOfFuelException not enough fuel to execute contract
     */
    ResultValue process(ExecutionLog executionLog) throws OutOfFuelException;

    /**
     * Commit all changes.
     */
    void commit();

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
