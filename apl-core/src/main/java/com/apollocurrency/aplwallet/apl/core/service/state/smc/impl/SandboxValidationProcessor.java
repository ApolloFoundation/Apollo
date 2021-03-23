package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractService;
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
public class SandboxValidationProcessor extends AbstractContractCmdProcessor {

    public SandboxValidationProcessor(ContractService contractService) {
        super(contractService);
    }

    @Override
    public ExecutionLog process(SMCMachine smcMachine, Transaction smcTransaction) {
        boolean isValid;
        SmartContract smartContract = contractService.createNewContract(smcTransaction);
        validateState(ContractState.CREATED, smartContract);

        isValid = smcMachine.validate(smartContract);
        ExecutionLog executionLog = new ExecutionLog(smcMachine.getExecutionLog());
        if (!isValid) {
            //TODO: Update the Error code
            executionLog.setErrorCode(1L);
        }

        smcMachine.shutdown();

        return executionLog;
    }
}
