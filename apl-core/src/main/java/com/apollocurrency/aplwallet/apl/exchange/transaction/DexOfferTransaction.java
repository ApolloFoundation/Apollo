/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.transaction;

import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountEntity;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountServiceImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.rest.service.DexOfferAttachmentFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOfferAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOfferAttachmentV2;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferType;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import org.json.simple.JSONObject;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Map;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.incorrect;
import static com.apollocurrency.aplwallet.apl.util.Constants.MAX_ORDER_DURATION_SEC;

@Singleton
public class DexOfferTransaction extends DEX {

    private DexService dexService = CDI.current().select(DexService.class).get();
    private EpochTime epochTime = CDI.current().select(EpochTime.class).get();
    private AccountService accountService = CDI.current().select(AccountServiceImpl.class).get();

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
        return DexOfferAttachmentFactory.build(buffer);
    }

    @Override
    public AbstractAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return DexOfferAttachmentFactory.parse(attachmentData);
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        DexOfferAttachment attachment = (DexOfferAttachment) transaction.getAttachment();

        if (attachment.getOfferCurrency() == attachment.getPairCurrency()) {
            throw new AplException.NotValidException("Invalid Currency codes: " + attachment.getOfferCurrency() + " / " + attachment.getPairCurrency());
        }

        DexCurrencies offerCurrency;
        try {
            offerCurrency = DexCurrencies.getType(attachment.getOfferCurrency());
            DexCurrencies.getType(attachment.getPairCurrency());
        } catch (Exception ex){
            throw new AplException.NotValidException("Invalid Currency codes: " + attachment.getOfferCurrency() + " / " + attachment.getPairCurrency());
        }

        if (OfferType.SELL.ordinal() == attachment.getType() && !offerCurrency.isApl()){
            throw new AplException.NotValidException(JSON.toString(incorrect("offerCurrency", String.format("Not supported pair."))));
        }

        if (attachment.getPairRate() <= 0 ) {
            throw new AplException.NotValidException(JSON.toString(incorrect("pairRate", String.format("Should be more than zero."))));
        }
        if (attachment.getOfferAmount() <= 0) {
            throw new AplException.NotValidException(JSON.toString(incorrect("offerAmount", String.format("Should be more than zero."))));
        }

        if(attachment instanceof DexOfferAttachmentV2){
            String address = ((DexOfferAttachmentV2)attachment).getFromAddress();
            if(StringUtils.isBlank(address) || address.length() > Constants.MAX_ADDRESS_LENGTH){
                throw new AplException.NotValidException(JSON.toString(incorrect("FromAddress", String.format("Should be not null and address length less then " + Constants.MAX_ADDRESS_LENGTH))));
            }
        }

        try {
            Math.multiplyExact(attachment.getPairRate(), attachment.getOfferAmount());
        } catch (ArithmeticException ex){
            throw new AplException.NotValidException("PairRate or OfferAmount is too big.");
        }


        Integer currentTime = epochTime.getEpochTime();
        if (attachment.getFinishTime() <= 0 || attachment.getFinishTime() - currentTime  > MAX_ORDER_DURATION_SEC) {
            throw new AplException.NotValidException(JSON.toString(incorrect("amountOfTime",  String.format("value %d not in range [%d-%d]", attachment.getFinishTime(), 0, MAX_ORDER_DURATION_SEC))));
        }

        if (OfferType.SELL.ordinal() == attachment.getType()) {
            Long amountATM = attachment.getOfferAmount();
            AccountEntity sender = accountService.getAccountEntity(transaction.getSenderId());

            if (sender.getUnconfirmedBalanceATM() < amountATM) {
                throw new AplException.NotValidException("Not enough money.");
            }
        }
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        return isDuplicate(DEX.DEX_OFFER_TRANSACTION, Long.toUnsignedString(transaction.getId()), duplicates, true);
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, AccountEntity senderAccount) {
        return true;
    }

    @Override
    public void applyAttachment(Transaction transaction, AccountEntity senderAccount, AccountEntity recipientAccount) {
        DexOfferAttachment attachment = (DexOfferAttachment) transaction.getAttachment();

        // On the Apl side.
        if(OfferType.SELL.ordinal() == attachment.getType()) {
            lockOnAplSide(transaction, senderAccount);
        }

        dexService.saveOffer(new DexOffer(transaction, attachment));
    }

    private void lockOnAplSide(Transaction transaction, AccountEntity senderAccount){
        DexOfferAttachment dexOfferAttachment = (DexOfferAttachment) transaction.getAttachment();
        long amountATM = dexOfferAttachment.getOfferAmount();

        accountService.addToUnconfirmedBalanceATM(senderAccount, LedgerEvent.DEX_FREEZE_MONEY, transaction.getId(), -amountATM);
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, AccountEntity senderAccount) {
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
