/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.app.EcBlockData;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableWeld
@ExtendWith(MockitoExtension.class)
class ChildAccountApplyTest extends ChildAccountTest{

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
    void applyAttachment() {
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

        //WHEN
        txApplier.apply(tx);

        //THEN
        assertEquals(sender.getId(), child1.getParentId());
        assertEquals(sender.getId(), child2.getParentId());

        assertTrue(child1.isChild());
        assertTrue(child2.isChild());

        assertTrue(child1.isMultiSig());
        assertTrue(child2.isMultiSig());

        assertEquals(attachment.getAddressScope(), child1.getAddrScope());
        assertEquals(attachment.getAddressScope(), child2.getAddrScope());

        ArgumentCaptor<Account> argument = ArgumentCaptor.forClass(Account.class);
        verify(accountService, times(2)).update(argument.capture(), eq(false));
        List<Long> args = argument.getAllValues().stream().map(Account::getId).collect(Collectors.toUnmodifiableList());
        assertTrue(args.contains(CHILD_ID_1));
        assertTrue(args.contains(CHILD_ID_2));

        verify(accountPublicKeyService).apply(child1, CHILD_PUBLIC_KEY_1);
        verify(accountPublicKeyService).apply(child2, CHILD_PUBLIC_KEY_2);
    }

}