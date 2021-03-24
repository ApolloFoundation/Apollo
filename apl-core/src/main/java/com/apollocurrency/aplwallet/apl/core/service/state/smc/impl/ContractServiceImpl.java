/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractModelToStateEntityConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractStateTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractStateEntity;
import com.apollocurrency.aplwallet.apl.core.model.smc.AplAddress;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcPublishContractAttachment;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
import com.apollocurrency.smc.blockchain.SMCNotFoundException;
import com.apollocurrency.smc.blockchain.crypt.HashSumProvider;
import com.apollocurrency.smc.contract.ContractState;
import com.apollocurrency.smc.contract.ContractType;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.SmartSource;
import com.apollocurrency.smc.contract.fuel.ContractFuel;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.persistence.tx.log.ArrayTxLog;
import com.apollocurrency.smc.persistence.tx.log.TxLog;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class ContractServiceImpl implements ContractService {
    private SmcContractTable smcContractTable;
    private SmcContractStateTable smcContractStateTable;

    private ContractModelToEntityConverter contractModelToEntityConverter;
    private ContractModelToStateEntityConverter contractModelToStateConverter;

    private HashSumProvider hashSumProvider;

    @Inject
    public ContractServiceImpl(SmcContractTable smcContractTable, SmcContractStateTable smcContractStateTable, ContractModelToEntityConverter contractModelToEntityConverter, ContractModelToStateEntityConverter contractModelToStateConverter, HashSumProvider hashSumProvider) {
        this.smcContractTable = smcContractTable;
        this.smcContractStateTable = smcContractStateTable;
        this.contractModelToEntityConverter = contractModelToEntityConverter;
        this.contractModelToStateConverter = contractModelToStateConverter;
        this.hashSumProvider = hashSumProvider;
    }

    @Override
    @Transactional
    public void saveContract(SmartContract contract) {
        SmcContractEntity smcContractEntity = contractModelToEntityConverter.convert(contract);
        SmcContractStateEntity smcContractStateEntity = contractModelToStateConverter.convert(contract);
        //save contract
        smcContractTable.insert(smcContractEntity);
        //save state
        smcContractStateTable.insert(smcContractStateEntity);
    }

    /**
     * Load the saved contract by the given address or null if the given address doesn't correspond the smart contract
     * The loaded smart contract instance hase an undefined fuel value.
     *
     * @param address given contract address
     * @return loaded smart contract or throw {@link SMCNotFoundException}
     */
    @Override
    @Transactional(readOnly = true)
    public SmartContract loadContract(Address address) {
        SmcContractEntity smcEntity = loadContractEntity(address);
        SmcContractStateEntity smcStateEntity = loadContractStateEntity(address);

        SmartContract contract = convert(smcEntity, smcStateEntity);
        contract.setTxLog(createLog(address.getHex()));

        return contract;
    }

    @Override
    @Transactional
    public void updateContractState(SmartContract contract) {
        SmcContractStateEntity smcContractStateEntity = contractModelToStateConverter.convert(contract);
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
    public SmartContract createNewContract(Transaction smcTransaction) {
        if (smcTransaction.getAttachment().getTransactionTypeSpec() != TransactionTypes.TransactionTypeSpec.SMC_PUBLISH) {
            throw new IllegalStateException("Invalid transaction attachment: " + smcTransaction.getAttachment().getTransactionTypeSpec()
                + ", expected " + TransactionTypes.TransactionTypeSpec.SMC_PUBLISH);
        }
        SmcPublishContractAttachment attachment = (SmcPublishContractAttachment) smcTransaction.getAttachment();

        Address contractAddress = new AplAddress(smcTransaction.getRecipientId());
        SmartContract contract = SmartContract.builder()
            .address(contractAddress)
            .owner(new AplAddress(smcTransaction.getSenderId()))
            .sender(new AplAddress(smcTransaction.getSenderId()))
            //TODO determine the contract type by source code
            .type(ContractType.PAYABLE)
            .code(SmartSource.builder()
                .sourceCode(attachment.getContractSource())
                .name(attachment.getContractName())
                .args(String.join(",", attachment.getConstructorParams()))
                .languageName(attachment.getLanguageName())
                .build()
            )
            .state(ContractState.CREATED)
            .fuel(new ContractFuel(smcTransaction.getFuelLimit(), smcTransaction.getFuelPrice()))
            .txLog(createLog(contractAddress.getHex()))
            .build();

        log.debug("Created contract={}", contract);

        return contract;
    }

    public static SmartContract convert(SmcContractEntity smcContractEntity, SmcContractStateEntity smcContractStateEntity) {
        return SmartContract.builder()
            .address(new AplAddress(smcContractEntity.getAddress()))
            .owner(new AplAddress(smcContractEntity.getOwner()))
            //TODO get the senderId by transactionId
            .sender(new AplAddress(smcContractEntity.getOwner()))
            //TODO determine the contract type by source code
            .type(ContractType.PAYABLE)
            .code(SmartSource.builder()
                .sourceCode(smcContractEntity.getData())
                .name(smcContractEntity.getContractName())
                .args(smcContractEntity.getArgs())
                .languageName(smcContractEntity.getLanguageName())
                .languageVersion(smcContractEntity.getLanguageVersion())
                .build()
            )
            .serializedObject(smcContractStateEntity.getSerializedObject())
            .state(ContractState.valueOf(smcContractStateEntity.getStatus()))
            .build();
    }

    private SmcContractStateEntity loadContractStateEntity(Address address) {
        AplAddress aplAddress = new AplAddress(address);
        SmcContractStateEntity smcStateEntity = smcContractStateTable.get(SmcContractStateTable.KEY_FACTORY.newKey(aplAddress.getLongId()));
        if (smcStateEntity == null) {
            log.error("Contract state not found at addr={}", address.getHex());
            throw new SMCNotFoundException("Contract state not found at addr=" + address.getHex());
        }
        return smcStateEntity;
    }

    private SmcContractEntity loadContractEntity(Address address) {
        AplAddress aplAddress = new AplAddress(address);
        SmcContractEntity smcContractEntity = smcContractTable.get(SmcContractTable.KEY_FACTORY.newKey(aplAddress.getLongId()));
        if (smcContractEntity == null) {
            log.error("Contract not found at addr={}", address.getHex());
            throw new SMCNotFoundException("Contract not found at addr=" + address.getHex());
        }
        return smcContractEntity;
    }

    private TxLog createLog(String address) {
        return new ArrayTxLog(address, hashSumProvider);
    }

}
