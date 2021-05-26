/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractMappingTable;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractMappingEntity;
import com.apollocurrency.aplwallet.apl.core.model.smc.AplAddress;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractStorageService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.data.type.Key;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

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
    public void saveEntry(Address address, Key key, String name, String jsonObject) {
        SmcContractMappingEntity smcContractMappingEntity = SmcContractMappingEntity.builder()
            .address(new AplAddress(address).getLongId())
            .key(key.key())
            .name(name)
            .serializedObject(jsonObject)
            .height(blockchain.getHeight()) // new height value
            .build();
        smcContractMappingTable.insert(smcContractMappingEntity);
        log.trace("Saved mapping={}", smcContractMappingEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public String loadEntry(Address address, Key key) {
        long id = new AplAddress(address).getLongId();
        SmcContractMappingEntity smcContractMappingEntity = smcContractMappingTable.get(SmcContractMappingTable.KEY_FACTORY.newKey(id, key.key()));
        if (smcContractMappingEntity != null) {
            log.trace("Load mapping={}", smcContractMappingEntity);
            return smcContractMappingEntity.getSerializedObject();
        }
        log.trace("Load: mapping not found, address={} key={}", address.getHex(), Convert.toHexString(key.key()));
        return null;
    }

    @Override
    public boolean deleteEntry(Address address, Key key) {
        boolean rc = false;
        long id = new AplAddress(address).getLongId();
        SmcContractMappingEntity smcContractMappingEntity = smcContractMappingTable.get(SmcContractMappingTable.KEY_FACTORY.newKey(id, key.key()));
        if (smcContractMappingEntity != null) {
            int height = blockchain.getHeight();
            rc = smcContractMappingTable.deleteAtHeight(smcContractMappingEntity, height);
            log.trace("Delete mapping={} at height={} rc={}", smcContractMappingEntity, height, rc);
        } else {
            log.trace("Delete: mapping not found, address={} key={}", address.getHex(), Convert.toHexString(key.key()));
        }
        return rc;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMappingExist(Address address, String name) {
        long id = new AplAddress(address).getLongId();
        int count = smcContractMappingTable.getCount(
            new DbClause.LongClause("address", id).and(new DbClause.StringClause("name", name))
        );
        log.trace("Found {} entries, address={} mapping name={}", count, address.getHex(), name);
        return count > 0;
    }
}
