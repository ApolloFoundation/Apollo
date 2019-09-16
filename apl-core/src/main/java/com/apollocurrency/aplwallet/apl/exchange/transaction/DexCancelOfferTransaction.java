package com.apollocurrency.aplwallet.apl.exchange.transaction;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOrderCancelAttachment;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.exchange.utils.DexCurrencyValidator;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONObject;

import javax.enterprise.inject.spi.CDI;
import java.nio.ByteBuffer;
import java.util.Map;

public class DexCancelOfferTransaction extends DEX {

    private DexService dexService = CDI.current().select(DexService.class).get();

    @Override
    public byte getSubtype() {
        return TransactionType.SUBTYPE_DEX_OFFER_CANCEL;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.TRANSACTION_FEE;
    }

    @Override
    public AbstractAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new DexOrderCancelAttachment(buffer);
    }

    @Override
    public AbstractAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new DexOrderCancelAttachment(attachmentData);
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        DexOrderCancelAttachment attachment = (DexOrderCancelAttachment) transaction.getAttachment();
        long orderTransactionId = attachment.getOrderId();

        DexOrder order = dexService.getOrder(orderTransactionId);
        if (order == null) {
            throw new AplException.NotCurrentlyValidException("Order was not found. OrderId: " + orderTransactionId);
        }

        if (!Long.valueOf(order.getAccountId()).equals(transaction.getSenderId())) {
            throw new AplException.NotValidException("Can cancel only your orders.");
        }

        if (!OrderStatus.OPEN.equals(order.getStatus())) {
            throw new AplException.NotCurrentlyValidException("Can cancel only Open orders. Order Id/Tx: " + order.getId() + "/" + Long.toUnsignedString(order.getId())
                    + ", order status: " + order.getStatus() + " , Cancel Tx dbId:" + Long.toUnsignedString(transaction.getId()) + ", BlockId: " + Long.toUnsignedString(transaction.getECBlockId()));
        }

    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        DexOrderCancelAttachment attachment = (DexOrderCancelAttachment) transaction.getAttachment();
        DexOrder order = dexService.getOrder(attachment.getOrderId());

        try {
            if (DexCurrencyValidator.haveFreezeOrRefundApl(order)) {
                dexService.refundAPLFrozenMoney(order);
            }

            dexService.cancelOffer(order);

            dexService.reopenIncomeOrders(order.getId());
        } catch (AplException.ExecutiveProcessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {

    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        DexOrderCancelAttachment attachment = (DexOrderCancelAttachment) transaction.getAttachment();
        return isDuplicate(DEX.DEX_CANCEL_ORDER_TRANSACTION, Long.toUnsignedString(attachment.getOrderId()), duplicates, true);
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }

    @Override
    public boolean isPhasingSafe() {
        return false;
    }

    @Override
    public String getName() {
        return "CancelOrder";
    }
}
