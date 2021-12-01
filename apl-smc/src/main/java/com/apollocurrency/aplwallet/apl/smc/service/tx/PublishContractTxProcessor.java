/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.service.tx;

import com.apollocurrency.aplwallet.apl.smc.SmcContext;
import com.apollocurrency.smc.contract.ContractStatus;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.fuel.OutOfFuelException;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.ResultValue;
import com.apollocurrency.smc.polyglot.PolyglotException;
import lombok.extern.slf4j.Slf4j;

/**
 * Validate transaction, perform smart contract and manipulate balances
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class PublishContractTxProcessor extends AbstractSmcContractTxProcessor {

    public PublishContractTxProcessor(SmartContract smartContract, SmcContext context) {
        super(smartContract, context);
    }

    @Override
    protected ResultValue executeContract(ExecutionLog executionLog) throws OutOfFuelException, PolyglotException {
        validateStatus(ContractStatus.CREATED);
        getSmartContract().setStatus(ContractStatus.PUBLISHED);
        //call smart contract constructor, charge the fuel
        smcMachine.publishPayableContract(getSmartContract());
        executionLog.join(smcMachine.getExecutionLog());
        validateStatus(ContractStatus.ACTIVE);
        smcMachine.resetExecutionLog();
        return ResultValue.from(getSmartContract());
    }

}
