package com.apollocurrency.aplwallet.apl.exchange.transaction;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.incorrect;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexCloseOfferAttachment;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferType;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Map;
import javax.enterprise.inject.spi.CDI;


public class DexCloseOfferTransaction extends DEX {

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
        return new DexCloseOfferAttachment(buffer);
    }

    @Override
    public AbstractAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new DexCloseOfferAttachment(attachmentData);
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        DexCloseOfferAttachment attachment = (DexCloseOfferAttachment) transaction.getAttachment();
        DexOffer offer = dexService.getOfferByTransactionId(attachment.getOrderId());
        if (offer == null) {
            throw new AplException.NotValidException("Order with id " + attachment.getOrderId() + " does not exists");
        }
        if (offer.getAccountId() != transaction.getSenderId()) {
            throw new AplException.NotValidException(JSON.toString(incorrect("orderId", "You can close only your orders.")));
        }
        if (offer.getType() == OfferType.BUY) {
            throw new AplException.NotValidException("APL buy orders are closing automatically");
        }
        if (!offer.getStatus().isWaitingForApproval()) {
            throw new AplException.NotCurrentlyValidException(JSON.toString(incorrect("orderStatus", "You can close order in the status WaitingForApproval only, but: " + offer.getStatus().name())));
        }
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        DexCloseOfferAttachment attachment = (DexCloseOfferAttachment) transaction.getAttachment();
        dexService.closeOrder(attachment.getOrderId());
    }


    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {

    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        DexCloseOfferAttachment attachment = (DexCloseOfferAttachment) transaction.getAttachment();
        return isDuplicate(DEX.DEX_CLOSE_OFFER, Long.toUnsignedString(attachment.getOrderId()), duplicates, true);
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
