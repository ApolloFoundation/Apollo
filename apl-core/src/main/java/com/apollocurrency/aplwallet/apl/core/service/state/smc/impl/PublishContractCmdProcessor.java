package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcPublishContractAttachment;
import com.apollocurrency.smc.contract.ContractState;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.SMCMachine;
import lombok.extern.slf4j.Slf4j;

/**
 * Validate transaction, perform smart contract and manipulate balances
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class PublishContractCmdProcessor extends AbstractContractCmdProcessor {

    public PublishContractCmdProcessor(ContractService contractService) {
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

        SmartContract smartContract = contractService.createNewContract(smcTransaction);
        validateState(ContractState.CREATED, smartContract);


        validateState(ContractState.PUBLISHED, smartContract);

        //call smart contract constructor, charge the fuel
        smcMachine.callConstructor(smartContract);
        executionLog.join(smcMachine.getExecutionLog());
        validateState(ContractState.ACTIVE, smartContract);

        smcMachine.resetExecutionLog();

        contractService.saveContract(smartContract);
        return executionLog;
    }

}
