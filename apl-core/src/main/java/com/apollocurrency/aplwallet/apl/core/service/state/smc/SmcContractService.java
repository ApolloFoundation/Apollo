/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.aplwallet.api.v2.model.ContractDetails;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.fuel.Fuel;
import com.apollocurrency.smc.data.type.Address;

import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface SmcContractService {

    /**
     * Save the published contract
     *
     * @param contract the published contract
     */
    void saveContract(SmartContract contract);

    /**
     * Load the saved contract by the given address or null if the given address doesn't correspond the smart contract
     *
     * @param address      given contract address
     * @param contractFuel given fuel to execute method calling
     * @return loaded smart contract or null
     */
    SmartContract loadContract(Address address, Fuel contractFuel);

    /**
     * Checks if contract already exists
     *
     * @param address given contract address
     * @return true if contract exists at given address
     */
    boolean isContractExist(Address address);

    void updateContractState(SmartContract contract);

    String loadSerializedContract(Address address);

    void saveSerializedContract(SmartContract contract, String serializedObject);

    /**
     * Creates a new smart contract instance. That one is not persisted in the blockchain.
     *
     * @param smcTransaction blockchain transaction instance
     * @return smart contract instance
     */
    SmartContract createNewContract(Transaction smcTransaction);

    /**
     * Returns the list of contracts published by given owner
     *
     * @param owner given owner
     * @return the list of contracts
     */
    List<ContractDetails> loadContractsByOwner(Address owner, int from, int limit);

    /**
     * Returns the details information about contract given transaction id
     *
     * @param txAddress the given transaction id
     * @return the details information about contract
     */
    ContractDetails getContractDetailsByTransaction(Address txAddress);

    /**
     * Returns the details information about contract given address
     *
     * @param address the given contract address
     * @return the details information about contract
     */
    ContractDetails getContractDetailsByAddress(Address address);
}
