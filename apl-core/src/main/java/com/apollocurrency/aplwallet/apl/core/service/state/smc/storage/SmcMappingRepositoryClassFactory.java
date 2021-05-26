/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.storage;

import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractStorageService;
import com.apollocurrency.smc.blockchain.storage.AddressJsonConverter;
import com.apollocurrency.smc.blockchain.storage.BigIntegerJsonConverter;
import com.apollocurrency.smc.blockchain.storage.StringJsonConverter;
import com.apollocurrency.smc.contract.vm.ContractMappingFactory;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.data.type.AddressMapping;
import com.apollocurrency.smc.data.type.BigIntegerMapping;
import com.apollocurrency.smc.data.type.Key;
import com.apollocurrency.smc.data.type.StringMapping;
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

    public ContractMappingFactory createMappingFactory(final Address contract) {
        return new ContractMappingFactory() {

            @Override
            public Address contract() {
                return contract;
            }

            @Override
            public boolean isMappingExist(String mappingName) {
                return smcContractStorageService.isMappingExist(contract, mappingName);
            }

            @Override
            public AddressMapping address(String mappingName) {
                return new AddressMappingRepository(smcContractStorageService, contract, mappingName);
            }

            @Override
            public BigIntegerMapping bigNumber(String mappingName) {
                return new BigIntegerMappingRepository(smcContractStorageService, contract, mappingName);
            }

            @Override
            public StringMapping string(String mappingName) {
                return new StringMappingRepository(smcContractStorageService, contract, mappingName);
            }
        };
    }

    static class AddressMappingRepository extends SmcMappingRepository<Address> implements AddressMapping {

        protected AddressMappingRepository(SmcContractStorageService smcContractStorageService, Address contractAddress, String mappingName) {
            super(smcContractStorageService, contractAddress, mappingName, new AddressJsonConverter());
        }

    }

    static class BigIntegerMappingRepository extends SmcMappingRepository<BigInteger> implements BigIntegerMapping {

        protected BigIntegerMappingRepository(SmcContractStorageService smcContractStorageService, Address contractAddress, String mappingName) {
            super(smcContractStorageService, contractAddress, mappingName, new BigIntegerJsonConverter());
        }

        @Override
        public BigInteger getOne(Key key) {
            var v = super.getOne(key);
            if (v == null) {
                return BigInteger.ZERO;
            }
            return v;
        }
    }

    static class StringMappingRepository extends SmcMappingRepository<String> implements StringMapping {

        protected StringMappingRepository(SmcContractStorageService smcContractStorageService, Address contractAddress, String mappingName) {
            super(smcContractStorageService, contractAddress, mappingName, new StringJsonConverter());
        }

    }

}
