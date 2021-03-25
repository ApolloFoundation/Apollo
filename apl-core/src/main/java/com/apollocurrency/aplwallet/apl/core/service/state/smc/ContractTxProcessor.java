package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
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
    ExecutionLog process(Transaction smcTransaction);

}
