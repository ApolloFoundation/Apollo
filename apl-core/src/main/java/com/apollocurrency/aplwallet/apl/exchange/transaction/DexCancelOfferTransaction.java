package com.apollocurrency.aplwallet.apl.exchange.transaction;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOfferCancelAttachment;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONObject;

import javax.enterprise.inject.spi.CDI;
import java.nio.ByteBuffer;

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
        return new DexOfferCancelAttachment(buffer);
    }

    @Override
    public AbstractAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new DexOfferCancelAttachment(attachmentData);
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        DexOfferCancelAttachment attachment = (DexOfferCancelAttachment) transaction.getAttachment();
        long orderTransactionId = attachment.getTransactionId();

        DexOffer offer = dexService.getOfferByTransactionId(orderTransactionId);
        if(offer == null) {
            throw new AplException.NotCurrentlyValidException("Order was not found. Transaction: " + orderTransactionId);
        }

        if(!Long.valueOf(offer.getAccountId()).equals(transaction.getSenderId())){
            throw new AplException.NotCurrentlyValidException("Can cancel only your orders.");
        }

    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        DexOfferCancelAttachment attachment = (DexOfferCancelAttachment) transaction.getAttachment();
        DexOffer offer = dexService.getOfferByTransactionId(attachment.getTransactionId());

        dexService.cancelOffer(offer);
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {

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
