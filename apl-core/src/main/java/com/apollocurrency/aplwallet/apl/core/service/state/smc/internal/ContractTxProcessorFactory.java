/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.internal;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcCallMethodAttachment;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.SmartMethod;
import com.apollocurrency.smc.contract.vm.SMCMachine;
import com.apollocurrency.smc.contract.vm.SMCMachineFactory;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Validate transaction, perform smart contract and manipulate balances
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class ContractTxProcessorFactory {

    private final ContractService contractService;
    private final SMCMachineFactory machineFactory;

    @Inject
    public ContractTxProcessorFactory(ContractService contractService, SMCMachineFactory machineFactory) {
        this.contractService = contractService;
        this.machineFactory = machineFactory;
    }

    public ContractTxProcessor createPublishContractProcessor(Transaction smcTransaction) {
        checkPrecondition(smcTransaction);
        SmartContract smartContract = contractService.createNewContract(smcTransaction);
        SMCMachine smcMachine = machineFactory.createNewInstance();
        return new PublishContractTxProcessor(smcMachine, smartContract);
    }

    public ContractTxProcessor createCallMethodProcessor(Transaction smcTransaction) {
        checkPrecondition(smcTransaction);
        SmartContract smartContract = contractService.createNewContract(smcTransaction);
        SmcCallMethodAttachment attachment = (SmcCallMethodAttachment) smcTransaction.getAttachment();
        SmartMethod smartMethod = SmartMethod.builder()
            .name(attachment.getMethodName())
            .args(String.join(",", attachment.getMethodParams()))
            .value(smcTransaction.getAmount())
            .build();
        SMCMachine smcMachine = machineFactory.createNewInstance();

        return new CallMethodContractTxProcessor(smcMachine, smartContract, smartMethod);
    }

    public ContractTxProcessor createContractValidationProcessor(Transaction smcTransaction) {
        checkPrecondition(smcTransaction);
        SmartContract smartContract = contractService.createNewContract(smcTransaction);
        SMCMachine smcMachine = machineFactory.createNewInstance();

        return new SandboxValidationProcessor(smcMachine, smartContract);
    }

    private void checkPrecondition(Transaction smcTransaction) {
        TransactionTypes.TransactionTypeSpec spec = smcTransaction.getAttachment().getTransactionTypeSpec();
        if (spec != TransactionTypes.TransactionTypeSpec.SMC_PUBLISH
            && spec != TransactionTypes.TransactionTypeSpec.SMC_CALL_METHOD) {
            log.error("Invalid transaction attachment, txType={} txId={}", smcTransaction.getType().getSpec(), smcTransaction.getChainId());
            throw new IllegalStateException("Invalid transaction attachment: " + smcTransaction.getAttachment().getTransactionTypeSpec());
        }
    }

}
