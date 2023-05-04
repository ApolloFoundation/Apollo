/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractMappingTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractMappingEntity;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.smc.model.AplAddress;
import com.apollocurrency.aplwallet.apl.smc.service.SmcContractStorageService;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.data.type.Key;
import lombok.extern.slf4j.Slf4j;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
@Slf4j
public class SmcContractStorageServiceImpl implements SmcContractStorageService {
    private final Blockchain blockchain;
    private final SmcContractMappingTable smcContractMappingTable;

    @Inject
    public SmcContractStorageServiceImpl(Blockchain blockchain, SmcContractMappingTable smcContractMappingTable) {
        this.blockchain = blockchain;
        this.smcContractMappingTable = smcContractMappingTable;
    }

    @Override
    @Transactional
    public void saveOrUpdateEntry(Address contract, Key key, String name, String jsonObject) {
        SmcContractMappingEntity smcContractMappingEntity = getContractMappingEntity(contract, key, name);
        if (smcContractMappingEntity != null) {
            //update entity
            smcContractMappingEntity.setSerializedObject(jsonObject);
            smcContractMappingEntity.setHeight(blockchain.getHeight());
            log.trace("Update mapping={} at height={}", smcContractMappingEntity, blockchain.getHeight());
        } else {
            //new entity
            smcContractMappingEntity = SmcContractMappingEntity.builder()
                .address(new AplAddress(contract).getLongId())
                .key(key.key())
                .name(name)
                .serializedObject(jsonObject)
                .height(blockchain.getHeight()) // new height value
                .build();
            log.trace("Save new mapping={} at height={}", smcContractMappingEntity, blockchain.getHeight());
        }
        smcContractMappingTable.insert(smcContractMappingEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public String loadEntry(Address contract, Key key, String name) {
        SmcContractMappingEntity smcContractMappingEntity = getContractMappingEntity(contract, key, name);
        if (smcContractMappingEntity != null) {
            log.trace("Load mapping={} at height={}", smcContractMappingEntity, blockchain.getHeight());
            return smcContractMappingEntity.getSerializedObject();
        }
        log.trace("Load: mapping={} not found, contract={} key={} at height={}",
            name, contract.getHex(), Convert.toHexString(key.key()), blockchain.getHeight());
        return null;
    }

    @Override
    public boolean deleteEntry(Address contract, Key key, String name) {
        boolean rc = false;
        SmcContractMappingEntity smcContractMappingEntity = getContractMappingEntity(contract, key, name);
        if (smcContractMappingEntity != null) {
            int height = blockchain.getHeight();
            smcContractMappingEntity.setHeight(height);
            rc = smcContractMappingTable.deleteAtHeight(smcContractMappingEntity, height);
            log.trace("Delete mapping={} at height={} rc={}", smcContractMappingEntity, height, rc);
        } else {
            log.trace("Delete: mapping={} not found, contract={} key={}", name, contract.getHex(), Convert.toHexString(key.key()));
        }
        return rc;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMappingExist(Address contract, String name) {
        long id = new AplAddress(contract).getLongId();
        int count = smcContractMappingTable.getCount(
            new DbClause.LongClause("address", id).and(new DbClause.StringClause("name", name))
        );
        log.trace("Found {} entries, contract={} mapping={}", count, contract.getHex(), name);
        return count > 0;
    }

    private SmcContractMappingEntity getContractMappingEntity(Address contract, Key key, String name) {
        long id = new AplAddress(contract).getLongId();
        return smcContractMappingTable.get(SmcContractMappingTable.KEY_FACTORY.newKey(id, name, key.key()));
    }

}
