/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.PostponedContractService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractRepository;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.fuel.Fuel;
import com.apollocurrency.smc.data.type.Address;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class SmcPostponedContractServiceImpl implements PostponedContractService {
    private final SmcContractRepository contractRepository;
    private final Map<Address, SmartContract> cachedContracts;

    public SmcPostponedContractServiceImpl(SmcContractRepository contractRepository) {
        this.contractRepository = contractRepository;
        this.cachedContracts = new HashMap<>();
    }

    @Override
    public void saveContract(SmartContract contract) {
        log.trace("Save in memory collection, contract={}", contract.getAddress().getHex());
        cachedContracts.put(contract.getAddress(), contract);
    }

    @Override
    public SmartContract loadContract(Address address, Address originator, Address caller, Fuel contractFuel) {
        return cachedContracts.computeIfAbsent(address, contract -> contractRepository.loadContract(address, originator, caller, contractFuel));
    }

    @Override
    public boolean isContractExist(Address address) {
        var rc = cachedContracts.containsKey(address);
        if (!rc) {
            rc = contractRepository.isContractExist(address);
        }
        return rc;
    }

    @Override
    public void updateContractState(SmartContract contract) {
        log.trace("Update in memory collection, contract={}", contract.getAddress().getHex());
        cachedContracts.put(contract.getAddress(), contract);
    }

    @Override
    public void commitContractChanges(Transaction transaction) {
        log.trace("For committing {}", cachedContracts.size());
        try {
            cachedContracts.forEach((address, smartContract) -> {
                if (contractRepository.isContractExist(address)) {
                    log.trace("Update sate on address={}, state={}", address.getHex(), smartContract.getSerializedObject());
                    contractRepository.updateContractState(smartContract);
                } else {
                    log.trace("Save new contract={}", address.getHex());
                    contractRepository.saveContract(smartContract, transaction.getId(), transaction.getFullHash());
                }
            });
        } finally {
            cachedContracts.clear();
        }
    }
}
