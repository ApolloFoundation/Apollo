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
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.model.smc.SmcTxData;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.SmcContractRepositoryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.SmcContractToolServiceImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcPublishContractAttachment;
import com.apollocurrency.aplwallet.apl.smc.model.AplAddress;
import com.apollocurrency.aplwallet.apl.smc.model.AplContractSpec;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.smc.contract.AddressNotFoundException;
import com.apollocurrency.smc.contract.ContractSource;
import com.apollocurrency.smc.contract.ContractStatus;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.fuel.ContractFuel;
import com.apollocurrency.smc.contract.fuel.Fuel;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.polyglot.SimpleVersion;
import com.apollocurrency.smc.polyglot.language.LanguageContext;
import com.apollocurrency.smc.polyglot.language.LanguageContextFactory;
import com.apollocurrency.smc.polyglot.language.SmartSource;
import com.apollocurrency.smc.polyglot.language.lib.Preprocessor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author andrew.zinchenko@gmail.com
 */
@ExtendWith(MockitoExtension.class)
class SmcContractRepositoryTest {
    static int HEIGHT = 100;
    static long TX_ID = 100L;
    static final byte[] TRANSACTION_HASH = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

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
    Preprocessor preprocessor;
    @Mock
    SmcContractService contractService;

    SmcPublishContractAttachment smcPublishContractAttachment;
    SmcTxData smcTxData;
    SmartContract smartContract;
    SmcContractEntity smcContractEntity;
    SmcContractStateEntity smcContractStateEntity;

    SmcContractRepository contractRepository;
    ContractToolService contractToolService;
    SmartSource smartSource;

    @BeforeEach
    void setUp() {
        when(smcConfig.createLanguageContext()).thenReturn(languageContext);
        //when(languageContext.getLibraryProvider()).thenReturn(libraryProvider);
        when(languageContext.getPreprocessor()).thenReturn(preprocessor);
        contractRepository = new SmcContractRepositoryImpl(blockchain,
            smcContractTable,
            smcContractStateTable,
            contractModelToEntityConverter,
            contractModelToStateConverter,
            smcConfig,
            contractService);
        contractToolService = new SmcContractToolServiceImpl(blockchain, smcConfig);

        smcTxData = SmcTxData.builder()
            .address("APL-632K-TWX3-2ALQ-973CU")
            .recipient("APL-632K-TWX3-2ALQ-973CU")
            .sender("APL-X5JH-TJKJ-DVGC-5T2V8")
            .name("Deal")
            .baseContract("Contract")
            .source("class Deal extends Contract {}")
            .params(List.of("123"))
            .amountATM(10_00000000L)
            .fuelLimit(20_000_000L)
            .fuelPrice(10_000L)
            .secret("1")
            .build();

        smcPublishContractAttachment = SmcPublishContractAttachment.builder()
            .contractName(smcTxData.getName())
            .baseContract(smcTxData.getBaseContract())
            .contractSource(smcTxData.getSource())
            .constructorParams(String.join(",", smcTxData.getParams()))
            .languageName("js")
            .languageVersion(LanguageContextFactory.LANGUAGE_VERSION.toString())
            .fuelLimit(BigInteger.valueOf(smcTxData.getFuelLimit()))
            .fuelPrice(BigInteger.valueOf(smcTxData.getFuelPrice()))
            .build();

        smartSource = ContractSource.builder()
            .name(smcPublishContractAttachment.getContractName())
            .baseContract(smcPublishContractAttachment.getBaseContract())
            .sourceCode(smcPublishContractAttachment.getContractSource())
            .languageName(smcPublishContractAttachment.getLanguageName())
            .languageVersion(SimpleVersion.fromString(smcPublishContractAttachment.getLanguageVersion()))
            .build();

        smartContract = SmartContract.builder()
            .address(smcTxData.getContractAddress())
            .owner(smcTxData.getSenderAddress())
            .originator(smcTxData.getSenderAddress())
            .caller(smcTxData.getSenderAddress())
            .txId(new AplAddress(TX_ID))
            .args(smcPublishContractAttachment.getConstructorParams())
            .code(smartSource)
            .status(ContractStatus.CREATED)
            .fuel(new ContractFuel(smcTxData.getSenderAddress(), smcPublishContractAttachment.getFuelLimit(), smcPublishContractAttachment.getFuelPrice()))
            .build();

        smcContractEntity = contractModelToEntityConverter.convert(smartContract);
        smcContractEntity.setHeight(HEIGHT); // new height value
        smcContractEntity.setTransactionHash(TRANSACTION_HASH);
        smcContractStateEntity = contractModelToStateConverter.convert(smartContract);
        smcContractStateEntity.setHeight(HEIGHT); // new height value
    }

    @Test
    void saveContract() {
        //GIVEN
        when(blockchain.getHeight()).thenReturn(HEIGHT);
        //WHEN
        contractRepository.saveContract(smartContract, TX_ID, TRANSACTION_HASH);

        //THEN
        verify(smcContractTable, times(1)).insert(smcContractEntity);
        verify(smcContractStateTable, times(1)).insert(smcContractStateEntity);
    }

    @Test
    void loadContract() {
        //GIVEN
        Fuel fuel = new ContractFuel(smcTxData.getSenderAddress(), smcTxData.getFuelLimit(), smcTxData.getFuelPrice());
        when(smcContractTable.get(SmcContractTable.KEY_FACTORY.newKey(smcTxData.getRecipientAddress().getLongId()))).thenReturn(smcContractEntity);
        when(smcContractStateTable.get(SmcContractStateTable.KEY_FACTORY.newKey(smcTxData.getRecipientAddress().getLongId()))).thenReturn(smcContractStateEntity);

        //WHEN
        SmartContract loaded = contractRepository.loadContract(smcTxData.getContractAddress(), smcTxData.getSenderAddress(), fuel);
        //THEN
        assertEquals(smartContract.getAddress(), loaded.getAddress());
        assertEquals(smartContract.getCode(), loaded.getCode());
        assertEquals(smartContract, loaded);
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
        contractRepository.updateContractState(smartContract);
        //THEN
        verify(smcContractStateTable, times(1)).insert(smcContractStateEntity);
    }

    @Test
    void loadSerializedContract() {
        //GIVEN
        String jsonObject = "{\"value\":6789}";
        smcContractStateEntity.setSerializedObject(jsonObject);
        when(smcContractStateTable.get(SmcContractStateTable.KEY_FACTORY.newKey(smcTxData.getRecipientAddress().getLongId()))).thenReturn(smcContractStateEntity);

        //WHEN
        String serializedObject = contractRepository.loadSerializedContract(smcTxData.getContractAddress());
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
        contractRepository.saveSerializedContract(smartContract, jsonObject);
        //THEN
        verify(smcContractStateTable, times(1)).insert(localStateEntity);
    }

    @Test
    void createNewContract() {
        //GIVEN
        Transaction smcTransaction = mock(Transaction.class);
        when(smcTransaction.getAttachment()).thenReturn(smcPublishContractAttachment);
        when(smcTransaction.getRecipientId()).thenReturn(smcTxData.getRecipientAddress().getLongId());
        when(smcTransaction.getSenderId()).thenReturn(smcTxData.getSenderAddress().getLongId());
        when(smcTransaction.getId()).thenReturn(TX_ID);
        //WHEN
        SmartContract newContract = contractToolService.createNewContract(smcTransaction);

        //THEN
        assertEquals(smartContract, newContract);
    }

    @SneakyThrows
    @Test
    void loadSpecByAddress() {
        //GIVEN
        var address = new AplAddress(123L);
        var module = mock(AplContractSpec.class);
        var entity = spy(smcContractEntity);
        when(entity.getBaseContract()).thenReturn("APL20");
        when(smcContractTable.get(SmcContractTable.KEY_FACTORY.newKey(address.getLongId()))).thenReturn(entity);
        when(contractService.loadAsrModuleSpec(entity.getBaseContract(),
            entity.getLanguageName(),
            SimpleVersion.fromString(entity.getLanguageVersion()))).thenReturn(module);
        //WHEN
        var rc = contractRepository.loadAsrModuleSpec(address);
        //THEN
        assertEquals(module, rc);
    }

    @Test
    void loadSpecByAddressThrowException() {
        //GIVEN
        var address = new AplAddress(123L);
        //WHEN
        //THEN
        assertThrows(AddressNotFoundException.class, () -> contractRepository.loadAsrModuleSpec(address));
    }

    static String convertToString(Address address) {
        return Long.toUnsignedString(new AplAddress(address).getLongId());
    }

    static String convertToRS(Address address) {
        return Convert2.rsAccount(new AplAddress(address).getLongId());
    }

}
