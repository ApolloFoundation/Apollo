/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import static org.slf4j.LoggerFactory.getLogger;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.http.API;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.monetary.Currency;
import com.apollocurrency.aplwallet.apl.core.monetary.CurrencySellOffer;
import com.apollocurrency.aplwallet.apl.core.monetary.MonetarySystem;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionScheduler;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemExchangeBuyAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemPublishExchangeOffer;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Filter;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;
import org.slf4j.Logger;

public final class ScheduleCurrencyBuy extends CreateTransaction {
    private static final Logger LOG = getLogger(ScheduleCurrencyBuy.class);

    private static class ScheduleCurrencyBuyHolder {
        private static final ScheduleCurrencyBuy INSTANCE = new ScheduleCurrencyBuy();
    }

    public static ScheduleCurrencyBuy getInstance() {
        return ScheduleCurrencyBuyHolder.INSTANCE;
    }

    private ScheduleCurrencyBuy() {
        super(new APITag[] {APITag.MS, APITag.CREATE_TRANSACTION}, "currency", "rateATM", "units", "offerIssuer",
                "transactionJSON", "transactionBytes", "prunableAttachmentJSON", "adminPassword");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        String transactionJSON = Convert.emptyToNull(req.getParameter("transactionJSON"));
        String transactionBytes = Convert.emptyToNull(req.getParameter("transactionBytes"));
        String prunableAttachmentJSON = Convert.emptyToNull(req.getParameter("prunableAttachmentJSON"));
        long offerIssuerId = ParameterParser.getAccountId(req, "offerIssuer", true);

        try {
            JSONObject response;
            Transaction transaction;
            if (transactionBytes == null && transactionJSON == null) {
                boolean broadcast = !"false".equalsIgnoreCase(req.getParameter("broadcast"));
                if (broadcast) {
                    return JSONResponses.error("Must use broadcast=false to schedule a future currency buy");
                }
                Currency currency = ParameterParser.getCurrency(req);
                long rateATM = ParameterParser.getLong(req, "rateATM", 0, Long.MAX_VALUE, true);
                long units = ParameterParser.getLong(req, "units", 0, Long.MAX_VALUE, true);
                Account account = ParameterParser.getSenderAccount(req);
                byte[] keySeed = ParameterParser.getKeySeed(req, account.getId(), false);
                Attachment attachment = new MonetarySystemExchangeBuyAttachment(currency.getId(), rateATM, units);
                response = (JSONObject)JSONValue.parse(JSON.toString(createTransaction(req, account, attachment)));
                if (keySeed == null || "true".equalsIgnoreCase(req.getParameter("calculateFee"))) {
                    response.put("scheduled", false);
                    return response;
                }
                transaction = Transaction.newTransactionBuilder((JSONObject) response.get("transactionJSON")).build();
            } else {
                response = new JSONObject();
                transaction = ParameterParser.parseTransaction(transactionJSON, transactionBytes, prunableAttachmentJSON).build();
                JSONObject json = JSONData.unconfirmedTransaction(transaction);
                response.put("transactionJSON", json);
                try {
                    response.put("unsignedTransactionBytes", Convert.toHexString(transaction.getUnsignedBytes()));
                } catch (AplException.NotYetEncryptedException ignore) {}
                response.put("transactionBytes", Convert.toHexString(transaction.getBytes()));
                response.put("signatureHash", json.get("signatureHash"));
                response.put("transaction", transaction.getStringId());
                response.put("fullHash", transaction.getFullHashString());
            }

            MonetarySystemExchangeBuyAttachment attachment = (MonetarySystemExchangeBuyAttachment)transaction.getAttachment();
            Filter<Transaction> filter = new ExchangeOfferFilter(offerIssuerId, attachment.getCurrencyId(), attachment.getRateATM());

            lookupBlockchain().updateLock();
            try {
                transaction.validate();
                CurrencySellOffer sellOffer = CurrencySellOffer.getOffer(attachment.getCurrencyId(), offerIssuerId);
                if (sellOffer != null && sellOffer.getSupply() > 0 && sellOffer.getRateATM() <= attachment.getRateATM()) {
                    LOG.debug("Exchange offer found in blockchain, broadcasting transaction " + transaction.getStringId());
                    lookupTransactionProcessor().broadcast(transaction);
                    response.put("broadcasted", true);
                    return response;
                }
                try (DbIterator<? extends Transaction> unconfirmedTransactions = lookupTransactionProcessor().getAllUnconfirmedTransactions()) {
                    while (unconfirmedTransactions.hasNext()) {
                        if (filter.test(unconfirmedTransactions.next())) {
                            LOG.debug("Exchange offer found in unconfirmed pool, broadcasting transaction " + transaction.getStringId());
                            lookupTransactionProcessor().broadcast(transaction);
                            response.put("broadcasted", true);
                            return response;
                        }
                    }
                }
                if (API.checkPassword(req)) {
                    LOG.debug("Scheduling transaction " + transaction.getStringId());
                    TransactionScheduler.schedule(filter, transaction);
                    response.put("scheduled", true);
                } else {
                    return JSONResponses.error("No sell offer is currently available. Please try again when there is an open sell offer. " +
                            "(To schedule a buy order even in the absence of a sell offer, on a node protected by admin password, please first specify the admin password in the account settings.)");
                }
            } finally {
                lookupBlockchain().updateUnlock();
            }
            return response;

        } catch (AplException.InsufficientBalanceException e) {
            return JSONResponses.NOT_ENOUGH_FUNDS;
        }
    }

    @Override
    protected boolean requireFullClient() {
        return true;
    }

    private static class ExchangeOfferFilter implements Filter<Transaction> {

        private final long senderId;
        private final long currencyId;
        private final long rateATM;

        private ExchangeOfferFilter(long senderId, long currencyId, long rateATM) {
            this.senderId = senderId;
            this.currencyId = currencyId;
            this.rateATM = rateATM;
        }

        @Override
        public boolean test(Transaction transaction) {
            if (transaction.getSenderId() != senderId
                    || transaction.getType() != MonetarySystem.PUBLISH_EXCHANGE_OFFER
                    || transaction.getPhasing() != null) {
                return false;
            }
            MonetarySystemPublishExchangeOffer attachment = (MonetarySystemPublishExchangeOffer)transaction.getAttachment();
            if (attachment.getCurrencyId() != currencyId || attachment.getSellRateATM() > rateATM) {
                return false;
            }
            return true;
        }

    }


}
