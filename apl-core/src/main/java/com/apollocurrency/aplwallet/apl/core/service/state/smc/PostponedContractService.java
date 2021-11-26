/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.fuel.Fuel;
import com.apollocurrency.smc.data.type.Address;

/**
 * The specified service to postpone the real modification of the data.
 * All changes will be committed later.
 *
 * @author andrew.zinchenko@gmail.com
 */
public interface PostponedContractService {
    /**
     * Save the published contract,
     * the changes will be committed later.
     *
     * @param contract the published contract
     */
    void saveContract(SmartContract contract);

    /**
     * Load the saved contract by the given address or null if the given address doesn't correspond the smart contract
     *
     * @param address      given contract address
     * @param originator   the origin transaction sender
     * @param caller       the contract caller
     * @param contractFuel given fuel to execute method calling
     * @return loaded smart contract or null
     */
    SmartContract loadContract(Address address, Address originator, Address caller, Fuel contractFuel);

    /**
     * Checks if contract already exists
     *
     * @param address given contract address
     * @return true if contract exists at given address
     */
    boolean isContractExist(Address address);

    /**
     * Update the contract state,
     * the changes will be committed later.
     *
     * @param contract the already published contract
     */
    void updateContractState(SmartContract contract);

    /**
     * Fix all contract changes in the blockchain.
     * Saves new contract and updates state for existing contracts.
     */
    void commitContractChanges(Transaction transaction);

}
