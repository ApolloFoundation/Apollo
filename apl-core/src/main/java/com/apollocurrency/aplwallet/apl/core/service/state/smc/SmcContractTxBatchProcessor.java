/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.ResultValue;

import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */

public interface SmcContractTxBatchProcessor extends SmcContractTxProcessor {

    /**
     * Batch processing. Validate, perform the smart contract and manipulate balances
     * Call the function/method of the smart contract.
     *
     * @param executionLog execution log
     * @return list of the contract executions
     */
    List<ResultValue> batchProcess(ExecutionLog executionLog);

}
