package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.SMCMachine;

/**
 * @author andrew.zinchenko@gmail.com
 */

public interface ContractCmdProcessor {
    /**
     * Validate transaction, perform smart contract and manipulate balances
     *
     * @return detailed execution log
     */
    ExecutionLog process(SMCMachine smcMachine, Transaction smcTransaction);

}
