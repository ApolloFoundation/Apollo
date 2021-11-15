/*
 * Copyright (c) 2020-2021. Apollo Foundation.
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
import com.apollocurrency.aplwallet.apl.core.exception.AplCoreContractViolationException;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcPublishContractAttachment;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.smc.model.AplAddress;
import com.apollocurrency.aplwallet.apl.smc.model.AplContractSpec;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.smc.contract.AddressNotFoundException;
import com.apollocurrency.smc.contract.ContractSource;
import com.apollocurrency.smc.contract.ContractStatus;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.fuel.ContractFuel;
import com.apollocurrency.smc.contract.fuel.Fuel;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.polyglot.SimpleVersion;
import com.apollocurrency.smc.polyglot.Version;
import com.apollocurrency.smc.polyglot.language.LanguageContext;
import com.apollocurrency.smc.polyglot.language.lib.LibraryProvider;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class SmcContractServiceImpl implements SmcContractService {
    private final Blockchain blockchain;
    private final SmcContractTable smcContractTable;
    private final SmcContractStateTable smcContractStateTable;

    private final ContractModelToEntityConverter contractModelToEntityConverter;
    private final ContractModelToStateEntityConverter contractModelToStateConverter;

    protected final SmcConfig smcConfig;
    private final LibraryProvider libraryProvider;

    @Inject
    public SmcContractServiceImpl(Blockchain blockchain, SmcContractTable smcContractTable, SmcContractStateTable smcContractStateTable, ContractModelToEntityConverter contractModelToEntityConverter, ContractModelToStateEntityConverter contractModelToStateConverter, SmcConfig smcConfig) {
        this.blockchain = blockchain;
        this.smcContractTable = smcContractTable;
        this.smcContractStateTable = smcContractStateTable;
        this.contractModelToEntityConverter = contractModelToEntityConverter;
        this.contractModelToStateConverter = contractModelToStateConverter;
        this.smcConfig = smcConfig;
        final LanguageContext languageContext = smcConfig.createLanguageContext();
        this.libraryProvider = languageContext.getLibraryProvider();
    }

    @Override
    @Transactional
    public void saveContract(SmartContract contract) {
        //it's a new contract
        SmcContractEntity smcContractEntity = contractModelToEntityConverter.convert(contract);
        smcContractEntity.setHeight(blockchain.getHeight()); // new height value
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

    @Override
    public SmartContract loadContract(Address address, Address originator, Fuel contractFuel) {
        return loadContract(address, originator, originator, contractFuel);
    }

    @Override
    public SmartContract loadContract(Address address) {
        //originator and caller are ignored
        return loadContract(address, null, null, new ContractFuel(address, 0, 0));
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
        return loadAsrModuleSpec(smcEntity.getBaseContract(), smcEntity.getLanguageName(), SimpleVersion.fromString(smcEntity.getLanguageVersion()));
    }

    @SneakyThrows
    @Override
    public AplContractSpec loadAsrModuleSpec(String asrModuleName, String language, Version version) {
        checkLibraryCompatibility(language, version);
        var contractSpec = libraryProvider.loadSpecification(asrModuleName);
        log.trace("Loaded specification for module={}, spec={}", asrModuleName, contractSpec);
        var src = libraryProvider.importModule(asrModuleName);
        return AplContractSpec.builder()
            .language(language)
            .version(version)
            .contractSpec(contractSpec)
            .content(src.getSourceCode())
            .build();
    }

    @Override
    public List<String> getInheritedAsrModules(String asrModuleName, String language, Version version) {
        checkLibraryCompatibility(language, version);
        var resultList = new ArrayList<String>();
        resultList.add(asrModuleName);
        resultList.addAll(libraryProvider.getInheritedModules(asrModuleName));
        return resultList;
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
        SmcContractStateEntity smcContractStateEntity = loadContractStateEntity(contract.getAddress(), true);
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

        SmcContractStateEntity smcContractStateEntity = loadContractStateEntity(contract.getAddress());
        smcContractStateEntity.setSerializedObject(serializedObject);
        smcContractStateTable.insert(smcContractStateEntity);
    }

    @Override
    public List<ContractDetails> loadContractsByOwner(Address owner, int from, int limit) {
        long id = new AplAddress(owner).getLongId();
        List<ContractDetails> result = CollectionUtil.toList(
            smcContractTable.getManyBy(new DbClause.LongClause("owner", id), from, limit))
            .stream()
            .map(value -> getContractDetailsByTransaction(new AplAddress(value.getTransactionId())))
            .collect(Collectors.toList());
        return result;
    }

    @Override
    public List<ContractDetails> loadContractsByFilter(Address address, Address owner, String name, ContractStatus status, int height, int from, int to) {
        Long contractId = address != null ? new AplAddress(address).getLongId() : null;
        Long ownerId = owner != null ? new AplAddress(owner).getLongId() : null;
        List<ContractDetails> result = smcContractTable.getContractsByFilter(contractId, ownerId, name, status != null ? status.name() : null, height < 0 ? blockchain.getHeight() : height, from, to);
        return result;
    }

    @Override
    public ContractDetails getContractDetailsByAddress(Address address) {
        SmcContractEntity smcEntity = loadContractEntity(address);
        AplAddress txAddress = new AplAddress(smcEntity.getTransactionId());
        return getContractDetailsByTransaction(txAddress);
    }

    @Override
    public ContractDetails getContractDetailsByTransaction(Address txAddress) {
        Transaction smcTransaction = blockchain.getTransaction(new AplAddress(txAddress).getLongId());
        if (smcTransaction == null) {
            log.error("Transaction not found, addr={}", txAddress.getHex());
            throw new IllegalArgumentException("Transaction not found, addr=" + txAddress.getHex());
        }
        if (smcTransaction.getAttachment().getTransactionTypeSpec() != TransactionTypes.TransactionTypeSpec.SMC_PUBLISH) {
            throw new IllegalStateException("Invalid transaction attachment: " + smcTransaction.getAttachment().getTransactionTypeSpec()
                + ", expected " + TransactionTypes.TransactionTypeSpec.SMC_PUBLISH);
        }
        SmcPublishContractAttachment attachment = (SmcPublishContractAttachment) smcTransaction.getAttachment();
        AplAddress contractAddress = new AplAddress(smcTransaction.getRecipientId());

        SmcContractEntity smcContractEntity = loadContractEntity(contractAddress);

        ContractDetails contract = new ContractDetails();
        contract.setAddress(Convert2.rsAccount(smcContractEntity.getAddress()));
        contract.setTransaction(Long.toUnsignedString(smcContractEntity.getTransactionId()));
        contract.setAmount(Long.toUnsignedString(smcTransaction.getAmountATM()));
        contract.setFee(Long.toUnsignedString(smcTransaction.getFeeATM()));
        contract.setSignature(smcTransaction.getSignature().getHexString());
        contract.setTimestamp(Convert2.fromEpochTime(smcTransaction.getBlockTimestamp()));
        contract.setName(smcContractEntity.getContractName());
        contract.setBaseContract(smcContractEntity.getBaseContract());
        contract.setParams(smcContractEntity.getArgs());
        contract.setSrc(smcContractEntity.getData());
        contract.setFuelLimit(attachment.getFuelLimit().toString());
        contract.setFuelPrice(attachment.getFuelPrice().toString());
        SmcContractStateEntity smcContractStateEntity = loadContractStateEntity(contractAddress, true);
        contract.setStatus(smcContractStateEntity != null ? smcContractStateEntity.getStatus() : ContractStatus.CREATED.name());
        log.trace("Transaction details, tx addr={} {}", txAddress, contract);
        return contract;
    }

    @Override
    public List<String> getAsrModules(String language, Version version, String type) {
        if (libraryProvider.isCompatible(language, version)) {
            return libraryProvider.getAsrModules(type);
        } else {
            return List.of();
        }
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
                .parsedSourceCode(entity.getData())
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

    private void checkLibraryCompatibility(String language, Version version) {
        if (!libraryProvider.isCompatible(language, version)) {
            throw new AplCoreContractViolationException("The library provider is not compatible for the given version of language ["
                + language + ":" + version + "], expected language="
                + libraryProvider.getLanguageName() + ", version=" + libraryProvider.getLanguageVersion());
        }
    }

}