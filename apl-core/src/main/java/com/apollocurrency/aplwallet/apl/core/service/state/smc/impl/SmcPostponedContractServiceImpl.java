/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.apl.core.service.state.smc.PostponedContractService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractService;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.fuel.Fuel;
import com.apollocurrency.smc.data.type.Address;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class SmcPostponedContractServiceImpl implements PostponedContractService {
    private final SmcContractService contractService;

    private final Map<Address, SmartContract> cachedContracts;

    @Inject
    public SmcPostponedContractServiceImpl(SmcContractService contractService) {
        this.contractService = contractService;
        this.cachedContracts = new HashMap<>();
    }

    @Override
    public void saveContract(SmartContract contract) {
        log.trace("Save in memory collection, contract={}", contract.getAddress().getHex());
        cachedContracts.put(contract.getAddress(), contract);
    }

    @Override
    public SmartContract loadContract(Address address, Address originator, Address caller, Fuel contractFuel) {
        return cachedContracts.computeIfAbsent(address, contract -> contractService.loadContract(address, originator, caller, contractFuel));
    }

    @Override
    public boolean isContractExist(Address address) {
        var rc = cachedContracts.containsKey(address);
        if (!rc) {
            rc = contractService.isContractExist(address);
        }
        return rc;
    }

    @Override
    public void updateContractState(SmartContract contract) {
        log.trace("Update in memory collection, contract={}", contract.getAddress().getHex());
        cachedContracts.put(contract.getAddress(), contract);
    }

    public void commit() {
        log.trace("For committing {}", cachedContracts.size());
        cachedContracts.forEach((address, smartContract) -> {
            if (contractService.isContractExist(address)) {
                log.trace("Update sate on address={}, state={}", address.getHex(), smartContract.getSerializedObject());
                contractService.updateContractState(smartContract);
            } else {
                log.trace("Save new contract={}", address.getHex());
                contractService.saveContract(smartContract);
            }
        });
    }
}
