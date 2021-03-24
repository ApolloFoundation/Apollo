/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.data.type.Address;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface ContractService {

    /**
     * Save the published contract
     *
     * @param contract the published contract
     */
    void saveContract(SmartContract contract);

    /**
     * Load the saved contract by the given address or null if the given address doesn't correspond the smart contract
     *
     * @param address given contract address
     * @return loaded smart contract or null
     */
    SmartContract loadContract(Address address);

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

}
