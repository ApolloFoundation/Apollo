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
import com.apollocurrency.aplwallet.apl.exchange.model.OfferType;
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
            throw new AplException.NotCurrentlyValidException(JSON.toString(incorrect("pairRate", String.format("Should be more than zero."))));
        }
        if (attachment.getOfferAmount() <= 0) {
            throw new AplException.NotCurrentlyValidException(JSON.toString(incorrect("offerAmount", String.format("Should be more than zero."))));
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

        if (shouldFreezeAPL(attachment.getType(), attachment.getOfferCurrency())) {
            Long fee = transaction.getFeeATM();
            Long amountATM = attachment.getOfferAmount();
            long totalAmountATM = Math.addExact(amountATM, fee);
            Account sender = Account.getAccount(transaction.getSenderId());

            if (sender.getUnconfirmedBalanceATM() < totalAmountATM) {
                throw new AplException.NotCurrentlyValidException("Not enough money.");
            }
        }
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        DexOfferAttachment attachment = (DexOfferAttachment) transaction.getAttachment();

        // On the Apl side.
        if(shouldFreezeAPL(attachment.getType(), attachment.getOfferCurrency())) {
            lockOnAplSide(transaction, senderAccount);
        }

        dexService.saveOffer(new DexOffer(transaction, attachment));
    }

    private void lockOnAplSide(Transaction transaction, Account senderAccount){
        DexOfferAttachment dexOfferAttachment = (DexOfferAttachment) transaction.getAttachment();
        long amountATM = dexOfferAttachment.getOfferAmount();

        senderAccount.addToUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), -amountATM);
    }

    private boolean shouldFreezeAPL(int offerType, int dexCurrencies){
        if (OfferType.SELL.ordinal() == offerType && DexCurrencies.APL.ordinal() == dexCurrencies) {
            return true;
        }
        return false;
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
        return "DexOrder";
    }


}
