/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.transaction;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.rest.service.DexOrderAttachmentFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOrderAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOrderAttachmentV2;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderType;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.exchange.utils.DexCurrencyValidator;
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
public class DexOrderTransaction extends DEX {

    private DexService dexService = CDI.current().select(DexService.class).get();
    private TimeService timeService = CDI.current().select(TimeService.class).get();

    @Override
    public byte getSubtype() {
        return TransactionType.SUBTYPE_DEX_ORDER;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.TRANSACTION_FEE;
    }

    @Override
    public AbstractAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return DexOrderAttachmentFactory.build(buffer);
    }

    @Override
    public AbstractAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return DexOrderAttachmentFactory.parse(attachmentData);
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        DexOrderAttachment attachment = (DexOrderAttachment) transaction.getAttachment();

        if (attachment.getOrderCurrency() == attachment.getPairCurrency()) {
            throw new AplException.NotValidException("Invalid Currency codes: " + attachment.getOrderCurrency() + " / " + attachment.getPairCurrency());
        }

        DexCurrencies offerCurrency;
        DexCurrencies pairCurrency;
        OrderType orderType;
        try {
            offerCurrency = DexCurrencies.getType(attachment.getOrderCurrency());
            pairCurrency = DexCurrencies.getType(attachment.getPairCurrency());
            orderType = OrderType.getType(attachment.getType());
        } catch (Exception ex) {
            throw new AplException.NotValidException("Invalid dex codes: " + attachment.getOrderCurrency() + " / " + attachment.getPairCurrency() + " / " + attachment.getType());
        }

        if (attachment.getPairRate() <= 0) {
            throw new AplException.NotValidException(JSON.toString(incorrect("pairRate", String.format("Should be more than zero."))));
        }
        if (attachment.getOrderAmount() <= 0) {
            throw new AplException.NotValidException(JSON.toString(incorrect("offerAmount", String.format("Should be more than zero."))));
        }

        if (attachment instanceof DexOrderAttachmentV2) {
            String address = ((DexOrderAttachmentV2) attachment).getFromAddress();
            if (StringUtils.isBlank(address) || address.length() > Constants.MAX_ADDRESS_LENGTH) {
                throw new AplException.NotValidException(JSON.toString(incorrect("FromAddress", String.format("Should be not null and address length less then " + Constants.MAX_ADDRESS_LENGTH))));
            }
        }

        try {
            Math.multiplyExact(attachment.getPairRate(), attachment.getOrderAmount());
        } catch (ArithmeticException ex) {
            throw new AplException.NotValidException("PairRate or OfferAmount is too big.");
        }


        Integer currentTime = timeService.getEpochTime();
        if (attachment.getFinishTime() <= 0 || attachment.getFinishTime() - currentTime > MAX_ORDER_DURATION_SEC) {
            throw new AplException.NotValidException(JSON.toString(incorrect("amountOfTime", String.format("value %d not in range [%d-%d]", attachment.getFinishTime(), 0, MAX_ORDER_DURATION_SEC))));
        }


        if (DexCurrencyValidator.haveFreezeOrRefundApl(orderType, offerCurrency, pairCurrency)) {
            Long amountATM = attachment.getOrderAmount();
            Account sender = Account.getAccount(transaction.getSenderId());

            if (sender == null) {
                throw new AplException.NotValidException("Unknown account:" + transaction.getSenderId());
            }

            if (sender.getUnconfirmedBalanceATM() < amountATM) {
                throw new AplException.NotValidException("Not enough money.");
            }
        }
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        return isDuplicate(DEX.DEX_ORDER_TRANSACTION, Long.toUnsignedString(transaction.getId()), duplicates, true);
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        DexOrderAttachment attachment = (DexOrderAttachment) transaction.getAttachment();

        DexOrder order = new DexOrder(transaction, attachment);
        // On the Apl side.
        if (DexCurrencyValidator.haveFreezeOrRefundApl(order)) {
            lockOnAplSide(transaction, senderAccount);
        }

        dexService.saveOrder(new DexOrder(transaction, attachment));
    }

    private void lockOnAplSide(Transaction transaction, Account senderAccount){
        DexOrderAttachment dexOrderAttachment = (DexOrderAttachment) transaction.getAttachment();
        long amountATM = dexOrderAttachment.getOrderAmount();

        senderAccount.addToUnconfirmedBalanceATM(LedgerEvent.DEX_FREEZE_MONEY, transaction.getId(), -amountATM);
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
