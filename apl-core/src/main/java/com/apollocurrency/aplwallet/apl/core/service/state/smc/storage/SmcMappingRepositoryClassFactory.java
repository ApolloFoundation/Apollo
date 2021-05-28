/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.storage;

import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractStorageService;
import com.apollocurrency.smc.blockchain.storage.AddressJsonConverter;
import com.apollocurrency.smc.blockchain.storage.BigIntegerJsonConverter;
import com.apollocurrency.smc.blockchain.storage.ContractMappingRepository;
import com.apollocurrency.smc.blockchain.storage.ContractMappingRepositoryFactory;
import com.apollocurrency.smc.blockchain.storage.StringJsonConverter;
import com.apollocurrency.smc.data.type.Address;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class SmcMappingRepositoryClassFactory {
    private final SmcContractStorageService smcContractStorageService;

    @Inject
    public SmcMappingRepositoryClassFactory(SmcContractStorageService smcContractStorageService) {
        this.smcContractStorageService = smcContractStorageService;
    }

    public ContractMappingRepositoryFactory createMappingFactory(final Address contract) {
        return new ContractMappingRepositoryFactory() {
            @Override
            public boolean isMappingExist(String mappingName) {
                return smcContractStorageService.isMappingExist(contract, mappingName);
            }

            @Override
            public ContractMappingRepository<Address> addressRepository(String mappingName) {
                return new PersistentMappingRepository<>(smcContractStorageService, contract, mappingName, new AddressJsonConverter());
            }

            @Override
            public ContractMappingRepository<BigInteger> bigIntegerRepository(String mappingName) {
                return new PersistentMappingRepository<>(smcContractStorageService, contract, mappingName, new BigIntegerJsonConverter());
            }

            @Override
            public ContractMappingRepository<String> stringRepository(String mappingName) {
                return new PersistentMappingRepository<>(smcContractStorageService, contract, mappingName, new StringJsonConverter());
            }
        };
    }
}
