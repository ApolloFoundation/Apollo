/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractModelToStateEntityConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractMappingTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractMappingEntity;
import com.apollocurrency.aplwallet.apl.core.model.smc.AplAddress;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractStorageService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.smc.data.type.Key;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author andrew.zinchenko@gmail.com
 */
@ExtendWith(MockitoExtension.class)
class SmcContractStorageServiceImplTest {
    static int HEIGHT = 100;
    static long TX_ID = 100L;

    static {
        Convert2.init("APL", 1739068987193023818L);
    }


    @Mock
    Blockchain blockchain;
    @Mock
    SmcContractMappingTable smcContractMappingTable;

    ContractModelToEntityConverter contractModelToEntityConverter = new ContractModelToEntityConverter();

    ContractModelToStateEntityConverter contractModelToStateConverter = new ContractModelToStateEntityConverter();

    AplAddress contractAddress;

    SmcContractStorageService smcContractStorageService;

    @BeforeEach
    void setUp() {
        initMocks(this);

        smcContractStorageService = new SmcContractStorageServiceImpl(blockchain, smcContractMappingTable);

        contractAddress = new AplAddress(Convert.parseAccountId("APL-632K-TWX3-2ALQ-973CU"));

    }


    @Test
    void saveEntry() {
        //GIVEN
        String json = "{}";
        String name = "mapping";
        Key key = new AplAddress("0x0102030405");
        SmcContractMappingEntity smcContractMappingEntity = SmcContractMappingEntity.builder()
            .address(contractAddress.getLongId())
            .key(key.key())
            .name(name)
            .height(blockchain.getHeight()) // new height value
            .build();

        //WHEN
        smcContractStorageService.saveEntry(contractAddress, key, name, json);

        //THEN
        verify(smcContractMappingTable, times(1)).insert(smcContractMappingEntity);

    }

    @Test
    void loadEntry() {
    }

    @Test
    void deleteEntry() {
    }

    @Test
    void isMappingExist() {
    }
}