/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.transaction;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOfferAttachment;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONObject;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.incorrect;
import static com.apollocurrency.aplwallet.apl.util.Constants.MAX_ORDER_DURATION_SEC;

@Singleton
public class DexOfferTransaction extends DEX {

    private DexService dexService = CDI.current().select(DexService.class).get();
    private EpochTime epochTime = CDI.current().select(EpochTime.class).get();

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

        try {
            DexCurrencies.getType(attachment.getOfferCurrency());
            DexCurrencies.getType(attachment.getPairCurrency());
        } catch (Exception ex){
            throw new AplException.NotCurrentlyValidException("Invalid Currency codes: " + attachment.getOfferCurrency() + " / " + attachment.getPairCurrency());
        }

        if (attachment.getPairRate() <= 0 ) {
            throw new AplException.NotCurrentlyValidException(JSON.toString(incorrect("pairRate", String.format("Couldn't be less than zero."))));
        }
        if (attachment.getOfferAmount() <= 0) {
            throw new AplException.NotCurrentlyValidException(JSON.toString(incorrect("offerAmount", String.format("Couldn't be less than zero."))));
        }

        try {
            Math.multiplyExact(attachment.getPairRate(), attachment.getOfferAmount());
        } catch (ArithmeticException ex){
            throw new AplException.NotCurrentlyValidException("PairRate or OfferAmount is too big.");
        }


        Integer currentTime = epochTime.getEpochTime();
        if (attachment.getFinishTime() <= 0 || attachment.getFinishTime() - currentTime  > MAX_ORDER_DURATION_SEC) {
            throw new AplException.NotCurrentlyValidException(JSON.toString(incorrect("amountOfTime",  String.format("value %d not in range [%d-%d]", attachment.getFinishTime(), 0, MAX_ORDER_DURATION_SEC))));
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
