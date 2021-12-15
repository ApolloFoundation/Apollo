/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.service.storage;

import com.apollocurrency.aplwallet.apl.smc.service.SmcContractStorageService;
import com.apollocurrency.aplwallet.apl.smc.service.mapping.PersistentMappingRepository;
import com.apollocurrency.smc.blockchain.storage.AddressJsonConverter;
import com.apollocurrency.smc.data.type.Address;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * @author andrew.zinchenko@gmail.com
 */
class PersistentMappingRepositoryTest {
    Address contract = mock(Address.class);
    SmcContractStorageService smcContractStorageService = mock(SmcContractStorageService.class);

    @Test
    void javaTypeName() {
        var repository = new PersistentMappingRepository<>(smcContractStorageService, contract, "mappingName", new AddressJsonConverter());
        assertNotNull(repository);
        assertEquals("Mapping<Address>", repository.getJavaTypeName());
    }

}