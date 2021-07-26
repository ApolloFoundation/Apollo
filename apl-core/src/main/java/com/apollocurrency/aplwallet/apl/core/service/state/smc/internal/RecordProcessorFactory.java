/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.internal;

import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcMappingRepositoryClassFactory;
import com.apollocurrency.smc.blockchain.storage.ContractMappingRepository;
import com.apollocurrency.smc.blockchain.storage.ContractMappingRepositoryFactory;
import com.apollocurrency.smc.persistence.record.RecordProcessor;
import com.apollocurrency.smc.persistence.record.RemoteCallRecord;
import com.apollocurrency.smc.persistence.record.TransferRecord;
import com.apollocurrency.smc.persistence.record.WriteMappingRecord;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class RecordProcessorFactory {
    private final AccountService accountService;
    private final Blockchain blockchain;
    private final SmcMappingRepositoryClassFactory smcMappingRepositoryClassFactory;

    @Inject
    public RecordProcessorFactory(AccountService accountService, Blockchain blockchain, SmcMappingRepositoryClassFactory smcMappingRepositoryClassFactory) {
        this.accountService = Objects.requireNonNull(accountService);
        this.blockchain = Objects.requireNonNull(blockchain);
        this.smcMappingRepositoryClassFactory = Objects.requireNonNull(smcMappingRepositoryClassFactory);
    }

    public RecordProcessor<RemoteCallRecord> createRemoteCallRecordProcessor() {
        return (header, data) -> {
            var contract = header.getContract();


            if (data.getOperation() == WriteMappingRecord.Operation.UPDATE) {
                repository.set(data.getKey(), data.getValue());
            } else {//DELETE
                repository.delete(data.getKey());
            }
        };
    }

    public RecordProcessor<TransferRecord> createTransferRecordProcessor() {
        return (header, data) -> {
            var contract = header.getContract();
            var mapFactory = smcMappingRepositoryClassFactory.createMappingFactory(contract);
            ContractMappingRepository repository = getRepository(data, mapFactory);

            if (data.getOperation() == WriteMappingRecord.Operation.UPDATE) {
                repository.set(data.getKey(), data.getValue());
            } else {//DELETE
                repository.delete(data.getKey());
            }
        };
    }


    public RecordProcessor<WriteMappingRecord> createWriteMappingRecordProcessor() {
        return (header, data) -> {
            var contract = header.getContract();
            var mapFactory = smcMappingRepositoryClassFactory.createMappingFactory(contract);
            ContractMappingRepository repository = getRepository(data, mapFactory);

            if (data.getOperation() == WriteMappingRecord.Operation.UPDATE) {
                repository.set(data.getKey(), data.getValue());
            } else {//DELETE
                repository.delete(data.getKey());
            }
        };
    }


    private ContractMappingRepository getRepository(WriteMappingRecord data, ContractMappingRepositoryFactory mapFactory) {
        ContractMappingRepository repository;
        if (data.getMappingType() == WriteMappingRecord.MappingType.ADDRESS) {
            repository = mapFactory.addressRepository(data.getName());
        } else if (data.getMappingType() == WriteMappingRecord.MappingType.BIG_NUM) {
            repository = mapFactory.bigNumRepository(data.getName());
        } else if (data.getMappingType() == WriteMappingRecord.MappingType.BIG_INTEGER) {
            repository = mapFactory.bigIntegerRepository(data.getName());
        } else if (data.getMappingType() == WriteMappingRecord.MappingType.STRING) {
            repository = mapFactory.stringRepository(data.getName());
        } else throw new IllegalStateException("Unsupported mapping type: " + data.getMappingType());
        return repository;
    }


}
