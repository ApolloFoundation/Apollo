/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.smc.contract.ContractStatus;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.persistence.txlog.TxLog;
import com.apollocurrency.smc.polyglot.engine.ExecutionEnv;

import java.util.Optional;

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
     */
    Optional<Object> process(ExecutionLog executionLog);

    /**
     * Commit all changes.
     *
     * @param txLog the log of operations that should be committed.
     * @return result of commit operation, true if no error occurred during committing.
     */
    boolean commit(TxLog txLog);

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
