/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.storage;

import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractStorageService;
import com.apollocurrency.smc.blockchain.storage.ContractMappingRepository;
import com.apollocurrency.smc.blockchain.storage.JsonConverter;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.data.type.Key;

import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class PersistentMappingRepository<V> extends ContractMappingRepository<V> {

    private final SmcContractStorageService smcContractStorageService;

    protected PersistentMappingRepository(SmcContractStorageService smcContractStorageService,
                                          Address contractAddress,
                                          String mappingName,
                                          JsonConverter<V> jsonConverter) {
        super(contractAddress, mappingName, jsonConverter);
        this.smcContractStorageService = Objects.requireNonNull(smcContractStorageService);
    }

    @Override
    public String getOneObject(Key key) {
        return smcContractStorageService.loadEntry(getContract(), key);
    }

    @Override
    public void putOneObject(Key key, String value) {
        smcContractStorageService.saveEntry(getContract(), key, getName(), value);
    }

    @Override
    public void deleteOneObject(Key key) {
        smcContractStorageService.deleteEntry(getContract(), key);
    }
}
