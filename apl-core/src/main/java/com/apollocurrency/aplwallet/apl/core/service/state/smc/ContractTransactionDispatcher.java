package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.smc.contract.vm.ExecutionLog;

/**
 * @author andrew.zinchenko@gmail.com
 */

public interface ContractTransactionDispatcher {

    ContractCmdProcessor registerProcessor(TransactionTypes.TransactionTypeSpec transactionType, ContractCmdProcessor processor);

    /**
     * Validate transaction, perform smart contract and manipulate balances
     *
     * @return the execution log
     */
    ExecutionLog dispatch(Transaction smcTransaction);

}
