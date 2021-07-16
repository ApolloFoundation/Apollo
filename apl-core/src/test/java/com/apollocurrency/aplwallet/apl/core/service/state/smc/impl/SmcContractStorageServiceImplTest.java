/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractMappingTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractMappingEntity;
import com.apollocurrency.aplwallet.apl.core.model.smc.AplAddress;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractStorageService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.smc.data.type.Key;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author andrew.zinchenko@gmail.com
 */
@ExtendWith(MockitoExtension.class)
class SmcContractStorageServiceImplTest {
    static {
        Convert2.init("APL", 1739068987193023818L);
    }

    @Mock
    Blockchain blockchain;
    @Mock
    SmcContractMappingTable smcContractMappingTable;

    AplAddress contractAddress = new AplAddress(Convert.parseAccountId("APL-632K-TWX3-2ALQ-973CU"));

    SmcContractStorageService smcContractStorageService;

    @BeforeEach
    void setUp() {
        initMocks(this);
        smcContractStorageService = new SmcContractStorageServiceImpl(blockchain, smcContractMappingTable);
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
            .serializedObject(json)
            .height(blockchain.getHeight()) // new height value
            .build();
        //WHEN
        smcContractStorageService.saveOrUpdateEntry(contractAddress, key, name, json);
        //THEN
        verify(smcContractMappingTable, times(1)).insert(smcContractMappingEntity);
    }

    @Test
    void loadEntry() {
        //GIVEN
        String json = "{}";
        String name = "mapping";
        Key key = new AplAddress("0x0102030405");
        SmcContractMappingEntity smcContractMappingEntity = SmcContractMappingEntity.builder()
            .address(contractAddress.getLongId())
            .key(key.key())
            .name(name)
            .serializedObject(json)
            .height(blockchain.getHeight()) // new height value
            .build();
        when(smcContractMappingTable.get(SmcContractMappingTable.KEY_FACTORY.newKey(contractAddress.getLongId(), key.key()))).thenReturn(smcContractMappingEntity);
        //WHEN
        String actualJson = smcContractStorageService.loadEntry(contractAddress, key);
        //THEN
        assertEquals(json, actualJson);
    }

    @Test
    void deleteEntry() {
        //GIVEN
        String json = "{}";
        String name = "mapping";
        Key key = new AplAddress("0x0102030405");
        SmcContractMappingEntity smcContractMappingEntity = SmcContractMappingEntity.builder()
            .address(contractAddress.getLongId())
            .key(key.key())
            .name(name)
            .serializedObject(json)
            .height(blockchain.getHeight()) // new height value
            .build();
        when(smcContractMappingTable.get(SmcContractMappingTable.KEY_FACTORY.newKey(contractAddress.getLongId(), key.key())))
            .thenReturn(smcContractMappingEntity);
        when(smcContractMappingTable.deleteAtHeight(smcContractMappingEntity, blockchain.getHeight()))
            .thenReturn(true);
        //WHEN
        boolean rc = smcContractStorageService.deleteEntry(contractAddress, key);
        //THEN
        assertTrue(rc);
    }

    @Test
    void deleteEntry_False() {
        //GIVEN
        Key key = new AplAddress("0x0102030405");
        when(smcContractMappingTable.get(SmcContractMappingTable.KEY_FACTORY.newKey(contractAddress.getLongId(), key.key())))
            .thenReturn(null);
        //WHEN
        boolean rc = smcContractStorageService.deleteEntry(contractAddress, key);
        //THEN
        assertFalse(rc);
    }

    @Test
    void isMappingExist() {
        //GIVEN
        String name = "mapping";
        when(smcContractMappingTable.getCount(any(DbClause.class))).thenReturn(1);
        //WHEN
        boolean rc = smcContractStorageService.isMappingExist(contractAddress, name);
        //THEN
        assertTrue(rc);
    }

    @Test
    void isMappingNonExist() {
        //GIVEN
        String name = "mapping";
        when(smcContractMappingTable.getCount(any(DbClause.class))).thenReturn(0);
        //WHEN
        boolean rc = smcContractStorageService.isMappingExist(contractAddress, name);
        //THEN
        assertFalse(rc);
    }
}