/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.transaction;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOfferAttachment;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONObject;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

@Singleton
public class DexOfferTransaction extends DEX {

    private DexService dexService = CDI.current().select(DexService.class).get();

    @Override
    public byte getSubtype() {
        return TransactionType.SUBTYPE_DEX_OFFER;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.TRANSACTION_FEE;
    }

    @Override
    public AbstractAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new DexOfferAttachment(buffer);
    }

    @Override
    public AbstractAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new DexOfferAttachment(attachmentData);
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        DexOfferAttachment attachment = (DexOfferAttachment) transaction.getAttachment();

        if (attachment.getOfferCurrency() == attachment.getPairCurrency()) {
            throw new AplException.NotCurrentlyValidException("Invalid Currency codes: " + attachment.getOfferCurrency() + " / " + attachment.getPairCurrency());
        }

    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        DexOfferAttachment attachment = (DexOfferAttachment) transaction.getAttachment();

        if(dexService.getOfferByTransactionId(transaction.getId()) == null) {
            dexService.saveOffer(new DexOffer(transaction, attachment));
        }

        return true;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        DexOfferAttachment attachment = (DexOfferAttachment) transaction.getAttachment();

        if(dexService.getOfferByTransactionId(transaction.getId()) == null) {
            dexService.saveOffer(new DexOffer(transaction, attachment));
        }
        //TODO Implement change status on Close.
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        dexService.deleteOfferByTransactionId(transaction.getId());
    }

    @Override
    public boolean canHaveRecipient() {
        return true;
    }

    @Override
    public boolean mustHaveRecipient() {
        return false;
    }

    @Override
    public boolean isPhasingSafe() {
        return false;
    }

    @Override
    public String getName() {
        return "DexOffer";
    }


}
