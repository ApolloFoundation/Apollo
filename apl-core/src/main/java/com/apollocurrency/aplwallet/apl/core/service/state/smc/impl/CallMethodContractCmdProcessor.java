package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.model.smc.AplAddress;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcPublishContractAttachment;
import com.apollocurrency.smc.contract.ContractState;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.SmartMethod;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.SMCMachine;
import lombok.extern.slf4j.Slf4j;

/**
 * Validate transaction, perform smart contract and manipulate balances
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class CallMethodContractCmdProcessor extends AbstractContractCmdProcessor {

    public CallMethodContractCmdProcessor(ContractService contractService) {
        super(contractService);
    }

    @Override
    public ExecutionLog process(SMCMachine smcMachine, Transaction smcTransaction) {
        log.debug("process: txType={} tx={}", smcTransaction.getType().getSpec(), smcTransaction);
        ExecutionLog executionLog = new ExecutionLog();
        executionLog.join(
            super.process(smcMachine, smcTransaction)
        );
        SmcPublishContractAttachment attachment = (SmcPublishContractAttachment) smcTransaction.getAttachment();

        SmartContract smartContract = contractService.loadContract(new AplAddress(smcTransaction.getRecipientId()));
        validateState(ContractState.ACTIVE, smartContract);

        validateState(ContractState.ACTIVE, smartContract);

        SmartMethod smartMethod = SmartMethod.builder()
/*
            .name(smcTransaction.getMethodName())
            .args(smcTransaction.getArgs())
            .value(smcTransaction.getValue())
*/
            .build();

        //call the method and charge the fuel
        smcMachine.callMethod(smartContract, smartMethod);
        executionLog.join(smcMachine.getExecutionLog());

        smcMachine.resetExecutionLog();

        contractService.updateContractState(smartContract);
        return executionLog;
    }

}
