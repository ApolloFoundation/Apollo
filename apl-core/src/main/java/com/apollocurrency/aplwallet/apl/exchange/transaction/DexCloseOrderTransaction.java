package com.apollocurrency.aplwallet.apl.exchange.transaction;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.incorrect;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexCloseOrderAttachment;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderType;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Map;
import javax.enterprise.inject.spi.CDI;


public class DexCloseOrderTransaction extends DEX {

    private DexService dexService = CDI.current().select(DexService.class).get();

    @Override
    public byte getSubtype() {
        return TransactionType.SUBTYPE_DEX_CLOSE_OFFER;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.TRANSACTION_FEE;
    }

    @Override
    public AbstractAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new DexCloseOrderAttachment(buffer);
    }

    @Override
    public AbstractAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new DexCloseOrderAttachment(attachmentData);
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        DexCloseOrderAttachment attachment = (DexCloseOrderAttachment) transaction.getAttachment();
        DexOrder order = dexService.getOrderByTransactionId(attachment.getOrderId());
        if (order == null) {
            throw new AplException.NotValidException("Order with id " + attachment.getOrderId() + " does not exists");
        }
        if (order.getAccountId() != transaction.getSenderId()) {
            throw new AplException.NotValidException(JSON.toString(incorrect("orderId", "You can close only your orders.")));
        }
        if (order.getType() == OrderType.BUY) {
            throw new AplException.NotValidException("APL buy orders are closing automatically");
        }
        if (!order.getStatus().isWaitingForApproval()) {
            throw new AplException.NotCurrentlyValidException(JSON.toString(incorrect("orderStatus", "You can close order in the status WaitingForApproval only, but: " + order.getStatus().name())));
        }
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        DexCloseOrderAttachment attachment = (DexCloseOrderAttachment) transaction.getAttachment();
        dexService.closeOrder(attachment.getOrderId());
    }


    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {

    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        DexCloseOrderAttachment attachment = (DexCloseOrderAttachment) transaction.getAttachment();
        return isDuplicate(DEX.DEX_CLOSE_ORDER, Long.toUnsignedString(attachment.getOrderId()), duplicates, true);
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
        return "DexCloseOrder";
    }


}
