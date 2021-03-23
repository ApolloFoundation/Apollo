/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.SmcContractStateToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractStateTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractStateEntity;
import com.apollocurrency.aplwallet.apl.core.model.smc.AplAddress;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcPublishContractAttachment;
import com.apollocurrency.smc.blockchain.crypt.HashSumProvider;
import com.apollocurrency.smc.contract.ContractState;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.SmartSource;
import com.apollocurrency.smc.contract.fuel.ContractFuel;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.persistence.tx.log.ArrayTxLog;
import com.apollocurrency.smc.persistence.tx.log.TxLog;
import com.apollocurrency.smc.polyglot.LanguageContextFactory;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;
import java.util.Optional;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class ContractServiceImpl implements ContractService {
    private AccountService accountService;
    private SmcContractTable smcContractTable;
    private SmcContractStateTable smcContractStateTable;

    private SmcContractStateToEntityConverter smcContractStateToEntityConverter;

    private HashSumProvider hashSumProvider;

    @Inject
    public ContractServiceImpl(AccountService accountService, SmcContractTable smcContractTable, SmcContractStateTable smcContractStateTable, SmcContractStateToEntityConverter smcContractStateToEntityConverter, HashSumProvider hashSumProvider) {
        this.accountService = accountService;
        this.smcContractTable = smcContractTable;
        this.smcContractStateTable = smcContractStateTable;
        this.smcContractStateToEntityConverter = smcContractStateToEntityConverter;
        this.hashSumProvider = hashSumProvider;
    }

    @Override
    public void saveContract(SmartContract contract) {


    }

    @Override
    public SmartContract loadContract(Address address) {
        AplAddress aplAddress = new AplAddress(address);
        SmcContractEntity smcEntity = smcContractTable.get(SmcContractTable.KEY_FACTORY.newKey(aplAddress.getLongId()));

        if (smcEntity == null) {
            throw new RuntimeException("Contract not found at addr=" + address.getHex());
        }
        SmcContractStateEntity smcStateEntity = smcContractStateTable.get(SmcContractStateTable.KEY_FACTORY.newKey(aplAddress.getLongId()));
        if (smcStateEntity == null) {
            throw new RuntimeException("Contract state not found at addr=" + address.getHex());
        }

        SmartContract contract = create(smcEntity, smcStateEntity);

        return contract;
    }

    @Override
    public void updateContractState(SmartContract contract) {

    }

    @Override
    public String loadSerializedContract(Address address) {
        SmartContract loadedContract = null; /*contractService.findByAddress(address).orElseThrow(() -> {
            log.error("Contract at address {} not found.", address);
            return new SMCNotFoundException("Contract at address " + address + " not found.");
        });*/
        return loadedContract.getSerializedObject();
    }

    @Override
    public boolean saveSerializedContract(Address address, String serializedObject) {
        Objects.requireNonNull(address);
        Objects.requireNonNull(serializedObject);

        Optional<SmartContract> loadedContract = null;//contractService.findByAddress(address);
        if (loadedContract.isPresent()) {
            //contractService.updateSerializedObjectByAddress(address, serializedObject);
            log.info("SmartContract status updated, address={}", address);

            return true;
        } else {
            log.error("Contract at address {} not found.", address);
            return false;
        }
    }

    @Override
    public SmartContract createNewContract(Transaction smcTransaction) {
        if (smcTransaction.getAttachment().getTransactionTypeSpec() != TransactionTypes.TransactionTypeSpec.SMC_PUBLISH) {
            throw new IllegalStateException("Invalid transaction attachment: " + smcTransaction.getAttachment().getTransactionTypeSpec());
        }
        SmcPublishContractAttachment attachment = (SmcPublishContractAttachment) smcTransaction.getAttachment();

        Address contractAddress = new AplAddress(smcTransaction.getRecipientId());
        SmartContract contract = SmartContract.builder()
            .address(contractAddress)
            .owner(new AplAddress(smcTransaction.getSenderId()))
            .sender(new AplAddress(smcTransaction.getSenderId()))
            .code(SmartSource.builder()
                .sourceCode(attachment.getContractSource())
                .name(attachment.getContractName())
                .args(String.join(",", attachment.getConstructorParams()))
                .languageName(LanguageContextFactory.JS_NAME)
                .build()
            )
            .state(ContractState.CREATED)
            .fuel(new ContractFuel(smcTransaction.getFuelLimit(), smcTransaction.getFuelPrice()))
            .txLog(createLog(contractAddress.getHex()))
            .build();

        return contract;
    }


    public static SmartContract create(SmcContractEntity smcContractEntity, SmcContractStateEntity smcContractStateEntity) {
        return SmartContract.builder()
            .address(new AplAddress(smcContractEntity.getAddress()))
            .owner(new AplAddress(smcContractEntity.getOwner()))
            .code(SmartSource.builder()
                .sourceCode(smcContractEntity.getData())
                .name(smcContractEntity.getContractName())
                .args(smcContractEntity.getArgs())
                .languageName(smcContractEntity.getLanguageName())
                .build()
            )
            .serializedObject(smcContractStateEntity.getSerializedObject())
            .state(ContractState.valueOf(smcContractStateEntity.getStatus()))
            .fuel(ContractFuel.builder()
/*
                .limit(entity.getFuelValue())
                .price(entity.getFuelPrice())
                .remaining(entity.getFuelRemaining())
*/
                    .build()
            )
            .build();
    }

    private TxLog createLog(String address) {
        return new ArrayTxLog(address, hashSumProvider);
    }

}
