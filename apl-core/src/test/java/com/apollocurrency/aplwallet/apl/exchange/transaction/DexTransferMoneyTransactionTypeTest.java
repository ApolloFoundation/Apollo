/*
 * Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.transaction;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;
import com.apollocurrency.aplwallet.apl.core.model.dex.ExchangeContract;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexControlOfFrozenMoneyAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.types.dex.DexTransferMoneyTransactionType;
import com.apollocurrency.aplwallet.apl.dex.core.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.dex.core.model.ExchangeContractStatus;
import com.apollocurrency.aplwallet.apl.dex.core.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.dex.core.model.OrderType;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
@ExtendWith(MockitoExtension.class)
class DexTransferMoneyTransactionTypeTest {
    DexControlOfFrozenMoneyAttachment attachment = new DexControlOfFrozenMoneyAttachment(64, 100);
    ExchangeContract contract = new ExchangeContract(
        1L, 64L, 200L, 300L, 1000L, 2000L,
        ExchangeContractStatus.STEP_3, new byte[32], null, null,
        new byte[32], 7200, 1, false);
    DexTransferMoneyTransactionType transactionType;

    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    AccountService accountService;
    @Mock
    DexService dexService = mock(DexService.class);

    @BeforeEach
    void setUp() {
        transactionType = new DexTransferMoneyTransactionType(blockchainConfig, accountService, dexService);
    }

    @Test
    void parseByteAttachment() throws AplException.NotValidException {
        ByteBuffer buffer = ByteBuffer.allocate(17);
        buffer.put((byte) 1);
        buffer.putLong(64);
        buffer.putLong(100);
        buffer.rewind();
        AbstractAttachment parsedAttachment = transactionType.parseAttachment(buffer);

        assertEquals(attachment, parsedAttachment);
    }

    @Test
    void testJsonParseAttachment() throws AplException.NotValidException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("contractId", 64L);
        jsonObject.put("offerAmount", 100L);
        jsonObject.put("version.DexTransferMoney", 1);

        AbstractAttachment parsedAttachment = transactionType.parseAttachment(jsonObject);

        assertEquals(attachment, parsedAttachment);
    }

    @Test
    void testSerializeDeserializeAttachmentToByteBuffer() throws AplException.NotValidException {
        int myFullSize = attachment.getFullSize();
        ByteBuffer buffer = ByteBuffer.allocate(myFullSize);
        attachment.putBytes(buffer);
        buffer.rewind();

        AbstractAttachment parsedAttachment = transactionType.parseAttachment(buffer);

        assertEquals(attachment, parsedAttachment);
    }

    @Test
    void testSerializeDeserializeAttachmentToJson() throws AplException.NotValidException {
        JSONObject jsonObject = attachment.getJSONObject();

        AbstractAttachment parsedAttachment = transactionType.parseAttachment(jsonObject);

        assertEquals(attachment, parsedAttachment);
    }

    @Test
    void testValidateAttachment() throws AplException.ValidationException {
        Transaction tx = mock(Transaction.class);
        doReturn(attachment).when(tx).getAttachment();
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.doStateDependentValidation(tx)); // no contract

        doReturn(contract).when(dexService).getDexContractById(anyLong());
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.doStateDependentValidation(tx));

        doReturn(1000L).when(tx).getSenderId();
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.doStateDependentValidation(tx));

        doReturn(2000L).when(tx).getSenderId();
        doReturn(1000L).when(tx).getRecipientId();
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.doStateDependentValidation(tx));

        contract.setCounterTransferTxId("100");
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.doStateDependentValidation(tx));

        doReturn(100L).when(tx).getId();
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.doStateDependentValidation(tx));

        DexOrder offer = new DexOrder(300L, 0L, "", "", OrderType.SELL, OrderStatus.OPEN, DexCurrency.APL, 100L, DexCurrency.PAX, BigDecimal.ONE, 500);
        doReturn(offer).when(dexService).getOrder(200L);
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.doStateDependentValidation(tx));

        offer.setAccountId(1000L);
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.doStateDependentValidation(tx));

        offer.setStatus(OrderStatus.WAITING_APPROVAL);
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.doStateDependentValidation(tx));

        offer.setType(OrderType.BUY);
        transactionType.doStateDependentValidation(tx);

        doReturn(1000L).when(tx).getSenderId();
        doReturn(2000L).when(tx).getRecipientId();
        doReturn(offer).when(dexService).getOrder(300L);
        contract.setCounterTransferTxId("1");
        contract.setTransferTxId("100");
        offer.setAccountId(2000L);
        transactionType.doStateDependentValidation(tx);
    }


    @Test
    void testApplyAttachment() {
        Transaction tx = mock(Transaction.class);
        doReturn(attachment).when(tx).getAttachment();
        doReturn(contract).when(dexService).getDexContractById(anyLong());

        Account sender = mock(Account.class);
        Account recipient = mock(Account.class);
        doReturn(1000L).when(sender).getId();
//        doReturn(2000L).when(recipient).getId();

        transactionType.applyAttachment(tx, sender, recipient);

        verify(transactionType.getAccountService()).addToBalanceATM(sender, LedgerEvent.DEX_TRANSFER_MONEY, 0, -100);
        verify(transactionType.getAccountService()).addToBalanceAndUnconfirmedBalanceATM(recipient, LedgerEvent.DEX_TRANSFER_MONEY, 0, 100);
        verify(dexService).closeOrder(300);
    }

    @Test
    void testApplyAttachmentForContractRecipient() {
        Transaction tx = mock(Transaction.class);
        doReturn(attachment).when(tx).getAttachment();
        doReturn(contract).when(dexService).getDexContractById(anyLong());
        Account sender = mock(Account.class);
        Account recipient = mock(Account.class);
        doReturn(2000L).when(sender).getId();
//        doReturn(1000L).when(recipient).getId();

        transactionType.applyAttachment(tx, sender, recipient);

        verify(transactionType.getAccountService()).addToBalanceATM(sender, LedgerEvent.DEX_TRANSFER_MONEY, 0, -100);
        verify(transactionType.getAccountService()).addToBalanceAndUnconfirmedBalanceATM(recipient, LedgerEvent.DEX_TRANSFER_MONEY, 0, 100);
        verify(dexService).closeOrder(200);
    }

    @Test
    void testIsDuplicate() {
        Transaction tx = mock(Transaction.class);
        doReturn(attachment).when(tx).getAttachment();

        Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates = new HashMap<>();

        assertFalse(transactionType.isDuplicate(tx, duplicates)); // populate map
        assertTrue(transactionType.isDuplicate(tx, duplicates)); // now contract with id = 64 added to map and another tx, which refer to this contract will be rejected as duplicate
    }
}