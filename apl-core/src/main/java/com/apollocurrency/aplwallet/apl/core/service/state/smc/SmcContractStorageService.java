/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.data.type.Key;


/**
 * @author andrew.zinchenko@gmail.com
 */
public interface SmcContractStorageService {

    /**
     * Save the serialized object in persistent mapping storage
     *
     * @param address    the contract address
     * @param key        the mapping key
     * @param name       the mapping name
     * @param jsonObject the serialized object
     */
    void saveEntry(Address address, Key key, String name, String jsonObject);

    /**
     * Load the serialized object by the given address or null if the given key doesn't match the value
     *
     * @param address the contract address
     * @param key     the mapping key
     * @return the serialized object
     */
    String loadEntry(Address address, Key key);

    /**
     * Delete the serialized object by the given address from storage
     *
     * @param address the contract address
     * @param key     the mapping key
     */
    void deleteEntry(Address address, Key key);

    /**
     * Checks if mapping exists
     *
     * @param address given contract address
     * @param name    given mapping name
     * @return true if specified mapping exists
     */
    boolean isMappingExist(Address address, String name);

}
