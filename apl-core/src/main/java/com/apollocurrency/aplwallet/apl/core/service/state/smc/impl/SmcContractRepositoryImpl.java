/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.api.v2.model.ContractDetails;
import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractModelToStateEntityConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractStateTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractStateEntity;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractRepository;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractService;
import com.apollocurrency.aplwallet.apl.smc.model.AplAddress;
import com.apollocurrency.aplwallet.apl.smc.model.AplContractSpec;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
import com.apollocurrency.smc.contract.AddressNotFoundException;
import com.apollocurrency.smc.contract.ContractSource;
import com.apollocurrency.smc.contract.ContractStatus;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.fuel.Fuel;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.polyglot.SimpleVersion;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class SmcContractRepositoryImpl implements SmcContractRepository {
    private final Blockchain blockchain;
    private final SmcContractTable smcContractTable;
    private final SmcContractStateTable smcContractStateTable;

    private final ContractModelToEntityConverter contractModelToEntityConverter;
    private final ContractModelToStateEntityConverter contractModelToStateConverter;

    protected final SmcConfig smcConfig;
    private final SmcContractService contractService;

    @Inject
    public SmcContractRepositoryImpl(Blockchain blockchain, SmcContractTable smcContractTable, SmcContractStateTable smcContractStateTable, ContractModelToEntityConverter contractModelToEntityConverter, ContractModelToStateEntityConverter contractModelToStateConverter, SmcConfig smcConfig, SmcContractService contractService) {
        this.blockchain = blockchain;
        this.smcContractTable = smcContractTable;
        this.smcContractStateTable = smcContractStateTable;
        this.contractModelToEntityConverter = contractModelToEntityConverter;
        this.contractModelToStateConverter = contractModelToStateConverter;
        this.smcConfig = smcConfig;
        this.contractService = contractService;
    }

    @Override
    @Transactional
    public void saveContract(SmartContract contract, long transactionId, byte[] transactionHash) {
        //it's a new contract
        SmcContractEntity smcContractEntity = contractModelToEntityConverter.convert(contract);
        smcContractEntity.setHeight(blockchain.getHeight()); // new height value
        smcContractEntity.setBlockTimestamp(blockchain.getLastBlockTimestamp());
        smcContractEntity.setTransactionId(transactionId);
        smcContractEntity.setTransactionHash(transactionHash);
        SmcContractStateEntity smcContractStateEntity = contractModelToStateConverter.convert(contract);
        smcContractStateEntity.setHeight(blockchain.getHeight()); // new height value
        log.debug("Save smart contract at height {}, smc={}, state={}", smcContractEntity.getHeight(), smcContractEntity, smcContractStateEntity);

        //save contract
        smcContractTable.insert(smcContractEntity);
        //save state
        smcContractStateTable.insert(smcContractStateEntity);
    }

    /**
     * Load the saved contract by the given address or null if the given address doesn't correspond the smart contract
     * The loaded smart contract instance hase an undefined fuel value.
     *
     * @param address      given contract address
     * @param originator   the origin transaction sender
     * @param caller       the contract caller
     * @param contractFuel given fuel to execute method calling
     * @return loaded smart contract or throw {@link com.apollocurrency.smc.contract.AddressNotFoundException}
     */
    @Override
    @Transactional(readOnly = true)
    public SmartContract loadContract(Address address, Address originator, Address caller, Fuel contractFuel) {

        SmcContractEntity smcEntity = loadContractEntity(address);
        SmcContractStateEntity smcStateEntity = loadContractStateEntity(address);

        SmartContract contract = convert(smcEntity, smcStateEntity, originator, caller, contractFuel);
        log.debug("Loaded contract={}", contract);

        return contract;
    }

    /**
     * Load the contract specification by the given address or null if the given address doesn't correspond the smart contract
     *
     * @param address given contract address
     * @return loaded smart contract specification or throw {@link com.apollocurrency.smc.contract.AddressNotFoundException}
     */
    @Override
    @Transactional(readOnly = true)
    public AplContractSpec loadAsrModuleSpec(Address address) {
        SmcContractEntity smcEntity = loadContractEntity(address);
        log.trace("Loaded specification for contract name={} type={}", smcEntity.getContractName(), smcEntity.getBaseContract());
        return contractService.loadAsrModuleSpec(smcEntity.getBaseContract(), smcEntity.getLanguageName(), SimpleVersion.fromString(smcEntity.getLanguageVersion()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isContractExist(Address address) {
        AplAddress aplAddress = new AplAddress(address);
        SmcContractStateEntity smcStateEntity = smcContractStateTable.get(SmcContractStateTable.KEY_FACTORY.newKey(aplAddress.getLongId()));
        return (smcStateEntity != null);
    }

    @Override
    @Transactional
    public void updateContractState(SmartContract contract) {
        SmcContractStateEntity smcContractStateEntity = loadContractStateEntity(contract.address(), true);
        if (smcContractStateEntity == null) {
            smcContractStateEntity = contractModelToStateConverter.convert(contract);
        }
        smcContractStateEntity.setSerializedObject(contract.getSerializedObject());
        smcContractStateEntity.setStatus(contract.getStatus().name());
        smcContractStateEntity.setHeight(blockchain.getHeight()); // new height value
        smcContractStateTable.insert(smcContractStateEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public String loadSerializedContract(Address address) {
        SmcContractStateEntity smcStateEntity = loadContractStateEntity(address);
        return smcStateEntity.getSerializedObject();
    }

    @Override
    @Transactional
    public void saveSerializedContract(SmartContract contract, String serializedObject) {
        Objects.requireNonNull(contract);
        Objects.requireNonNull(serializedObject);

        SmcContractStateEntity smcContractStateEntity = loadContractStateEntity(contract.address());
        smcContractStateEntity.setSerializedObject(serializedObject);
        smcContractStateTable.insert(smcContractStateEntity);
    }

    @Override
    public List<ContractDetails> loadContractsByFilter(Address address, Address transaction, Address owner, String name, String baseContract, Long timestamp, ContractStatus status, int height, int from, int to) {
        Long contractId = address != null ? new AplAddress(address).getLongId() : null;
        Long txId = transaction != null ? new AplAddress(transaction).getLongId() : null;
        Long ownerId = owner != null ? new AplAddress(owner).getLongId() : null;
        Integer blockTimestamp = timestamp == null ? null : Convert2.toEpochTime(timestamp);
        List<ContractDetails> result = smcContractTable.getContractsByFilter(contractId, txId, ownerId, name, baseContract, blockTimestamp, status != null ? status.name() : null, height < 0 ? blockchain.getHeight() : height, from, to);
        return result;
    }

    @Override
    public List<ContractDetails> getContractDetailsByAddress(Address address) {
        var result = loadContractsByFilter(address, null, null, null, null, null, null, -1, 0, 1);
        return result;
    }

    public static SmartContract convert(SmcContractEntity entity, SmcContractStateEntity stateEntity, Address originator, Address caller, Fuel contractFuel) {
        return SmartContract.builder()
            .address(new AplAddress(entity.getAddress()))
            .owner(new AplAddress(entity.getOwner()))
            .originator(originator)
            .caller(caller)
            .txId(new AplAddress(entity.getTransactionId()))
            .args(entity.getArgs())
            .code(ContractSource.builder()
                .sourceCode(entity.getData())
                .name(entity.getContractName())
                .baseContract(entity.getBaseContract())
                .languageName(entity.getLanguageName())
                .languageVersion(SimpleVersion.fromString(entity.getLanguageVersion()))
                .build()
            )
            .serializedObject(stateEntity.getSerializedObject())
            .status(ContractStatus.valueOf(stateEntity.getStatus()))
            .fuel(contractFuel)
            .build();
    }

    private SmcContractStateEntity loadContractStateEntity(Address address) {
        return loadContractStateEntity(address, false);
    }

    private SmcContractStateEntity loadContractStateEntity(Address address, boolean quiet) {
        AplAddress aplAddress = new AplAddress(address);
        SmcContractStateEntity smcStateEntity = smcContractStateTable.get(SmcContractStateTable.KEY_FACTORY.newKey(aplAddress.getLongId()));
        if (smcStateEntity == null && !quiet) {
            log.error("Contract state not found at addr={}", address.getHex());
            throw new AddressNotFoundException(address);
        }
        return smcStateEntity;
    }

    private SmcContractEntity loadContractEntity(Address address) {
        AplAddress aplAddress = new AplAddress(address);
        SmcContractEntity smcContractEntity = smcContractTable.get(SmcContractTable.KEY_FACTORY.newKey(aplAddress.getLongId()));
        if (smcContractEntity == null) {
            log.error("Contract not found at addr={}", address.getHex());
            throw new AddressNotFoundException(address);
        }
        return smcContractEntity;
    }
}
