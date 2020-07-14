/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.transaction;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPollResult;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexCloseOrderAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexControlOfFrozenMoneyAttachment;
import com.apollocurrency.aplwallet.apl.exchange.DexConfig;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderType;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
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

@EnableWeld
class DexCloseOrderTransactionTest {

    DexCloseOrderAttachment attachment = new DexCloseOrderAttachment(10);
    ExchangeContract contract = new ExchangeContract(
        1L, 10L, 200L, 300L, 1000L, 2000L,
        ExchangeContractStatus.STEP_2, new byte[32], "100", null, new byte[32],
        7200, null, true);
    DexOrder order = new DexOrder(200L, 100L, "from", "to", OrderType.BUY, OrderStatus.OPEN, DexCurrency.APL, 250L, DexCurrency.ETH, BigDecimal.ONE, 500);
    DexService dexService = mock(DexService.class);
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from()
        .addBeans(
            MockBean.of(mock(DexConfig.class), DexConfig.class),
            MockBean.of(mock(BlockchainConfig.class), BlockchainConfig.class),
            MockBean.of(mock(BlockchainImpl.class), Blockchain.class, BlockchainImpl.class),
            MockBean.of(mock(PhasingPollService.class), PhasingPollService.class),
            MockBean.of(dexService, DexService.class),
            MockBean.of(mock(TimeService.class), TimeService.class)
        ).build();
    @Inject
    Blockchain blockchain;
    @Inject
    PhasingPollService phasingPollService;
    DexCloseOrderTransaction transactionType;

    @BeforeEach
    void setUp() {
        transactionType = new DexCloseOrderTransaction();
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
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.validateAttachment(tx));

        doReturn(contract).when(dexService).getDexContractById(10);
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.validateAttachment(tx));

        doReturn(1000L).when(tx).getSenderId();
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.validateAttachment(tx));

        contract.setContractStatus(ExchangeContractStatus.STEP_3);
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.validateAttachment(tx));

        doReturn(order).when(dexService).getOrder(200L);
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.validateAttachment(tx));

        order.setAccountId(1000L);
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.validateAttachment(tx));

        order.setType(OrderType.SELL);
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.validateAttachment(tx));

        order.setStatus(OrderStatus.WAITING_APPROVAL);

        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.validateAttachment(tx));

        Transaction transferTx = mock(Transaction.class);

        doReturn(transferTx).when(blockchain).getTransaction(100);
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.validateAttachment(tx));

        doReturn(DEX.DEX_TRANSFER_MONEY_TRANSACTION).when(transferTx).getType();
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.validateAttachment(tx));

        doReturn(1000L).when(transferTx).getSenderId();
        DexControlOfFrozenMoneyAttachment attachment = new DexControlOfFrozenMoneyAttachment(11L, 100000L);
        doReturn(attachment).when(transferTx).getAttachment();
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.validateAttachment(tx));

        attachment.setContractId(10L);
        assertThrows(AplException.NotCurrentlyValidException.class, () -> transactionType.validateAttachment(tx));
        doReturn(new PhasingPollResult(1L, 1, 1L, 1L, true)).when(phasingPollService).getResult(100);
        transactionType.validateAttachment(tx);

        // recipient
        order.setAccountId(2000L);
        doReturn(2000L).when(tx).getSenderId();
        doReturn(order).when(dexService).getOrder(300L);
        contract.setCounterTransferTxId("100");
        doReturn(2000L).when(transferTx).getSenderId();
        transactionType.validateAttachment(tx);
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
        HashMap<TransactionType, Map<String, Integer>> duplicates = new HashMap<>();
        assertFalse(transactionType.isDuplicate(tx, duplicates));
        assertTrue(transactionType.isDuplicate(tx, duplicates));
        assertTrue(transactionType.isDuplicate(tx, duplicates));
    }
}