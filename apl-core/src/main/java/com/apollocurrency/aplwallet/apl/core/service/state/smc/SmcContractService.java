/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.aplwallet.api.v2.model.ContractDetails;
import com.apollocurrency.aplwallet.apl.smc.model.AplContractSpec;
import com.apollocurrency.smc.contract.ContractStatus;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.fuel.Fuel;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.polyglot.Version;

import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface SmcContractService {

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
     * Returns the list of contracts published by given owner
     *
     * @param owner given owner
     * @return the list of contracts
     */
    List<ContractDetails> loadContractsByOwner(Address owner, int from, int limit);

    /**
     * Returns the list of contracts by filter
     *
     * @param address given address
     * @param owner   given owner
     * @param name    given contract name
     * @param height  the blockchain height
     * @param from    the first index
     * @param to      the last index
     * @return the list of contracts
     */
    List<ContractDetails> loadContractsByFilter(Address address, Address owner, String name, ContractStatus status, int height, int from, int to);

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

    /**
     * Returns the list of ASR module names.
     *
     * @param language the language name
     * @param version  the language version
     * @param type     the module type: token, escrow etc.
     * @return the list of ASR module names.
     */
    List<String> getAsrModules(String language, Version version, String type);

    /**
     * Returns the list of inherited ASR modules.
     *
     * @param asrModuleName the ASR module name
     * @param language      the language name
     * @param version       the language version
     * @return the list of ASR module names.
     */
    List<String> getInheritedAsrModules(String asrModuleName, String language, Version version);

    /**
     * Load the contract specification of the specified ASR module
     *
     * @param asrModuleName the ASR module name
     * @param language      the language name
     * @param version       the language version
     * @return the contract specification of the specified ASR module
     */
    AplContractSpec loadAsrModuleSpec(String asrModuleName, String language, Version version);

}
