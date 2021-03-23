package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.smc.blockchain.tx.SMCTransaction;
import com.apollocurrency.smc.blockchain.tx.SMCTransactionType;
import com.apollocurrency.smc.contract.vm.ExecutionLog;

/**
 * @author andrew.zinchenko@gmail.com
 */

public interface ContractTransactionDispatcher {

    ContractCmdProcessor registerProcessor(SMCTransactionType transactionType, ContractCmdProcessor processor);

    /**
     * Validate transaction, perform smart contract and manipulate balances
     *
     * @return the execution log
     */
    ExecutionLog dispatch(SMCTransaction smcTransaction);

}
