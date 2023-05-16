/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.dgs;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSPurchase;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.DGSService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DGSRefundAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.DGS_REFUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DGSRefundTransactionTypeTest {
    private final long purchaseId = -1000;
    private final long refundATM = 250;
    private final DGSRefundAttachment attachment = new DGSRefundAttachment(purchaseId, refundATM);
    private final long senderId = -1;
    private final long recipientId = -2;

    @Mock
    BlockchainConfig config;
    @Mock
    AccountService accountService;
    @Mock
    DGSService dgsService;
    @Mock
    HeightConfig heightConfig;

    @InjectMocks
    DGSRefundTransactionType type;

    @Mock
    Transaction tx;
    @Mock
    EncryptedMessageAppendix encryptedMessageAppendix;
    @Mock
    DGSPurchase purchase;

    @Mock
    Account sender;
    @Mock
    Account recipient;


    @Test
    void getSpec() {
        assertEquals(DGS_REFUND, type.getSpec());
    }

    @Test
    void getLedgerEvent() {
        assertEquals(LedgerEvent.DIGITAL_GOODS_REFUND, type.getLedgerEvent());
    }

    @Test
    void getName() {
        assertEquals("DigitalGoodsRefund", type.getName());
    }

    @SneakyThrows
    @Test
    void parseAttachment_fromBytes() {
        ByteBuffer buff = ByteBuffer.allocate(17);
        buff.put((byte) 1);
        buff.putLong(purchaseId);
        buff.putLong(refundATM);
        buff.flip();

        DGSRefundAttachment parsedAttachment = type.parseAttachment(buff);

        assertEquals(attachment, parsedAttachment);
        assertFalse(buff.hasRemaining(), "DGSRefund attachment should be of size 17");
    }

    @SneakyThrows
    @Test
    void parseAttachment_fromJson() {
        JSONObject json = new JSONObject();
        json.put("version.DigitalGoodsRefund", 1);
        json.put("purchase", Long.toUnsignedString(purchaseId));
        json.put("refundATM", refundATM);

        DGSRefundAttachment parsedAttachment = type.parseAttachment(json);

        assertEquals(attachment, parsedAttachment);
    }

    @Test
    void doStateIndependentValidation_negativeRefundATM() {
        when(tx.getAttachment()).thenReturn(new DGSRefundAttachment(purchaseId, -1));

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid digital goods refund: {\"version.DigitalGoodsRefund\":1,\"purchase\":" +
            "\"18446744073709550616\",\"refundATM\":-1}", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_refundIsTooBig() {
        when(tx.getAttachment()).thenReturn(new DGSRefundAttachment(purchaseId, 100_000));
        when(config.getCurrentConfig()).thenReturn(heightConfig);
        when(heightConfig.getMaxBalanceATM()).thenReturn(99_999L);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid digital goods refund: {\"version.DigitalGoodsRefund\":1,\"purchase\":" +
            "\"18446744073709550616\",\"refundATM\":100000}", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_encryptedMessageIsNotText() {
        when(tx.getAttachment()).thenReturn(new DGSRefundAttachment(purchaseId, 100_000));
        when(config.getCurrentConfig()).thenReturn(heightConfig);
        when(heightConfig.getMaxBalanceATM()).thenReturn(100_000L);
        when(tx.getEncryptedMessage()).thenReturn(encryptedMessageAppendix);
        when(encryptedMessageAppendix.isText()).thenReturn(false);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.doStateIndependentValidation(tx));

        assertEquals("Only text encrypted messages allowed", ex.getMessage());
    }

    @SneakyThrows
    @Test
    void doStateIndependentValidation_withEncryptedMessage_OK() {
        when(tx.getAttachment()).thenReturn(new DGSRefundAttachment(purchaseId, 100_000));
        when(config.getCurrentConfig()).thenReturn(heightConfig);
        when(heightConfig.getMaxBalanceATM()).thenReturn(100_000L);
        when(tx.getEncryptedMessage()).thenReturn(encryptedMessageAppendix);
        when(encryptedMessageAppendix.isText()).thenReturn(true);

        type.doStateIndependentValidation(tx);
    }

    @SneakyThrows
    @Test
    void doStateIndependentValidation_noEncryptedMessage_OK() {
        when(tx.getAttachment()).thenReturn(new DGSRefundAttachment(purchaseId, 100_000));
        when(config.getCurrentConfig()).thenReturn(heightConfig);
        when(heightConfig.getMaxBalanceATM()).thenReturn(100_000L);
        when(tx.getEncryptedMessage()).thenReturn(null);

        type.doStateIndependentValidation(tx);
    }

    @Test
    void applyAttachmentUnconfirmed_notEnoughAPL() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(sender.getUnconfirmedBalanceATM()).thenReturn(249L);

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertFalse(applied, "applyAttachmentUnconfirmed should not pass, when not enough apl funds");
        verifyNoInteractions(accountService);
    }

    @Test
    void applyAttachmentUnconfirmed_OK() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(sender.getUnconfirmedBalanceATM()).thenReturn(250L);

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertTrue(applied, "applyAttachmentUnconfirmed should pass, when enough apl funds");
        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.DIGITAL_GOODS_REFUND, 0, -250);
    }

    @Test
    void undoAttachmentUnconfirmed() {
        when(tx.getAttachment()).thenReturn(attachment);

        type.undoAttachmentUnconfirmed(tx, sender);

        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.DIGITAL_GOODS_REFUND, 0L, 250);
    }

    @Test
    void applyAttachment() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(tx.getSenderId()).thenReturn(senderId);

        type.applyAttachment(tx, sender, recipient);

        verify(dgsService).refund(LedgerEvent.DIGITAL_GOODS_REFUND, 0L, senderId, purchaseId, refundATM, null);
    }

    @Test
    void doValidateAttachment_buyerNotMatchToTheTxRecipient() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(dgsService.getPurchase(purchaseId)).thenReturn(purchase);
        when(purchase.getBuyerId()).thenReturn(0L);
        when(tx.getRecipientId()).thenReturn(recipientId);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.doValidateAttachment(tx));

        assertEquals("Invalid digital goods refund: {\"version.DigitalGoodsRefund\":1,\"purchase\":" +
            "\"18446744073709550616\",\"refundATM\":250}", ex.getMessage());
    }

    @Test
    void doValidateAttachment_sellerNotMatchToTheTxSender() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(dgsService.getPurchase(purchaseId)).thenReturn(purchase);
        when(purchase.getBuyerId()).thenReturn(recipientId);
        when(tx.getRecipientId()).thenReturn(recipientId);
        when(tx.getSenderId()).thenReturn(senderId);
        when(purchase.getSellerId()).thenReturn(0L);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.doValidateAttachment(tx));

        assertEquals("Invalid digital goods refund: {\"version.DigitalGoodsRefund\":1,\"purchase\":" +
            "\"18446744073709550616\",\"refundATM\":250}", ex.getMessage());
    }

    @Test
    void doValidateAttachment_noPurchaseEncryptedGoods() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(dgsService.getPurchase(purchaseId)).thenReturn(purchase);
        when(purchase.getBuyerId()).thenReturn(recipientId);
        when(tx.getRecipientId()).thenReturn(recipientId);
        when(tx.getSenderId()).thenReturn(senderId);
        when(purchase.getSellerId()).thenReturn(senderId);
        when(purchase.getEncryptedGoods()).thenReturn(null);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doValidateAttachment(tx));

        assertEquals("Purchase does not exist or is not delivered or is already refunded", ex.getMessage());
    }

    @Test
    void doValidateAttachment_purchaseAlreadyRefunded() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(dgsService.getPurchase(purchaseId)).thenReturn(purchase);
        when(purchase.getBuyerId()).thenReturn(recipientId);
        when(tx.getRecipientId()).thenReturn(recipientId);
        when(tx.getSenderId()).thenReturn(senderId);
        when(purchase.getSellerId()).thenReturn(senderId);
        when(purchase.getEncryptedGoods()).thenReturn(mock(EncryptedData.class));
        when(purchase.getRefundATM()).thenReturn(1L);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doValidateAttachment(tx));

        assertEquals("Purchase does not exist or is not delivered or is already refunded", ex.getMessage());
    }

    @Test
    void doValidateAttachment_notEnoughAplBalance() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(dgsService.getPurchase(purchaseId)).thenReturn(purchase);
        when(purchase.getBuyerId()).thenReturn(recipientId);
        when(tx.getRecipientId()).thenReturn(recipientId);
        when(tx.getSenderId()).thenReturn(senderId);
        when(purchase.getSellerId()).thenReturn(senderId);
        when(purchase.getEncryptedGoods()).thenReturn(mock(EncryptedData.class));
        when(purchase.getRefundATM()).thenReturn(0L);
        when(sender.getUnconfirmedBalanceATM()).thenReturn(249L);
        when(accountService.getAccount(senderId)).thenReturn(sender);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doValidateAttachment(tx));

        assertEquals("Sender 18446744073709551615 has not enough funds: required 250, but only has 249", ex.getMessage());
    }

    @SneakyThrows
    @Test
    void doValidateAttachment_OK() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(dgsService.getPurchase(purchaseId)).thenReturn(purchase);
        when(purchase.getBuyerId()).thenReturn(recipientId);
        when(tx.getRecipientId()).thenReturn(recipientId);
        when(tx.getSenderId()).thenReturn(senderId);
        when(purchase.getSellerId()).thenReturn(senderId);
        when(purchase.getEncryptedGoods()).thenReturn(mock(EncryptedData.class));
        when(purchase.getRefundATM()).thenReturn(0L);
        when(sender.getUnconfirmedBalanceATM()).thenReturn(refundATM);
        when(accountService.getAccount(senderId)).thenReturn(sender);

        type.doValidateAttachment(tx);
    }

    @Test
    void doValidateAttachment_noPurchase() {
        when(tx.getAttachment()).thenReturn(attachment);
        when(dgsService.getPurchase(purchaseId)).thenReturn(null);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doValidateAttachment(tx));

        assertEquals("Purchase does not exist or is not delivered or is already refunded", ex.getMessage());
    }

    @Test
    void isDuplicate_noPurchaseRefunds() {
        when(tx.getAttachment()).thenReturn(attachment);
        HashMap<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates = new HashMap<>();
        // refund tx for purchase with id -999 present in the block, but no refund tx for purchase with id -1000
        duplicates.put(DGS_REFUND, new HashMap<>(Map.of(Long.toUnsignedString(-999), 0)));

        boolean duplicate = type.isDuplicate(tx, duplicates);

        assertFalse(duplicate, "New refund tx for purchase '-1000' should not be a duplicate, when refund " +
            "for purchase '-999' is present in the block");
        assertEquals(Map.of(DGS_REFUND, Map.of(Long.toUnsignedString(-999L), 0, Long.toUnsignedString(-1000L), 0)), duplicates);
    }

    @Test
    void isDuplicate_purchaseRefundAlreadyInTheBlock() {
        when(tx.getAttachment()).thenReturn(attachment);
        HashMap<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates = new HashMap<>();
        // refund tx for purchase with id -1000 is already present in the block
        duplicates.put(DGS_REFUND, new HashMap<>(Map.of(Long.toUnsignedString(purchaseId), 0)));

        boolean duplicate = type.isDuplicate(tx, duplicates);

        assertTrue(duplicate, "Second refund tx for purchase '-1000' should be a duplicate, when refund " +
            "for purchase '-1000' is already in the block");
        assertEquals(Map.of(DGS_REFUND, Map.of(Long.toUnsignedString(purchaseId), 0)), duplicates);
    }

    @Test
    void canHaveRecipient() {
        assertTrue(type.canHaveRecipient(), "DGSRefund tx type can have a recipient");
    }

    @Test
    void isPhasingSafe() {
        assertFalse(type.isPhasingSafe(), "DGSRefund tx type should not be phasing safe");
    }
}