package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.model.smc.AplAddress;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcCallMethodAttachment;
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
public class CallMethodContractTxProcessor extends AbstractContractTxProcessor {

    public CallMethodContractTxProcessor(ContractService contractService) {
        super(contractService);
    }

    @Override
    public ExecutionLog doProcess(SMCMachine smcMachine, Transaction smcTransaction) {
        log.debug("process: txType={} tx={}", smcTransaction.getType().getSpec(), smcTransaction);
        ExecutionLog executionLog = new ExecutionLog();
        SmcCallMethodAttachment attachment = (SmcCallMethodAttachment) smcTransaction.getAttachment();

        SmartContract smartContract = contractService.loadContract(new AplAddress(smcTransaction.getRecipientId()));
        validateState(ContractState.ACTIVE, smartContract);

        SmartMethod smartMethod = SmartMethod.builder()
            .name(attachment.getMethodName())
            .args(String.join(",", attachment.getMethodParams()))
            .value(smcTransaction.getAmount())
            .build();

        //call the method and charge the fuel
        smcMachine.callMethod(smartContract, smartMethod);
        executionLog.join(smcMachine.getExecutionLog());

        smcMachine.resetExecutionLog();

        contractService.updateContractState(smartContract);

        return executionLog;
    }

}
