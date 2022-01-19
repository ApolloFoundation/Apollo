/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.aplwallet.api.v2.model.ContractDetails;
import com.apollocurrency.aplwallet.apl.smc.model.AplContractSpec;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.fuel.Fuel;
import com.apollocurrency.smc.data.type.Address;

import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface SmcContractRepository {

    /**
     * Save the published contract
     *
     * @param contract        the published contract
     * @param transactionId   the transaction identifier, that contains given contract
     * @param transactionHash the transaction hash, that contains given contract
     */
    void saveContract(SmartContract contract, long transactionId, byte[] transactionHash);

    /**
     * Load the saved contract by the given address or null if the given address doesn't correspond the smart contract
     *
     * @param address      given contract address
     * @param originator   the origin transaction sender
     * @param caller       the contract caller
     * @param contractFuel given fuel to execute method calling
     * @return loaded smart contract or throw {@link com.apollocurrency.smc.contract.AddressNotFoundException}
     */
    SmartContract loadContract(Address address, Address originator, Address caller, Fuel contractFuel);

    SmartContract loadContract(Address address, Address originator, Fuel contractFuel);

    SmartContract loadContract(Address address);

    /**
     * Load the contract specification by given contract address
     *
     * @param address the contract address
     * @return the contract specification by given contract address
     */
    AplContractSpec loadAsrModuleSpec(Address address);

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
     * Returns the list of contracts by filter
     *
     * @param query given filter
     * @return the list of contracts
     */
    List<ContractDetails> loadContractsByFilter(ContractQuery query);

    /**
     * Returns the details information about contract given address
     *
     * @param address the given contract address
     * @return the details information about contract
     */
    List<ContractDetails> getContractDetailsByAddress(long address);

}
