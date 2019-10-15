package com.apollocurrency.aplwallet.apl.exchange.transaction;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexCloseOrderAttachment;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderType;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    ExchangeContract contract = new ExchangeContract(1L, 10L, 200L, 300L, 1000L, 2000L, ExchangeContractStatus.STEP_2, new byte[32], null, null, new byte[32], Constants.DEX_MIN_CONTRACT_TIME_WAITING_TO_REPLY);
    DexOrder order = new DexOrder(200L, 100L, "from", "to", OrderType.BUY, OrderStatus.OPEN, DexCurrencies.APL, 250L, DexCurrencies.ETH, BigDecimal.ONE, 500);
    DexService dexService = mock(DexService.class);
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from()
            .addBeans(
                    MockBean.of(mock(BlockchainConfig.class), BlockchainConfig.class),
                    MockBean.of(mock(BlockchainImpl.class), Blockchain.class, BlockchainImpl.class),
                    MockBean.of(dexService, DexService.class),
                    MockBean.of(mock(TimeService.class), TimeService.class)
            ).build();

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
        assertThrows(AplException.NotValidException.class, () -> transactionType.validateAttachment(tx));

        doReturn(contract).when(dexService).getDexContractById(10);
        assertThrows(AplException.NotValidException.class, () -> transactionType.validateAttachment(tx));

        doReturn(1000L).when(tx).getSenderId();
        assertThrows(AplException.NotValidException.class, () -> transactionType.validateAttachment(tx));

        contract.setContractStatus(ExchangeContractStatus.STEP_3);
        assertThrows(AplException.NotValidException.class, () -> transactionType.validateAttachment(tx));

        doReturn(order).when(dexService).getOrder(200L);
        assertThrows(AplException.NotValidException.class, () -> transactionType.validateAttachment(tx));

        order.setAccountId(1000L);
        assertThrows(AplException.NotValidException.class, () -> transactionType.validateAttachment(tx));

        order.setType(OrderType.SELL);
        assertThrows(AplException.NotValidException.class, () -> transactionType.validateAttachment(tx));

        order.setStatus(OrderStatus.WAITING_APPROVAL);

        transactionType.validateAttachment(tx);

        // recipient
        order.setAccountId(2000L);
        doReturn(2000L).when(tx).getSenderId();
        doReturn(order).when(dexService).getOrder(300L);

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