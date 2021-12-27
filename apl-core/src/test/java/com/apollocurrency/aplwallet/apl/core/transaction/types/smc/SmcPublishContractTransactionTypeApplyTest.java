/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.smc;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.model.smc.SmcTxData;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractToolService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcCallMethodAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcPublishContractAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.smc.model.AplAddress;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.fuel.ContractFuel;
import com.apollocurrency.smc.polyglot.language.lib.JSLibraryProvider;
import com.apollocurrency.smc.util.Utils;
import lombok.SneakyThrows;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.inject.Inject;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Tag("slow")
@EnableWeld
@ExtendWith(MockitoExtension.class)
class SmcPublishContractTransactionTypeApplyTest extends AbstractSmcTransactionTypeApplyTest {

    @Inject
    ContractToolService toolService;

    @Test
    void publishSmcApplyAttachment() throws AplException.NotValidException {
        //GIVEN
        SmcTxData txData = SmcTxData.builder()
            .sender("APL-X5JH-TJKJ-DVGC-5T2V8")
            .name("TestC")
            .source("class Contract {}; class TestC extends Contract {}")
            .baseContract("Contract")
            .params(List.of("123"))
            .amountATM(10_00000000L)
            .fuelLimit(50_000_000L)
            .fuelPrice(100L)
            .secret("1")
            .build();

        SmcPublishContractAttachment attachment = SmcPublishContractAttachment.builder()
            .contractName(txData.getName())
            .baseContract(txData.getBaseContract())
            .contractSource(txData.getSource())
            .constructorParams(String.join(",", txData.getParams()))
            .languageName("js")
            .languageVersion(JSLibraryProvider.LIBRARY_VERSION.toString())
            .fuelLimit(BigInteger.valueOf(txData.getFuelLimit()))
            .fuelPrice(BigInteger.valueOf(txData.getFuelPrice()))
            .build();

        long senderAccountId = Convert.parseAccountId(txData.getSender());
        Account account = new Account(senderAccountId, 100_000_00000000L, 100_000_00000000L, 100_000_00000000L, 0L, 10);

        Transaction newTx = createTransaction(txData, attachment, account);
        assertNotNull(newTx);
        newTx.setBlock(lastBlock);

        doNothing().when(spyAccountService).addToBalanceATM(any(Account.class), any(LedgerEvent.class), eq(newTx.getId()), eq(-txData.getAmountATM()), eq(-(txData.getFuelLimit() * txData.getFuelPrice())));

        long senderId = AccountService.getId(newTx.getSenderPublicKey());
        when(publicKeyDao.searchAll(senderId)).thenReturn(new PublicKey(senderId, newTx.getSenderPublicKey(), newTx.getHeight()));

        //WHEN
        DbUtils.inTransaction(extension, connection -> txApplier.apply(newTx));
        AplAddress contractAddress = new AplAddress(newTx.getId());

        SmartContract smartContract = contractRepository.loadContract(
            contractAddress,
            new AplAddress(newTx.getSenderId()),
            new AplAddress(newTx.getSenderId()),
            new ContractFuel(contractAddress, attachment.getFuelLimit(), attachment.getFuelPrice())
        );

        //THEN
        assertNotNull(smartContract);
        assertEquals(new AplAddress(newTx.getId()).getHex(), smartContract.getTxId().getHex());
    }

    @Test
    void publishSmcApplyAttachmentWithJSException() throws AplException.NotValidException {
        //GIVEN
        SmcTxData txData = SmcTxData.builder()
            .sender("APL-X5JH-TJKJ-DVGC-5T2V8")
            .name("TestC")
            .source("class TestC extends Contract2 {}")
            .baseContract("Contract")
            .params(List.of("123"))
            .amountATM(10_00000000L)
            .fuelLimit(50_000_000L)
            .fuelPrice(100L)
            .secret("1")
            .build();

        SmcPublishContractAttachment attachment = SmcPublishContractAttachment.builder()
            .contractName(txData.getName())
            .baseContract(txData.getBaseContract())
            .contractSource(txData.getSource())
            .constructorParams(String.join(",", txData.getParams()))
            .languageName("js")
            .languageVersion(JSLibraryProvider.LIBRARY_VERSION.toString())
            .fuelLimit(BigInteger.valueOf(txData.getFuelLimit()))
            .fuelPrice(BigInteger.valueOf(txData.getFuelPrice()))
            .build();

        var spyApplier = spy(txApplier);

        long senderAccountId = Convert.parseAccountId(txData.getSender());
        Account account = new Account(senderAccountId, 100_000_00000000L, 100_000_00000000L, 100_000_00000000L, 0L, 10);

        Transaction newTx = createTransaction(txData, attachment, account);
        assertNotNull(newTx);
        newTx.setBlock(lastBlock);

        doNothing().when(spyAccountService).addToBalanceATM(any(Account.class), any(LedgerEvent.class), eq(newTx.getId()), eq(-txData.getAmountATM()), eq(-(txData.getFuelLimit() * txData.getFuelPrice())));
        long senderId = AccountService.getId(newTx.getSenderPublicKey());
        when(publicKeyDao.searchAll(senderId)).thenReturn(new PublicKey(senderId, newTx.getSenderPublicKey(), newTx.getHeight()));
        when(blockchainConfig.isFailedTransactionsAcceptanceActiveAtHeight(lastBlock.getHeight())).thenReturn(true);

        //WHEN
        DbUtils.inTransaction(extension, connection -> spyApplier.apply(newTx));
        //THEN
        assertTrue(newTx.getErrorMessage().isPresent());
        assertTrue(newTx.getErrorMessage().get().contains("errorMessage=JSException:ReferenceError: Contract2 is not defined"));

    }

    @SneakyThrows
    @Test
    void callSmcApplyAttachmentInThreeTransaction() throws AplException.NotValidException {
        //GIVEN
        String contractSource = Utils.readResourceContent("address_mapping_2-contract.js");
        String senderAccount2RS = "APL-LTR8-GMHB-YG56-4NWSE";
        long senderAccountId2 = Convert.parseAccountId(senderAccount2RS);

        var smartSource = toolService.completeContractSource(
            toolService.createSmartSource("AddressMappingContract", contractSource, "js")
        );
        //when(blockchainConfig.isFailedTransactionsAcceptanceActiveAtHeight(lastBlock.getHeight())).thenReturn(true);

        SmcTxData txData1 = SmcTxData.builder()
            .sender("APL-X5JH-TJKJ-DVGC-5T2V8")
            .name(smartSource.getName())
            .source(smartSource.getSourceCode())
            .baseContract(smartSource.getBaseContract())
            .params(List.of("1400000000", "\"" + new AplAddress(senderAccountId2).getHex() + "\""))
            .amountATM(10_00000000L)
            .fuelLimit(60_000_000L)
            .fuelPrice(100L)
            .secret("1")
            .build();

        SmcPublishContractAttachment attachment = SmcPublishContractAttachment.builder()
            .contractName(txData1.getName())
            .contractSource(smartSource.getSourceCode())
            .baseContract(smartSource.getBaseContract())
            .languageName(smartSource.getLanguageName())
            .languageVersion(smartSource.getLanguageVersion().toString())
            .constructorParams(String.join(",", txData1.getParams()))
            .fuelLimit(BigInteger.valueOf(txData1.getFuelLimit()))
            .fuelPrice(BigInteger.valueOf(txData1.getFuelPrice()))
            .build();

        long senderAccountId = Convert.parseAccountId(txData1.getSender());
        Account account = new Account(senderAccountId, 100_000_00000000L, 100_000_00000000L, 100_000_00000000L, 0L, 10);

        Transaction newTx = createTransaction(txData1, attachment, account);
        assertNotNull(newTx);
        newTx.setBlock(lastBlock);

        doNothing().when(spyAccountService).addToBalanceATM(any(Account.class), any(LedgerEvent.class), eq(newTx.getId()), eq(-txData1.getAmountATM()), eq(-(txData1.getFuelLimit() * txData1.getFuelPrice())));
        long senderId = AccountService.getId(newTx.getSenderPublicKey());
        when(publicKeyDao.searchAll(senderId)).thenReturn(new PublicKey(senderId, newTx.getSenderPublicKey(), newTx.getHeight()));

        //WHEN
        DbUtils.inTransaction(extension, connection -> txApplier.apply(newTx));
        AplAddress contractAddress = new AplAddress(newTx.getId());
        SmartContract smartContract = contractRepository.loadContract(
            contractAddress,
            new AplAddress(newTx.getSenderId()),
            new AplAddress(newTx.getSenderId()),
            new ContractFuel(contractAddress, attachment.getFuelLimit(), attachment.getFuelPrice())
        );

        //THEN
        assertNotNull(smartContract);
        assertEquals(new AplAddress(newTx.getId()).getHex(), smartContract.getTxId().getHex());

        //GIVEN
        SmcTxData txData2 = SmcTxData.builder()
            .sender(senderAccount2RS)
            .method("set")
            .params(Collections.emptyList())
            .amountATM(0L)
            .fuelLimit(15_000_000L)
            .fuelPrice(100L)
            .secret("2")
            .build();

        SmcCallMethodAttachment attachment2 = SmcCallMethodAttachment.builder()
            .methodName(txData2.getMethod())
            .methodParams(String.join(",", txData2.getParams()))
            .fuelLimit(BigInteger.valueOf(txData2.getFuelLimit()))
            .fuelPrice(BigInteger.valueOf(txData2.getFuelPrice()))
            .build();

        Account account2 = new Account(senderAccountId2, 10_000_000_00000000L, 10_000_000_00000000L, 1000000000L, 0L, 1);

        Transaction newTx2 = createTransaction(txData2, attachment2, account2, contractAddress.getLongId());
        assertNotNull(newTx2);
        newTx2.setBlock(lastBlock);

        doNothing().when(spyAccountService).addToBalanceATM(any(Account.class), any(LedgerEvent.class), eq(newTx2.getId()), eq(-txData2.getAmountATM()), eq(-(txData2.getFuelLimit() * txData2.getFuelPrice())));
        doNothing().when(spyAccountService).addToBalanceAndUnconfirmedBalanceATM(any(Account.class), any(LedgerEvent.class), eq(newTx2.getId()), eq(txData2.getAmountATM()));
        long senderId2 = AccountService.getId(newTx2.getSenderPublicKey());
        when(publicKeyDao.searchAll(senderId2)).thenReturn(new PublicKey(senderId2, newTx2.getSenderPublicKey(), newTx2.getHeight()));

        //WHEN
        DbUtils.inTransaction(extension, connection -> txApplier.apply(newTx2));
        AplAddress contractAddress2 = new AplAddress(newTx2.getRecipientId());
        SmartContract smartContract2 = contractRepository.loadContract(
            contractAddress,
            contractAddress2,
            contractAddress2,
            new ContractFuel(contractAddress2, attachment2.getFuelLimit(), attachment2.getFuelPrice())
        );

        //THEN
        assertNotNull(smartContract2);
        assertEquals(smartContract.getTxId().getHex(), smartContract2.getTxId().getHex());

        //GIVEN
        SmcTxData txData3 = SmcTxData.builder()
            .sender(senderAccount2RS)
            .method("read")
            .params(Collections.emptyList())
            .fuelLimit(15_000_000L)
            .fuelPrice(100L)
            .secret("2")
            .build();

        SmcCallMethodAttachment attachment3 = SmcCallMethodAttachment.builder()
            .methodName(txData3.getMethod())
            .methodParams(String.join(",", txData3.getParams()))
            .fuelLimit(BigInteger.valueOf(txData3.getFuelLimit()))
            .fuelPrice(BigInteger.valueOf(txData3.getFuelPrice()))
            .build();

        long senderAccountId3 = Convert.parseAccountId(txData3.getSender());
        Account account3 = new Account(senderAccountId3, 10_000_000_00000000L, 10_000_000_00000000L, 1000000000L, 0L, 1);

        Transaction newTx3 = createTransaction(txData3, attachment3, account3, contractAddress.getLongId());
        assertNotNull(newTx3);
        newTx3.setBlock(lastBlock);

        doNothing().when(spyAccountService).addToBalanceATM(any(Account.class), any(LedgerEvent.class), eq(newTx3.getId()), eq(-txData3.getAmountATM()), eq(-(txData3.getFuelLimit() * txData3.getFuelPrice())));
        doNothing().when(spyAccountService).addToBalanceAndUnconfirmedBalanceATM(any(Account.class), any(LedgerEvent.class), eq(newTx3.getId()), eq(txData3.getAmountATM()));
        long senderId3 = AccountService.getId(newTx3.getSenderPublicKey());
        when(publicKeyDao.searchAll(senderId3)).thenReturn(new PublicKey(senderId3, newTx3.getSenderPublicKey(), newTx3.getHeight()));

        //WHEN
        DbUtils.inTransaction(extension, connection -> txApplier.apply(newTx3));
        AplAddress contractAddress3 = new AplAddress(newTx3.getId());
        SmartContract smartContract3 = contractRepository.loadContract(
            contractAddress,
            contractAddress3,
            contractAddress3,
            new ContractFuel(contractAddress3, attachment3.getFuelLimit(), attachment3.getFuelPrice())
        );

        //THEN
        assertNotNull(smartContract3);
        assertEquals(smartContract.getTxId().getHex(), smartContract3.getTxId().getHex());
    }

}
