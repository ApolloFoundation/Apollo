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
    void saveContract(SmartContract contract);

    SmartContract loadContract(Address address);

    void updateContractState(SmartContract contract);

    String loadSerializedContract(Address address);

    boolean saveSerializedContract(Address address, String serializedObject);

    SmartContract createNewContract(Transaction smcTransaction);

}
