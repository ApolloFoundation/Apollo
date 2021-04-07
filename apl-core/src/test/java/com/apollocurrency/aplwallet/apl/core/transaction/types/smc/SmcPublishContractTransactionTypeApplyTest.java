/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.smc;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractModelToStateEntityConverter;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractStateEntity;
import com.apollocurrency.aplwallet.apl.core.model.smc.AplAddress;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcCallMethodAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcPublishContractAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.fuel.ContractFuel;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.inject.Inject;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * @author andrew.zinchenko@gmail.com
 */
//@Tag("slow")
@EnableWeld
@ExtendWith(MockitoExtension.class)
class SmcPublishContractTransactionTypeApplyTest extends AbstractSmcTransactionTypeApplyTest {


    @Inject
    ContractModelToEntityConverter contractModelToEntityConverter;
    @Inject
    ContractModelToStateEntityConverter contractModelToStateEntityConverter;

    @Test
    void publishSmcApplyAttachment() throws AplException.NotValidException {
        //GIVEN
        TxData txData = TxData.builder()
            .sender("APL-X5JH-TJKJ-DVGC-5T2V8")
            .name("Deal")
            .source("class Deal {}")
            .params(List.of("123"))
            .amountATM(10L)
            .fuelLimit(5000L)
            .fuelPrice(100L)
            .secret("1")
            .build();

        SmcPublishContractAttachment attachment = SmcPublishContractAttachment.builder()
            .contractName(txData.getName())
            .contractSource(txData.getSource())
            .constructorParams(String.join(",", txData.getParams()))
            .languageName("javascript")
            .fuelLimit(BigInteger.valueOf(txData.getFuelLimit()))
            .fuelPrice(BigInteger.valueOf(txData.getFuelPrice()))
            .build();

        long senderAccountId = Convert.parseAccountId(txData.getSender());
        Account account = new Account(senderAccountId, 1000000000L, 1000000000L, 1000000000L, 0L, 10);

        byte[] recipientPublicKey = AccountService.generatePublicKey(account, attachment.getContractSource());
        long recipientId = AccountService.getId(recipientPublicKey);

        Transaction newTx = createTransaction(txData, attachment, account, recipientPublicKey, recipientId);
        assertNotNull(newTx);

        doNothing().when(spyAccountService).addToBalanceATM(any(Account.class), any(LedgerEvent.class), eq(newTx.getId()), eq(-txData.getAmountATM()), eq(-(txData.getFuelLimit() * txData.getFuelPrice())));
        doNothing().when(spyAccountService).addToBalanceAndUnconfirmedBalanceATM(any(Account.class), any(LedgerEvent.class), eq(newTx.getId()), eq(txData.getAmountATM()));
        long senderId = AccountService.getId(newTx.getSenderPublicKey());
        when(publicKeyDao.searchAll(senderId)).thenReturn(new PublicKey(senderId, newTx.getSenderPublicKey(), newTx.getHeight()));

        //WHEN
        DbUtils.inTransaction(extension, connection -> txApplier.apply(newTx));
        SmartContract smartContract = contractService.loadContract(
            new AplAddress(newTx.getRecipientId()),
            new ContractFuel(attachment.getFuelLimit(), attachment.getFuelPrice())
        );

        //THEN
        assertNotNull(smartContract);
        assertEquals(new AplAddress(newTx.getId()).getHex(), smartContract.getTxId().getHex());
    }

    @Test
    void callSmcApplyAttachment() throws AplException.NotValidException {
        //GIVEN

        String aa = "class Deal extends Contract{\n" +
            "  constructor( value, vendor ){\n" +
            "    console.log(\"--- in constructor ---\", value, vendor);\n" +
            "    super();\n" +
            "    this.value = value;\n" +
            "    this.vendor = vendor;\n" +
            "    this.customer = '';\n" +
            "    this.paid = false;\n" +
            "    this.accepted = false;\n" +
            "  }\n" +
            "  pay() {\n" +
            "      console.log(\"--- in pay msg.getValue()---\", msg.getValue(), typeof msg.getValue());\n" +
            "      console.log(\"--- in pay this.value ---\", this.value, typeof this.value);\n" +
            "      if ( this.value <= msg.getValue() ){\n" +
            "         super.transfer( msg.getValue() );\n" +
            "         this.paid = true;\n" +
            "         this.customer = msg.getSender();\n" +
            "      }\n" +
            "  }\n" +
            "  trace() {\n" +
            "      console.log(\"--- in trace msg.getValue()---\", msg.getValue(), typeof msg.getValue());\n" +
            "      console.log(\"--- in trace this.value ---\", this.value, typeof this.value);\n" +
            "  }\n" +
            "  accept(){\n" +
            "    console.log(\"--- in accept msg=\", msg.getValue(), \" \", msg.getSender());\n" +
            "    console.log(\"--- in accept accepted=\", this.accepted);\n" +
            "    console.log(\"--- in accept paid=\", this.paid);\n" +
            "    console.log(\"--- in accept customer=\", this.customer);\n" +
            "    if (!this.accepted && this.paid && this.customer == msg.getSender()){\n" +
            "      super.send(this.value, this.vendor)\n" +
            "      this.accepted = true \n" +
            "    }\n" +
            "  }\n" +
            "}";


        TxData txData = TxData.builder()
            .sender("APL-X5JH-TJKJ-DVGC-5T2V8")
            .name("Deal")
            .source("class Deal extends Contract{\n" +
                "  constructor( value, vendor ){\n" +
                "    console.log('--- in constructor ---', value, vendor);\n" +
                "    super();\n" +
                "    this.value = value;\n" +
                "    this.vendor = vendor;\n" +
                "    this.customer = '';\n" +
                "    this.paid = false;\n" +
                "    this.accepted = false;\n" +
                "  }\n" +
                "  pay() {\n" +
                "      console.log('--- in pay msg.getValue()---', msg.getValue(), typeof msg.getValue());\n" +
                "      console.log('--- in pay this.value ---', this.value, typeof this.value);\n" +
                "      if ( this.value <= msg.getValue() ){\n" +
                "         super.transfer( msg.getValue() );\n" +
                "         this.paid = true;\n" +
                "         this.customer = msg.getSender();\n" +
                "      }\n" +
                "  }\n" +
                "  trace() {\n" +
                "      console.log('--- in trace msg.getValue()---', msg.getValue(), typeof msg.getValue());\n" +
                "      console.log('--- in trace this.value ---', this.value, typeof this.value);\n" +
                "  }\n" +
                "  accept(){\n" +
                "    console.log('--- in accept msg=', msg.getValue(), ' ', msg.getSender());\n" +
                "    console.log('--- in accept accepted=', this.accepted);\n" +
                "    console.log('--- in accept paid=', this.paid);\n" +
                "    console.log('--- in accept customer=', this.customer);\n" +
                "    if (!this.accepted && this.paid && this.customer == msg.getSender()){\n" +
                "      super.send(this.value, this.vendor)\n" +
                "      this.accepted = true \n" +
                "    }\n" +
                "  }\n" +
                "}")
            .params(List.of("1400000000", "\"APL-X5JH-TJKJ-DVGC-5T2V8\""))
            .amountATM(10L)
            .fuelLimit(5000L)
            .fuelPrice(100L)
            .secret("1")
            .build();

        SmcPublishContractAttachment attachment = SmcPublishContractAttachment.builder()
            .contractName(txData.getName())
            .contractSource(txData.getSource())
            .constructorParams(String.join(",", txData.getParams()))
            .languageName("javascript")
            .fuelLimit(BigInteger.valueOf(txData.getFuelLimit()))
            .fuelPrice(BigInteger.valueOf(txData.getFuelPrice()))
            .build();

        long senderAccountId = Convert.parseAccountId(txData.getSender());
        Account account = new Account(senderAccountId, 1000000000L, 1000000000L, 1000000000L, 0L, 1);

        byte[] recipientPublicKey = AccountService.generatePublicKey(account, attachment.getContractSource());
        long recipientId = AccountService.getId(recipientPublicKey);

        Transaction newTx = createTransaction(txData, attachment, account, recipientPublicKey, recipientId);
        assertNotNull(newTx);

        doNothing().when(spyAccountService).addToBalanceATM(any(Account.class), any(LedgerEvent.class), eq(newTx.getId()), eq(-txData.getAmountATM()), eq(-(txData.getFuelLimit() * txData.getFuelPrice())));
        doNothing().when(spyAccountService).addToBalanceAndUnconfirmedBalanceATM(any(Account.class), any(LedgerEvent.class), eq(newTx.getId()), eq(txData.getAmountATM()));
        long senderId = AccountService.getId(newTx.getSenderPublicKey());
        when(publicKeyDao.searchAll(senderId)).thenReturn(new PublicKey(senderId, newTx.getSenderPublicKey(), newTx.getHeight()));

        //WHEN
        DbUtils.inTransaction(extension, connection -> txApplier.apply(newTx));
        SmartContract smartContract = contractService.loadContract(
            new AplAddress(newTx.getRecipientId()),
            new ContractFuel(attachment.getFuelLimit(), attachment.getFuelPrice())
        );

        SmcContractEntity contractEntity = contractModelToEntityConverter.convert(smartContract);
        SmcContractStateEntity contractStateEntity = contractModelToStateEntityConverter.convert(smartContract);


        //THEN
        assertNotNull(smartContract);
        assertEquals(new AplAddress(newTx.getId()).getHex(), smartContract.getTxId().getHex());

        //GIVEN
        TxData txData2 = TxData.builder()
            .sender("APL-LTR8-GMHB-YG56-4NWSE")
            .recipient(Convert.defaultRsAccount(newTx.getRecipientId()))
            .recipientPublicKey(Convert.toHexString(recipientPublicKey))
            .method("trace")
            .params(Collections.emptyList())
            .amountATM(1400000000L)
            .fuelLimit(5000L)
            .fuelPrice(100L)
            .secret("2")
            .build();

        SmcCallMethodAttachment attachment2 = SmcCallMethodAttachment.builder()
            .methodName(txData2.getMethod())
            .methodParams("")
            .fuelLimit(BigInteger.valueOf(txData2.getFuelLimit()))
            .fuelPrice(BigInteger.valueOf(txData2.getFuelPrice()))
            .build();

        long senderAccountId2 = Convert.parseAccountId(txData2.getSender());
        Account account2 = new Account(senderAccountId2, 1000000000000000L, 1000000000000000L, 1000000000L, 0L, 1);

        Transaction newTx2 = createTransaction(txData2, attachment2, account2, Convert.parseHexString(txData2.getRecipientPublicKey()), Convert.parseAccountId(txData2.getRecipient()));
        assertNotNull(newTx);

        doNothing().when(spyAccountService).addToBalanceATM(any(Account.class), any(LedgerEvent.class), eq(newTx2.getId()), eq(-txData2.getAmountATM()), eq(-(txData2.getFuelLimit() * txData2.getFuelPrice())));
        doNothing().when(spyAccountService).addToBalanceAndUnconfirmedBalanceATM(any(Account.class), any(LedgerEvent.class), eq(newTx2.getId()), eq(txData2.getAmountATM()));
        long senderId2 = AccountService.getId(newTx2.getSenderPublicKey());
        when(publicKeyDao.searchAll(senderId2)).thenReturn(new PublicKey(senderId2, newTx2.getSenderPublicKey(), newTx2.getHeight()));

        //WHEN
        DbUtils.inTransaction(extension, connection -> txApplier.apply(newTx2));
        SmartContract smartContract2 = contractService.loadContract(
            new AplAddress(newTx2.getRecipientId()),
            new ContractFuel(attachment.getFuelLimit(), attachment.getFuelPrice())
        );

        //THEN
        assertNotNull(smartContract2);
        assertEquals(smartContract.getTxId().getHex(), smartContract2.getTxId().getHex());
    }

}
