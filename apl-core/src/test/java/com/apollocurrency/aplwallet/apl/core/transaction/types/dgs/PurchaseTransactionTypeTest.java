/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.dgs;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSGoods;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.DGSService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsPurchaseAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseTransactionTypeTest {
    public static final int GOODS_ID = 1;
    public static final long SENDER_ID = 222L;
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    AccountService accountService;
    @Mock
    DGSService dgsService;
    @Mock
    Blockchain blockchain;

    @InjectMocks
    PurchaseTransactionType type;

    // supporting mocks for several scenarios
    @Mock
    Transaction tx;
    @Mock
    Account sender;
    @Mock
    Account recipient;
    @Mock
    HeightConfig heightConfig;


    @Test
    void getSpec() {
        assertEquals(TransactionTypes.TransactionTypeSpec.DGS_PURCHASE, type.getSpec());
    }

    @Test
    void getLedgerEvent() {
        assertEquals(LedgerEvent.DIGITAL_GOODS_PURCHASE, type.getLedgerEvent());
    }

    @Test
    void getName() {
        assertEquals("DigitalGoodsPurchase", type.getName());
    }

    @Test
    void parseAttachmentFromBytes() throws AplException.NotValidException {
        ByteBuffer buffer = ByteBuffer.allocate(25);
        buffer.put((byte) GOODS_ID); // version
        buffer.putLong(-GOODS_ID); // goods id
        buffer.putInt(2); // quantity
        buffer.putLong(10); // price
        buffer.putInt(1000); // deadline timestamp
        buffer.flip();

        DigitalGoodsPurchaseAttachment attachment = type.parseAttachment(buffer);

        assertEquals(new DigitalGoodsPurchaseAttachment(-GOODS_ID, 2, 10, 1000), attachment);
    }

    @Test
    void parseAttachmentFromJson() throws AplException.NotValidException {
        JSONObject attachmentJson = new JSONObject();
        attachmentJson.put("version.DigitalGoodsPurchase", GOODS_ID);
        attachmentJson.put("goods", "1");
        attachmentJson.put("quantity", 25L);
        attachmentJson.put("priceATM", 100L);
        attachmentJson.put("deliveryDeadlineTimestamp", 2000L);

        DigitalGoodsPurchaseAttachment parsedAttachment = type.parseAttachment(attachmentJson);

        assertEquals(new DigitalGoodsPurchaseAttachment(GOODS_ID, 25, 100, 2000), parsedAttachment);
    }

    @Test
    void doStateIndependentValidationZeroQuantity() {
        mockAttachment(0, 10L);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid digital goods purchase: {\"quantity\":0,\"deliveryDeadlineTimestamp\":1000,\"goods\":\"1\",\"priceATM\":10,\"version.DigitalGoodsPurchase\":1}", ex.getMessage());
    }

    @Test
    void doStateIndependentValidationQuantityGreaterThan1Billion() {
        mockAttachment(1_000_000_001, 10L);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid digital goods purchase: {\"quantity\":1000000001,\"deliveryDeadlineTimestamp\":1000,\"goods\":\"1\",\"priceATM\":10,\"version.DigitalGoodsPurchase\":1}", ex.getMessage());
    }

    @Test
    void doStateIndependentValidationPriceIsNegative() {
        mockAttachment(2, -10L);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid digital goods purchase: {\"quantity\":2,\"deliveryDeadlineTimestamp\":1000,\"goods\":\"1\",\"priceATM\":-10,\"version.DigitalGoodsPurchase\":1}", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_purchaseTotalPriceLongOverflow() {
        mockAttachment(2, Long.MAX_VALUE);
        doReturn(type).when(tx).getType();

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.doStateIndependentValidation(tx));

        assertEquals("Result of multiplying x=2, y=9223372036854775807 exceeds the allowed range [-9223372036854775808;9223372036854775807], transaction='null', type='DGS_PURCHASE', sender='0'", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_purchaseTotalPriceIsGreaterThanMaxAllowed() {
        mockAttachment(2, 10L);
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        doReturn(19L).when(heightConfig).getMaxBalanceATM();
        doReturn(type).when(tx).getType();

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid digital goods purchase: {\"quantity\":2,\"deliveryDeadlineTimestamp\":1000,\"goods\":\"1\",\"priceATM\":10,\"version.DigitalGoodsPurchase\":1}", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_encryptedMessageIsNotText() {
        mockAttachment(2, 10L);
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        doReturn(20L).when(heightConfig).getMaxBalanceATM();
        doReturn(type).when(tx).getType();
        EncryptedMessageAppendix encryptedMessageAppendix = mock(EncryptedMessageAppendix.class);
        doReturn(encryptedMessageAppendix).when(tx).getEncryptedMessage();

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.doStateIndependentValidation(tx));

        assertEquals("Only text encrypted messages allowed", ex.getMessage());
    }


    @Test
    void doStateIndependentValidation_deliveryDeadlineTimestampReferToThePast() {
        mockAttachment(2, 10L);
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        doReturn(20L).when(heightConfig).getMaxBalanceATM();
        doReturn(type).when(tx).getType();
        doReturn(1500).when(blockchain).getLastBlockTimestamp();

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateIndependentValidation(tx));

        assertEquals("Delivery deadline has already expired: 1000", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_OK_withEncryptedMessage() throws AplException.ValidationException {
        mockAttachment(2, 10L);
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        doReturn(20L).when(heightConfig).getMaxBalanceATM();
        doReturn(type).when(tx).getType();
        doReturn(500).when(blockchain).getLastBlockTimestamp();
        EncryptedMessageAppendix encryptedMessageAppendix = mock(EncryptedMessageAppendix.class);
        doReturn(encryptedMessageAppendix).when(tx).getEncryptedMessage();
        doReturn(true).when(encryptedMessageAppendix).isText();

        type.doStateIndependentValidation(tx);

        verify(blockchain).getLastBlockTimestamp();
    }

    @Test
    void doStateIndependentValidation_OK_withoutEncryptedMessage() throws AplException.ValidationException {
        mockAttachment(2, 10L);
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        doReturn(20L).when(heightConfig).getMaxBalanceATM();
        doReturn(type).when(tx).getType();
        doReturn(500).when(blockchain).getLastBlockTimestamp();

        type.doStateIndependentValidation(tx);

        verify(blockchain).getLastBlockTimestamp();
    }


    @Test
    void applyAttachmentUnconfirmedOK() {
        mockAttachment(2, 10L);
        doReturn(25L).when(sender).getUnconfirmedBalanceATM();

        boolean result = type.applyAttachmentUnconfirmed(tx, sender);

        assertTrue(result, "Attachment should be applied unconfirmed successfully for the purchase total 20 with account balance 25");
        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.DIGITAL_GOODS_PURCHASE, 0, -20);
    }

    @Test
    void applyAttachmentUnconfirmedDoubleSpending() {
        mockAttachment(2, 10L);
        doReturn(10L).when(sender).getUnconfirmedBalanceATM();

        boolean result = type.applyAttachmentUnconfirmed(tx, sender);

        assertFalse(result, "Attachment should NOT be applied unconfirmed (double spending) for the purchase total 20 with account balance 10");
        verifyNoInteractions(accountService);
    }

    @Test
    void undoAttachmentUnconfirmed() {
        mockAttachment(5, 10L);

        type.undoAttachmentUnconfirmed(tx, sender);

        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.DIGITAL_GOODS_PURCHASE, 0, 50);
    }

    @Test
    void applyAttachment() {
        DigitalGoodsPurchaseAttachment attachment = mockAttachment(GOODS_ID, 5);

        type.applyAttachment(tx, sender, recipient);

        verify(dgsService).purchase(tx, attachment);
    }

    @Test
    void doValidateAttachment_goodsSellerIsNotTxRecipient() {
        mockAttachment(5, 10);
        DGSGoods goods = mock(DGSGoods.class);
        doReturn(111L).when(goods).getSellerId();
        doReturn(110L).when(tx).getRecipientId();
        doReturn(goods).when(dgsService).getGoods(GOODS_ID);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doValidateAttachment(tx));

        assertEquals("Invalid digital goods purchase: {\"quantity\":5,\"deliveryDeadlineTimestamp\":1000,\"goods\":\"1\",\"priceATM\":10,\"version.DigitalGoodsPurchase\":1}", ex.getMessage());
    }

    @Test
    void doValidateAttachment_noGoods() {
        mockAttachment(5, 10);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class, () -> type.doValidateAttachment(tx));

        assertEquals("Goods '1' not yet listed or already delisted", ex.getMessage());
    }

    @Test
    void doValidateAttachment_goodsAlreadyDelisted() {
        mockAttachment(5, 10);
        DGSGoods goods = mock(DGSGoods.class);
        doReturn(111L).when(goods).getSellerId();
        doReturn(111L).when(tx).getRecipientId();
        doReturn(true).when(goods).isDelisted();
        doReturn(goods).when(dgsService).getGoods(GOODS_ID);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class, () -> type.doValidateAttachment(tx));

        assertEquals("Goods '1' not yet listed or already delisted", ex.getMessage());
    }

    @Test
    void doValidateAttachment_goodsQuantityChanged() {
        mockAttachment(5, 10);
        DGSGoods goods = mock(DGSGoods.class);
        doReturn(111L).when(goods).getSellerId();
        doReturn(111L).when(tx).getRecipientId();
        doReturn(false).when(goods).isDelisted();
        doReturn(goods).when(dgsService).getGoods(GOODS_ID);
        doReturn(4).when(goods).getQuantity();

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class, () -> type.doValidateAttachment(tx));

        assertEquals("Goods price or quantity changed: {\"quantity\":5,\"deliveryDeadlineTimestamp\":1000,\"goods\":\"1\",\"priceATM\":10,\"version.DigitalGoodsPurchase\":1}", ex.getMessage());
    }

    @Test
    void doValidateAttachment_goodsPriceChanged() {
        mockAttachment(5, 10);
        DGSGoods goods = mock(DGSGoods.class);
        doReturn(111L).when(goods).getSellerId();
        doReturn(111L).when(tx).getRecipientId();
        doReturn(false).when(goods).isDelisted();
        doReturn(goods).when(dgsService).getGoods(GOODS_ID);
        doReturn(5).when(goods).getQuantity();
        doReturn(20L).when(goods).getPriceATM();

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class, () -> type.doValidateAttachment(tx));

        assertEquals("Goods price or quantity changed: {\"quantity\":5,\"deliveryDeadlineTimestamp\":1000,\"goods\":\"1\",\"priceATM\":10,\"version.DigitalGoodsPurchase\":1}", ex.getMessage());
    }

    @Test
    void doValidateAttachment_notEnoughFunds() {
        mockAttachment(5, 10);
        DGSGoods goods = mock(DGSGoods.class);
        doReturn(111L).when(goods).getSellerId();
        doReturn(111L).when(tx).getRecipientId();
        doReturn(false).when(goods).isDelisted();
        doReturn(goods).when(dgsService).getGoods(GOODS_ID);
        doReturn(5).when(goods).getQuantity();
        doReturn(10L).when(goods).getPriceATM();
        when(tx.getSenderId()).thenReturn(SENDER_ID);
        when(accountService.getAccount(SENDER_ID)).thenReturn(sender);
        when(sender.getUnconfirmedBalanceATM()).thenReturn(49L);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class, () -> type.doValidateAttachment(tx));

        assertEquals("Sender 222 has not enough funds: required 50, but only has 49", ex.getMessage());
    }

    @Test
    void doValidateAttachmentOK() throws AplException.ValidationException {
        mockAttachment(5, 10);
        DGSGoods goods = mock(DGSGoods.class);
        doReturn(111L).when(goods).getSellerId();
        doReturn(111L).when(tx).getRecipientId();
        doReturn(false).when(goods).isDelisted();
        doReturn(goods).when(dgsService).getGoods(GOODS_ID);
        doReturn(5).when(goods).getQuantity();
        doReturn(10L).when(goods).getPriceATM();
        when(tx.getSenderId()).thenReturn(SENDER_ID);
        when(accountService.getAccount(SENDER_ID)).thenReturn(sender);
        when(sender.getUnconfirmedBalanceATM()).thenReturn(50L);

        type.doValidateAttachment(tx);

        verify(dgsService).getGoods(GOODS_ID);
    }

    @Test
    void isDuplicate_noDuplicatesForManyPurchasesOfTheSameGoods() {
        mockAttachment(1, 5);
        Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicatesMap = new HashMap<>();

        boolean duplicate = type.isDuplicate(tx, duplicatesMap);

        assertFalse(duplicate, "First duplicate check should not lead to duplicate occurrence");


        boolean duplicateAfterCheck = type.isDuplicate(tx, duplicatesMap);

        assertFalse(duplicateAfterCheck, "Second duplicate check also should not lead to duplicate occurrence, because check is not exclusive");
        assertEquals(Map.of(TransactionTypes.TransactionTypeSpec.DGS_DELISTING, Map.of("1", 2)), duplicatesMap);
    }

    @Test
    void isDuplicate_duplicateWhenGoodsShouldBeDelistedInTheBlock() {
        mockAttachment(1, 5);
        Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicatesMap = new HashMap<>();
        HashMap<String, Integer> delistingDuplicateMap = new HashMap<>();
        duplicatesMap.put(TransactionTypes.TransactionTypeSpec.DGS_DELISTING, delistingDuplicateMap);
        delistingDuplicateMap.put("1", 0); // lock the GOODS with id 1 to not be changed by other transactions, e.g. purchase

        boolean duplicate = type.isDuplicate(tx, duplicatesMap);

        assertTrue(duplicate, "Purchase transaction for goods " + GOODS_ID + " should be duplicate, because exclusive DGS_DELISTING tx is already in a block");
    }

    @Test
    void canHaveRecipient() {
        assertTrue(type.canHaveRecipient(), "DGS_PURCHASE type should have recipient (goods seller's id)");
    }

    @Test
    void isPhasingSafe() {
        assertFalse(type.isPhasingSafe(), "DGS_PURCHASE type should not be phasing safe");
    }

    private DigitalGoodsPurchaseAttachment mockAttachment(int quantity, long price) {
        DigitalGoodsPurchaseAttachment attachment = new DigitalGoodsPurchaseAttachment(1L, quantity, price, 1000);
        doReturn(attachment).when(tx).getAttachment();
        return attachment;
    }
}