/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.storage;

import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractStorageService;
import com.apollocurrency.smc.blockchain.storage.AbstractMapping;
import com.apollocurrency.smc.blockchain.storage.JsonConverter;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.data.type.Key;
import lombok.SneakyThrows;

import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class SmcMappingRepository<V> extends AbstractMapping<V> {

    private final SmcContractStorageService smcContractStorageService;
    private final JsonConverter<V> jsonConverter;

    protected SmcMappingRepository(SmcContractStorageService smcContractStorageService, Address contractAddress, String mappingName, JsonConverter<V> jsonConverter) {
        super(contractAddress, mappingName);
        this.smcContractStorageService = Objects.requireNonNull(smcContractStorageService);
        this.jsonConverter = Objects.requireNonNull(jsonConverter);
    }

    @SneakyThrows
    @Override
    protected V getOneKey(Key key) {
        String entry = smcContractStorageService.loadEntry(getContract(), key);
        if (entry != null) {
            return jsonConverter.fromJson(entry);
        }
        return null;
    }

    @Override
    protected V putOne(Key key, V value) {
        String entry = smcContractStorageService.loadEntry(getContract(), key);
        V old;
        if (entry != null) {
            old = jsonConverter.fromJson(entry);
        } else {
            old = null;
        }
        String json = jsonConverter.toJson(value);
        smcContractStorageService.saveEntry(getContract(), key, getName(), json);
        return old;
    }

    @Override
    protected V deleteOne(Key key) {
        String entry = smcContractStorageService.loadEntry(getContract(), key);
        V old;
        if (entry != null) {
            old = jsonConverter.fromJson(entry);
            smcContractStorageService.deleteEntry(getContract(), key);
        } else {
            old = null;
        }
        return old;
    }
}
