/*
 * Copyright (c) 2021-2022. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.service.tx;

import com.apollocurrency.aplwallet.apl.smc.SmcContext;
import com.apollocurrency.smc.contract.ContractStatus;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.fuel.OutOfFuelException;
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
    public ResultValue process() throws OutOfFuelException, PolyglotException {
        validateStatus(ContractStatus.CREATED);
        getSmartContract().setStatus(ContractStatus.PUBLISHED);
        //call smart contract constructor, charge the fuel
        machine.publishPayableContract(getSmartContract());
        validateStatus(ContractStatus.ACTIVE);
        return ResultValue.from(getSmartContract()).build();
    }

}
