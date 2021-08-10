/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractModelToStateEntityConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractStateTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractStateEntity;
import com.apollocurrency.aplwallet.apl.core.exception.AplCoreContractViolationException;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.model.smc.AplAddress;
import com.apollocurrency.aplwallet.apl.core.model.smc.SmcTxData;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.SmcContractServiceImpl;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcPublishContractAttachment;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.smc.contract.AddressNotFoundException;
import com.apollocurrency.smc.contract.ContractStatus;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.SmartSource;
import com.apollocurrency.smc.contract.fuel.ContractFuel;
import com.apollocurrency.smc.contract.fuel.Fuel;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.polyglot.LanguageContext;
import com.apollocurrency.smc.polyglot.Languages;
import com.apollocurrency.smc.polyglot.SimpleVersion;
import com.apollocurrency.smc.polyglot.lib.ContractSpec;
import com.apollocurrency.smc.polyglot.lib.LibraryProvider;
import com.apollocurrency.smc.polyglot.lib.ModuleSource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author andrew.zinchenko@gmail.com
 */
@ExtendWith(MockitoExtension.class)
class SmcContractServiceTest {
    static int HEIGHT = 100;
    static long TX_ID = 100L;

    static {
        Convert2.init("APL", 1739068987193023818L);
    }


    @Mock
    Blockchain blockchain;
    @Mock
    SmcContractTable smcContractTable;
    @Mock
    SmcContractStateTable smcContractStateTable;

    ContractModelToEntityConverter contractModelToEntityConverter = new ContractModelToEntityConverter();

    ContractModelToStateEntityConverter contractModelToStateConverter = new ContractModelToStateEntityConverter();

    @Mock
    SmcConfig smcConfig;
    @Mock
    LanguageContext languageContext;
    @Mock
    LibraryProvider libraryProvider;

    SmcPublishContractAttachment smcPublishContractAttachment;
    SmcTxData smcTxData;
    AplAddress contractAddress;
    SmartContract smartContract;
    SmcContractEntity smcContractEntity;
    SmcContractStateEntity smcContractStateEntity;
    AplAddress senderAddress;
    AplAddress recipientAddress;

    SmcContractService contractService;

    @BeforeEach
    void setUp() {
        initMocks(this);
        when(smcConfig.createLanguageContext()).thenReturn(languageContext);
        when(languageContext.getLibraryProvider()).thenReturn(libraryProvider);
        contractService = new SmcContractServiceImpl(blockchain,
            smcContractTable,
            smcContractStateTable,
            contractModelToEntityConverter,
            contractModelToStateConverter,
            smcConfig);

        smcTxData = SmcTxData.builder()
            .address("APL-632K-TWX3-2ALQ-973CU")
            .recipient("APL-632K-TWX3-2ALQ-973CU")
            .sender("APL-X5JH-TJKJ-DVGC-5T2V8")
            .name("Deal")
            .source("class Deal extends Contract {}")
            .params(List.of("123"))
            .amountATM(10_00000000L)
            .fuelLimit(20_000_000L)
            .fuelPrice(10_000L)
            .secret("1")
            .build();

        contractAddress = new AplAddress(Convert.parseAccountId(smcTxData.getAddress()));
        senderAddress = new AplAddress(Convert.parseAccountId(smcTxData.getSender()));
        recipientAddress = new AplAddress(Convert.parseAccountId(smcTxData.getRecipient()));

        smcPublishContractAttachment = SmcPublishContractAttachment.builder()
            .contractName(smcTxData.getName())
            .contractSource(smcTxData.getSource())
            .constructorParams(String.join(",", smcTxData.getParams()))
            .languageName("js")
            .fuelLimit(BigInteger.valueOf(smcTxData.getFuelLimit()))
            .fuelPrice(BigInteger.valueOf(smcTxData.getFuelPrice()))
            .build();

        smartContract = SmartContract.builder()
            .address(contractAddress)
            .owner(senderAddress)
            .sender(senderAddress)
            .txId(new AplAddress(TX_ID))
            .code(SmartSource.builder()
                .sourceCode(smcPublishContractAttachment.getContractSource())
                .name(smcPublishContractAttachment.getContractName())
                .baseContract("Contract")
                .args(smcPublishContractAttachment.getConstructorParams())
                .languageName(smcPublishContractAttachment.getLanguageName())
                .languageVersion(Languages.languageVersion(smcPublishContractAttachment.getContractSource()))
                .build()
            )
            .status(ContractStatus.CREATED)
            .fuel(new ContractFuel(contractAddress, smcPublishContractAttachment.getFuelLimit(), smcPublishContractAttachment.getFuelPrice()))
            .build();

        smcContractEntity = contractModelToEntityConverter.convert(smartContract);
        smcContractEntity.setHeight(HEIGHT); // new height value
        smcContractStateEntity = contractModelToStateConverter.convert(smartContract);
        smcContractStateEntity.setHeight(HEIGHT); // new height value
    }

    @Test
    void saveContract() {
        //GIVEN
        when(blockchain.getHeight()).thenReturn(HEIGHT);
        //WHEN
        contractService.saveContract(smartContract);

        //THEN
        verify(smcContractTable, times(1)).insert(smcContractEntity);
        verify(smcContractStateTable, times(1)).insert(smcContractStateEntity);
    }

    @Test
    void loadContract() {
        //GIVEN
        Fuel fuel = new ContractFuel(contractAddress, smcTxData.getFuelLimit(), smcTxData.getFuelPrice());
        when(smcContractTable.get(SmcContractTable.KEY_FACTORY.newKey(recipientAddress.getLongId()))).thenReturn(smcContractEntity);
        when(smcContractStateTable.get(SmcContractStateTable.KEY_FACTORY.newKey(recipientAddress.getLongId()))).thenReturn(smcContractStateEntity);

        //WHEN
        SmartContract loadContract = contractService.loadContract(contractAddress, fuel);
        //THEN
        assertEquals(smartContract.getAddress(), loadContract.getAddress());
        assertEquals(smartContract.getCode(), loadContract.getCode());
        assertEquals(smartContract, loadContract);
    }

    @Test
    void updateContractState() {
        //GIVEN
        String jsonObject = "{}";
        smartContract.updateSerializedObject(jsonObject);
        smcContractStateEntity = contractModelToStateConverter.convert(smartContract);
        smcContractStateEntity.setHeight(HEIGHT + 1); // new height value
        when(blockchain.getHeight()).thenReturn(HEIGHT + 1);
        //WHEN
        contractService.updateContractState(smartContract);
        //THEN
        verify(smcContractStateTable, times(1)).insert(smcContractStateEntity);
    }

    @Test
    void loadSerializedContract() {
        //GIVEN
        String jsonObject = "{\"value\":6789}";
        smcContractStateEntity.setSerializedObject(jsonObject);
        when(smcContractStateTable.get(SmcContractStateTable.KEY_FACTORY.newKey(recipientAddress.getLongId()))).thenReturn(smcContractStateEntity);

        //WHEN
        String serializedObject = contractService.loadSerializedContract(contractAddress);
        //THEN
        assertEquals(jsonObject, serializedObject);
    }

    @Test
    void saveSerializedContract() {
        //GIVEN
        String jsonObject = "{\"key\": 123}";
        var localStateEntity = contractModelToStateConverter.convert(smartContract);
        localStateEntity.setHeight(HEIGHT); // new height value
        localStateEntity.setSerializedObject(jsonObject);
        when(smcContractStateTable.get(SmcContractStateTable.KEY_FACTORY.newKey(((AplAddress) smartContract.getAddress()).getLongId()))).thenReturn(smcContractStateEntity);
        //WHEN
        contractService.saveSerializedContract(smartContract, jsonObject);
        //THEN
        verify(smcContractStateTable, times(1)).insert(localStateEntity);
    }

    @Test
    void createNewContract() {
        //GIVEN
        Transaction smcTransaction = mock(Transaction.class);
        when(smcTransaction.getAttachment()).thenReturn(smcPublishContractAttachment);
        when(smcTransaction.getRecipientId()).thenReturn(recipientAddress.getLongId());
        when(smcTransaction.getSenderId()).thenReturn(senderAddress.getLongId());
        when(smcTransaction.getId()).thenReturn(TX_ID);
        when(libraryProvider.parseContractType(smcPublishContractAttachment.getContractSource())).thenReturn(new ContractSpec.Item("newName", "Contract"));
        //WHEN
        SmartContract newContract = contractService.createNewContract(smcTransaction);

        //THEN
        assertEquals(smartContract, newContract);
    }

    @Test
    void getContractDetails() {
        //GIVEN
        Transaction smcTransaction = mock(Transaction.class);
        when(smcTransaction.getAttachment()).thenReturn(smcPublishContractAttachment);
        when(smcTransaction.getRecipientId()).thenReturn(recipientAddress.getLongId());
        when(smcTransaction.getSignature()).thenReturn(mock(Signature.class));
        when(smcTransaction.getBlockTimestamp()).thenReturn(1234567890);
        when(smcContractTable.get(SmcContractTable.KEY_FACTORY.newKey(recipientAddress.getLongId()))).thenReturn(smcContractEntity);
        when(smcContractStateTable.get(SmcContractStateTable.KEY_FACTORY.newKey(recipientAddress.getLongId()))).thenReturn(smcContractStateEntity);
        when(blockchain.getTransaction(TX_ID)).thenReturn(smcTransaction);
        //WHEN
        var response = contractService.getContractDetailsByTransaction(new AplAddress(TX_ID));

        //THEN
        assertEquals(convertToRS(smartContract.getAddress()), response.getAddress());
        assertEquals(smartContract.getFuel().limit().toString(), response.getFuelLimit());
        assertEquals(smartContract.getFuel().price().toString(), response.getFuelPrice());
    }

    @Test
    void loadMyContracts() {
        //GIVEN
        when(smcContractTable.getManyBy(any(), any(int.class), any(int.class)))
            .thenReturn(CollectionUtil.toDbIterator(List.of(smcContractEntity)));
        Transaction smcTransaction = mock(Transaction.class);
        when(smcTransaction.getAttachment()).thenReturn(smcPublishContractAttachment);
        when(smcTransaction.getRecipientId()).thenReturn(recipientAddress.getLongId());
        when(smcTransaction.getSignature()).thenReturn(mock(Signature.class));
        when(smcTransaction.getBlockTimestamp()).thenReturn(1234567890);
        when(smcContractTable.get(SmcContractTable.KEY_FACTORY.newKey(recipientAddress.getLongId()))).thenReturn(smcContractEntity);
        when(smcContractStateTable.get(SmcContractStateTable.KEY_FACTORY.newKey(recipientAddress.getLongId()))).thenReturn(smcContractStateEntity);
        when(blockchain.getTransaction(TX_ID)).thenReturn(smcTransaction);

        //WHEN
        var loadedContracts = contractService.loadContractsByOwner(senderAddress, 0, Integer.MAX_VALUE);
        //THEN
        assertEquals(1, loadedContracts.size());
        assertEquals(convertToRS(smartContract.getAddress()), loadedContracts.get(0).getAddress());
        assertEquals(convertToString(smartContract.getTxId()), loadedContracts.get(0).getTransaction());
    }

    @SneakyThrows
    @Test
    void loadSpecByModuleName() {
        //GIVEN
        var language = "js";
        var version = SimpleVersion.fromString("0.1.1");
        var moduleName = "APL20";
        var module = mock(ContractSpec.class);
        when(libraryProvider.isCompatible(language, version)).thenReturn(true);
        when(libraryProvider.loadSpecification(moduleName)).thenReturn(module);
        when(libraryProvider.importModule(moduleName)).thenReturn(mock(ModuleSource.class));

        //WHEN
        var rc = contractService.loadAsrModuleSpec(moduleName, language, version);
        //THEN
        assertNotNull(rc);
    }

    @Test
    void loadSpecByModuleNameWithException() {
        //GIVEN
        var language = "js";
        var version = SimpleVersion.fromString("0.1.1");
        var moduleName = "APL20";
        when(libraryProvider.getLanguageName()).thenReturn(language);
        when(libraryProvider.version()).thenReturn(version);

        //WHEN
        //THEN
        assertThrows(AplCoreContractViolationException.class, () -> contractService.loadAsrModuleSpec(moduleName, "java", version));

    }

    @SneakyThrows
    @Test
    void loadSpecByAddress() {
        //GIVEN
        var language = "js";
        var version = SimpleVersion.fromString("0.1.1");
        var address = new AplAddress(123L);
        var moduleName = "APL20";
        var module = mock(ContractSpec.class);
        when(libraryProvider.isCompatible(language, version)).thenReturn(true);
        when(libraryProvider.loadSpecification(moduleName)).thenReturn(module);
        when(libraryProvider.importModule(moduleName)).thenReturn(mock(ModuleSource.class));
        smcContractEntity.setBaseContract(moduleName);
        when(smcContractTable.get(SmcContractTable.KEY_FACTORY.newKey(address.getLongId()))).thenReturn(smcContractEntity);
        //WHEN
        var rc = contractService.loadAsrModuleSpec(address);
        //THEN
        assertNotNull(rc);
    }

    @Test
    void loadSpecByAddressThrowException() {
        //GIVEN
        var address = new AplAddress(123L);
        //WHEN
        //THEN
        assertThrows(AddressNotFoundException.class, () -> contractService.loadAsrModuleSpec(address));
    }

    static String convertToString(Address address) {
        return Long.toUnsignedString(new AplAddress(address).getLongId());
    }

    static String convertToRS(Address address) {
        return Convert2.rsAccount(new AplAddress(address).getLongId());
    }

}
