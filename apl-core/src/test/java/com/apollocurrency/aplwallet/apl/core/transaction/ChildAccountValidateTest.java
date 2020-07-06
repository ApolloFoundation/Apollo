/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.EcBlockData;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AddressScope;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ChildAccountAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableWeld
@ExtendWith(MockitoExtension.class)
class ChildAccountValidateTest extends ChildAccountTest{

    @BeforeEach
    void setUp() {
        txCreator= new TransactionCreator(validator, propertiesHolder, timeService, calculator, blockchain, processor);
        txApplier = new TransactionApplier(blockchainConfig, referencedTransactionDao, accountService, accountPublicKeyService);
        txValidator = new TransactionValidator(blockchainConfig, phasingPollService, blockchain, calculator, accountControlPhasingService, accountService);

        child1.setParentId(0L);
        child1.setMultiSig(false);
        child2.setParentId(0L);
        child2.setMultiSig(false);

        EcBlockData ecBlockData = new EcBlockData(ECBLOCK_ID, ECBLOCK_HEIGHT);
        when(blockchain.getECBlock(300)).thenReturn(ecBlockData);
        when(accountService.getAccount(senderId)).thenReturn(sender);
        when(accountService.addOrGetAccount(CHILD_ID_1)).thenReturn(child1);
        when(accountService.addOrGetAccount(CHILD_ID_2)).thenReturn(child2);
        when(accountService.getAccount(CHILD_PUBLIC_KEY_1)).thenReturn(child1).thenReturn(null);
    }

    @Test
    void validateAttachment() throws AplException.ValidationException {
        //GIVEN
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .deadlineValue("1440")
            .feeATM(0)
            .attachment(attachment)
            .timestamp(300)
            .keySeed(Crypto.getKeySeed(secretPhrase))
            .broadcast(false)
            .build();
        Transaction tx = txCreator.createTransactionThrowingException(request);
        assertNotNull(tx);
        when(blockchain.getHeight()).thenReturn(ECBLOCK_HEIGHT+1);
        when(blockchain.getBlockIdAtHeight(ECBLOCK_HEIGHT)).thenReturn(ECBLOCK_ID);
        when(blockchainConfig.getCurrentConfig()).thenReturn(heightConfig);
        when(heightConfig.getMaxBalanceATM()).thenReturn(Long.MAX_VALUE);

        when(heightConfig.getMaxPayloadLength()).thenReturn(255*Constants.MIN_TRANSACTION_SIZE);

        //WHEN
        txValidator.validate(tx);

        verify(accountControlPhasingService).checkTransaction(tx);
    }

    @Test
    void validateAttachment_AmountGTZero() {
        //GIVEN
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .deadlineValue("1440")
            .feeATM(0)
            .amountATM(1000L)
            .attachment(attachment)
            .timestamp(300)
            .keySeed(Crypto.getKeySeed(secretPhrase))
            .broadcast(false)
            .build();
        Transaction tx = txCreator.createTransactionThrowingException(request);

        assertNotNull(tx);
        when(blockchain.getHeight()).thenReturn(ECBLOCK_HEIGHT+1);
        when(blockchain.getBlockIdAtHeight(ECBLOCK_HEIGHT)).thenReturn(ECBLOCK_ID);
        when(blockchainConfig.getCurrentConfig()).thenReturn(heightConfig);
        when(heightConfig.getMaxBalanceATM()).thenReturn(Long.MAX_VALUE);

        //WHEN
        try {
            txValidator.validate(tx);
            fail("Unexpected flow.");
        } catch (AplException.ValidationException e) {
            //THEN
            assertTrue(e.getMessage().contains("Transactions of this type must have recipient == 0, amount == 0"), "Unexpected exception message.");
        }
    }

    @Test
    void validateAttachment_withWrongChildAccountCount() throws AplException.ValidationException {
        //GIVEN
        int wrongChildCountValue = 1;// 2 is valid
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .publicKey(Convert.parseHexString(publicKey))
            .deadlineValue("1440")
            .feeATM(0L)
            .amountATM(0L)
            .attachment(new ChildAccountAttachment(AddressScope.IN_FAMILY, wrongChildCountValue, List.of(CHILD_PUBLIC_KEY_1, CHILD_PUBLIC_KEY_2)))
            .timestamp(300)
            .broadcast(false)
            .build();
        Transaction tx = txCreator.createTransactionThrowingException(request);

        assertNotNull(tx);
        when(blockchain.getHeight()).thenReturn(ECBLOCK_HEIGHT+1);
        when(blockchain.getBlockIdAtHeight(ECBLOCK_HEIGHT)).thenReturn(ECBLOCK_ID);
        when(blockchainConfig.getCurrentConfig()).thenReturn(heightConfig);
        when(heightConfig.getMaxBalanceATM()).thenReturn(Long.MAX_VALUE);


        //WHEN
        try {
            txValidator.validate(tx);
            fail("Unexpected flow.");
        } catch (AplException.ValidationException e) {
            //THEN
            assertTrue(e.getMessage().contains("Wrong value of the child count, count=1"), "Unexpected exception message.");
        }
    }

    @Test
    void validateAttachment_withParentPublicKey() throws AplException.ValidationException {
        //GIVEN
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .publicKey(Convert.parseHexString(publicKey))
            .deadlineValue("1440")
            .feeATM(0L)
            .amountATM(0L)
            .attachment(new ChildAccountAttachment(AddressScope.IN_FAMILY, 2, List.of(CHILD_PUBLIC_KEY_1, Convert.parseHexString(publicKey))))
            .timestamp(300)
            .broadcast(false)
            .build();
        Transaction tx = txCreator.createTransactionThrowingException(request);

        assertNotNull(tx);
        when(blockchain.getHeight()).thenReturn(ECBLOCK_HEIGHT+1);
        when(blockchain.getBlockIdAtHeight(ECBLOCK_HEIGHT)).thenReturn(ECBLOCK_ID);
        when(blockchainConfig.getCurrentConfig()).thenReturn(heightConfig);
        when(heightConfig.getMaxBalanceATM()).thenReturn(Long.MAX_VALUE);

        //WHEN
        try {
            txValidator.validate(tx);
            fail("Unexpected flow.");
        } catch (AplException.ValidationException e) {
            //THEN
            assertTrue(e.getMessage().contains("a child can't simultaneously be a parent"), "Unexpected exception message.");
        }
    }

    @Test
    void validateAttachment_withChildAlreadyExists() {
        //GIVEN
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .publicKey(Convert.parseHexString(publicKey))
            .deadlineValue("1440")
            .feeATM(0L)
            .amountATM(0L)
            .attachment(attachment)
            .timestamp(300)
            .broadcast(false)
            .build();
        Transaction tx = txCreator.createTransactionThrowingException(request);

        assertNotNull(tx);
        when(blockchain.getHeight()).thenReturn(ECBLOCK_HEIGHT+1);
        when(blockchain.getBlockIdAtHeight(ECBLOCK_HEIGHT)).thenReturn(ECBLOCK_ID);
        when(blockchainConfig.getCurrentConfig()).thenReturn(heightConfig);
        when(heightConfig.getMaxBalanceATM()).thenReturn(Long.MAX_VALUE);

        //WHEN
        try {
            txValidator.validate(tx);
            fail("Unexpected flow.");
        } catch (AplException.ValidationException e) {
        //THEN
            assertTrue(e.getMessage().contains("Child account already exists"), "Unexpected exception message.");
        }
    }
}