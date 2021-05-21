/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.storage;

import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractStorageService;
import com.apollocurrency.smc.blockchain.storage.AddressJsonConverter;
import com.apollocurrency.smc.blockchain.storage.BigIntegerJsonConverter;
import com.apollocurrency.smc.blockchain.storage.MappingFactory;
import com.apollocurrency.smc.blockchain.storage.StringJsonConverter;
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
public class SmcDbMappingClassFactory {
    private final SmcContractStorageService smcContractStorageService;

    @Inject
    public SmcDbMappingClassFactory(SmcContractStorageService smcContractStorageService) {
        this.smcContractStorageService = smcContractStorageService;
    }

    MappingFactory createMappingFactory(final Address contract) {
        return new MappingFactory() {

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
        protected BigInteger getOneKey(Key key) {
            var v = super.getOneKey(key);
            if (v == null) {
                return BigInteger.ZERO;
            }
            return v;
        }

        @Override
        protected BigInteger putOne(Key key, BigInteger value) {
            var v = super.putOne(key, value);
            if (v == null) {
                return BigInteger.ZERO;
            }
            return v;
        }

        @Override
        protected BigInteger deleteOne(Key key) {
            var v = super.deleteOne(key);
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
