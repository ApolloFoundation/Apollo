/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.transaction;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPollResult;
import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;
import com.apollocurrency.aplwallet.apl.core.model.dex.ExchangeContract;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexCloseOrderAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexControlOfFrozenMoneyAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.types.dex.DexCloseOrderTransaction;
import com.apollocurrency.aplwallet.apl.core.transaction.types.dex.DexTransferMoneyTransaction;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
@ExtendWith(MockitoExtension.class)
class DexCloseOrderTransactionTest {

    DexCloseOrderAttachment attachment = new DexCloseOrderAttachment(10);
    ExchangeContract contract = new ExchangeContract(
        1L, 10L, 200L, 300L, 1000L, 2000L,
        ExchangeContractStatus.STEP_2, new byte[32], "100", null, new byte[32],
        7200, null, true);
    DexOrder order = new DexOrder(200L, 100L, "from", "to", OrderType.BUY, OrderStatus.OPEN, DexCurrency.APL, 250L, DexCurrency.ETH, BigDecimal.ONE, 500);
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    AccountService accountService;
    @Mock
    DexService dexService;

    @Mock
    Blockchain blockchain;
    @Mock
    PhasingPollService phasingPollService;
    DexCloseOrderTransaction transactionType;

    @BeforeEach
    void setUp() {
        transactionType = new DexCloseOrderTransaction(blockchainConfig, accountService, dexService, blockchain, phasingPollService);
    }

    @Test
    void testParseAttachmentFromByteBuffer() throws AplException.NotValidException {
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.put((byte) 1);
        buffer.putLong(10);
        buffer.rewind();
        AbstractAttachment parsedAttachment = transactionType.parseAttachment(buffer);

        assertEquals(attachment, parsedAttachment);
    }

    @Test
    void testParseAttachmentFromJson() throws AplException.NotValidException {
        JSONObject object = new JSONObject();
        //TODO Resolve json format definition after discussion
        object.put("version.DexCloseOrder", 1);
        object.put("contractId", "10");

        AbstractAttachment parsedAttachment = transactionType.parseAttachment(object);

        assertEquals(attachment, parsedAttachment);
    }

    @Test
    void testSerializeDeserializeToBytes() throws AplException.NotValidException {
        ByteBuffer buffer = ByteBuffer.allocate(9);
        attachment.putBytes(buffer);
        buffer.rewind();

        AbstractAttachment parsedAttachment = transactionType.parseAttachment(buffer);

        assertEquals(attachment, parsedAttachment);
    }

    @Test
    void testSerializeDeserializeToJson() throws AplException.NotValidException {
        JSONObject json = attachment.getJSONObject();

        AbstractAttachment parsedAttachment = transactionType.parseAttachment(json);

        assertEquals(attachment, parsedAttachment);
    }

    @Test
    void testValidateAttachment() throws AplException.ValidationException {
        // sender
        Transaction tx = mock(Transaction.class);
        doReturn(attachment).when(tx).getAttachment();
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.doStateDependentValidation(tx));

        doReturn(contract).when(dexService).getDexContractById(10);
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.doStateDependentValidation(tx));

        doReturn(1000L).when(tx).getSenderId();
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.doStateDependentValidation(tx));

        contract.setContractStatus(ExchangeContractStatus.STEP_3);
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.doStateDependentValidation(tx));

        doReturn(order).when(dexService).getOrder(200L);
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.doStateDependentValidation(tx));

        order.setAccountId(1000L);
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.doStateDependentValidation(tx));

        order.setType(OrderType.SELL);
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.doStateDependentValidation(tx));

        order.setStatus(OrderStatus.WAITING_APPROVAL);

        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.doStateDependentValidation(tx));

        Transaction transferTx = mock(Transaction.class);
        doReturn(new DexTransferMoneyTransaction(blockchainConfig, accountService, dexService)).when(transferTx).getType();

        doReturn(transferTx).when(blockchain).getTransaction(100);
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.doStateDependentValidation(tx));

        doReturn(transactionType).when(transferTx).getType();
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.doStateDependentValidation(tx));

        doReturn(new DexTransferMoneyTransaction(blockchainConfig, accountService, dexService)).when(transferTx).getType();
        doReturn(1000L).when(transferTx).getSenderId();
        DexControlOfFrozenMoneyAttachment attachment = new DexControlOfFrozenMoneyAttachment(11L, 100000L);
        doReturn(attachment).when(transferTx).getAttachment();
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.doStateDependentValidation(tx));

        attachment = new DexControlOfFrozenMoneyAttachment(10L, 100000);
        doReturn(attachment).when(transferTx).getAttachment();

        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.doStateDependentValidation(tx));
        doReturn(new PhasingPollResult(1L, 1, 1L, 1L, true)).when(phasingPollService).getResult(100);
        transactionType.doStateDependentValidation(tx);

        // recipient
        order.setAccountId(2000L);
        doReturn(2000L).when(tx).getSenderId();
        doReturn(order).when(dexService).getOrder(300L);
        contract.setCounterTransferTxId("100");
        doReturn(2000L).when(transferTx).getSenderId();
        transactionType.doStateDependentValidation(tx);
    }

    @Test
    void testApplyAttachment() {
        Transaction tx = mock(Transaction.class);
        doReturn(attachment).when(tx).getAttachment();
        doReturn(contract).when(dexService).getDexContractById(10L);
        Account sender = mock(Account.class);
        doReturn(1000L).when(sender).getId();

        transactionType.applyAttachment(tx, sender, null);

        verify(dexService).closeOrder(200);


        doReturn(2000L).when(sender).getId();

        transactionType.applyAttachment(tx, sender, null);

        verify(dexService).closeOrder(200);
    }

    @Test
    void testIsDuplicate() {
        Transaction tx = mock(Transaction.class);
        doReturn(attachment).when(tx).getAttachment();
        HashMap<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates = new HashMap<>();
        assertFalse(transactionType.isDuplicate(tx, duplicates));
        assertTrue(transactionType.isDuplicate(tx, duplicates));
        assertTrue(transactionType.isDuplicate(tx, duplicates));
    }
}