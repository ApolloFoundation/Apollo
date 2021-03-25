package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractTxProcessor;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.smc.blockchain.crypt.CryptoLibProvider;
import com.apollocurrency.smc.contract.ContractState;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.SMCMachine;
import com.apollocurrency.smc.contract.vm.SMCMachineFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Validate transaction, perform smart contract and manipulate balances
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public abstract class AbstractContractTxProcessor implements ContractTxProcessor {
    @Getter
    protected final ContractService contractService;
    protected final CryptoLibProvider cryptoLibProvider;
    private final SMCMachineFactory machineFactory;

    protected AbstractContractTxProcessor(ContractService contractService, CryptoLibProvider cryptoLibProvider, SMCMachineFactory machineFactory) {
        this.contractService = contractService;
        this.cryptoLibProvider = cryptoLibProvider;
        this.machineFactory = machineFactory;
    }

    public abstract ExecutionLog doProcess(SMCMachine smcMachine, Transaction smcTransaction);

    @Override
    public ExecutionLog process(Transaction smcTransaction) {
        TransactionTypes.TransactionTypeSpec spec = smcTransaction.getAttachment().getTransactionTypeSpec();
        if (spec != TransactionTypes.TransactionTypeSpec.SMC_PUBLISH
            && spec != TransactionTypes.TransactionTypeSpec.SMC_CALL_METHOD) {
            throw new IllegalStateException("Invalid transaction attachment: " + smcTransaction.getAttachment().getTransactionTypeSpec());
        }
        SMCMachine smcMachine = machineFactory.createNewInstance();

        ExecutionLog executionLog = doProcess(smcMachine, smcTransaction);

        smcMachine.shutdown();

        return executionLog;
    }

    public void validateState(ContractState expected, SmartContract smartContract) {
        if (!expected.equals(smartContract.getState())) {
            throw new IllegalStateException("Expected " + expected.name() +
                " actually " + smartContract.getState().name());
        }
    }

}
