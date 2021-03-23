package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractCmdProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.smc.contract.ContractState;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.SMCMachine;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * Validate transaction, perform smart contract and manipulate balances
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public abstract class AbstractContractCmdProcessor implements ContractCmdProcessor {
    @Getter
    protected final ContractService contractService;

    public AbstractContractCmdProcessor(ContractService contractService) {
        this.contractService = Objects.requireNonNull(contractService);
    }

    @Override
    public ExecutionLog process(SMCMachine smcMachine, Transaction smcTransaction) {
        TransactionTypes.TransactionTypeSpec spec = smcTransaction.getAttachment().getTransactionTypeSpec();
        if (spec != TransactionTypes.TransactionTypeSpec.SMC_PUBLISH
            && spec != TransactionTypes.TransactionTypeSpec.SMC_CALL_METHOD) {
            throw new IllegalStateException("Invalid transaction attachment: " + smcTransaction.getAttachment().getTransactionTypeSpec());
        }
        return ExecutionLog.EMPTY_LOG;
    }

    public void validateState(ContractState expected, SmartContract smartContract) {
        if (!expected.equals(smartContract.getState())) {
            throw new IllegalStateException("Expected " + expected.name() +
                " actually " + smartContract.getState().name());
        }
    }

}
