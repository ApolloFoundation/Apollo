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

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencySellOffer;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.model.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TransactionSchedulerService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyExchangeOfferFacade;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionWrapperHelper;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemExchangeBuyAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MSPublishExchangeOfferAttachment;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Filter;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;
import org.slf4j.Logger;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

import static org.slf4j.LoggerFactory.getLogger;


@Vetoed
public final class ScheduleCurrencyBuy extends CreateTransactionHandler {
    private static final Logger LOG = getLogger(ScheduleCurrencyBuy.class);
    private static TransactionValidator validator = CDI.current().select(TransactionValidator.class).get();
    private static GlobalSync globalSync = CDI.current().select(GlobalSync.class).get();
    private final TransactionSchedulerService transactionSchedulerService = CDI.current().select(TransactionSchedulerService.class).get();
    private final TransactionBuilderFactory transactionBuilderFactory = CDI.current().select(TransactionBuilderFactory.class).get();
    private final CurrencyExchangeOfferFacade exchangeOfferFacade = CDI.current().select(CurrencyExchangeOfferFacade.class).get();

    public ScheduleCurrencyBuy() {

        super(new APITag[]{APITag.MS, APITag.CREATE_TRANSACTION}, "currency", "rateATM", "units", "offerIssuer",
            "transactionJSON", "transactionBytes", "prunableAttachmentJSON", "adminPassword");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        String transactionJSON = Convert.emptyToNull(req.getParameter("transactionJSON"));
        String transactionBytes = Convert.emptyToNull(req.getParameter("transactionBytes"));
        String prunableAttachmentJSON = Convert.emptyToNull(req.getParameter("prunableAttachmentJSON"));
        long offerIssuerId = HttpParameterParserUtil.getAccountId(req, "offerIssuer", true);

        JSONObject response;
        Transaction transaction;
        if (transactionBytes == null && transactionJSON == null) {
            boolean broadcast = !"false".equalsIgnoreCase(req.getParameter("broadcast"));
            if (broadcast) {
                return JSONResponses.error("Must use broadcast=false to schedule a future currency buy");
            }
            Currency currency = HttpParameterParserUtil.getCurrency(req);
            long rateATM = HttpParameterParserUtil.getLong(req, "rateATM", 0, Long.MAX_VALUE, true);
            long units = HttpParameterParserUtil.getLong(req, "units", 0, Long.MAX_VALUE, true);
            Account account = HttpParameterParserUtil.getSenderAccount(req);
            byte[] keySeed = HttpParameterParserUtil.getKeySeed(req, account.getId(), false);
            Attachment attachment = new MonetarySystemExchangeBuyAttachment(currency.getId(), rateATM, units);
            response = (JSONObject) JSONValue.parse(JSON.toString(createTransaction(req, account, attachment)));
            if (keySeed == null || "true".equalsIgnoreCase(req.getParameter("calculateFee"))) {
                response.put("scheduled", false);
                return response;
            }
            transaction = transactionBuilderFactory.newTransaction((JSONObject) response.get("transactionJSON"));
        } else {
            response = new JSONObject();
            transaction = HttpParameterParserUtil.parseTransaction(transactionJSON, transactionBytes, prunableAttachmentJSON);

            Result unsignedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
            txBContext.createSerializer(transaction.getVersion())
                .serialize(TransactionWrapperHelper.createUnsignedTransaction(transaction), unsignedTxBytes);

            Result signedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
            txBContext.createSerializer(transaction.getVersion()).serialize(transaction, signedTxBytes);

            JSONObject json = JSONData.unconfirmedTransaction(transaction);
            response.put("transactionJSON", json);
            response.put("unsignedTransactionBytes", Convert.toHexString(unsignedTxBytes.array()));
            response.put("transactionBytes", Convert.toHexString(signedTxBytes.array()));
            response.put("signatureHash", json.get("signatureHash"));
            response.put("transaction", transaction.getStringId());
            response.put("fullHash", transaction.getFullHashString());
        }

        MonetarySystemExchangeBuyAttachment attachment = (MonetarySystemExchangeBuyAttachment) transaction.getAttachment();
        Filter<Transaction> filter = new ExchangeOfferFilter(offerIssuerId, attachment.getCurrencyId(), attachment.getRateATM());

        globalSync.updateLock();
        try {
            validator.validateFully(transaction);
            CurrencySellOffer sellOffer = exchangeOfferFacade.getCurrencySellOfferService()
                .getOffer(attachment.getCurrencyId(), offerIssuerId);
            if (sellOffer != null && sellOffer.getSupply() > 0 && sellOffer.getRateATM() <= attachment.getRateATM()) {
                LOG.debug("Exchange offer found in blockchain, broadcasting transaction " + transaction.getStringId());
                lookupTransactionProcessor().broadcast(transaction);
                response.put("broadcasted", true);
                return response;
            }
            for (UnconfirmedTransaction unconfirmedTransaction : CollectionUtil.toList(lookupMemPool().getAllStream())) {
                if (filter.test(unconfirmedTransaction)) {
                    LOG.debug("Exchange offer found in unconfirmed pool, broadcasting transaction " + transaction.getStringId());
                    lookupTransactionProcessor().broadcast(transaction);
                    response.put("broadcasted", true);
                }
                return response;
            }
            if (apw.checkPassword(req)) {
                LOG.debug("Scheduling transaction " + transaction.getStringId());
                transactionSchedulerService.schedule(filter, transaction);
                response.put("scheduled", true);
            } else {
                return JSONResponses.error("No sell offer is currently available. Please try again when there is an open sell offer. " +
                    "(To schedule a buy order even in the absence of a sell offer, on a node protected by admin password, please first specify the admin password in the account settings.)");
            }
        } finally {
            globalSync.updateUnlock();
        }
        return response;
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
                || transaction.getType().getSpec() != TransactionTypes.TransactionTypeSpec.MS_PUBLISH_EXCHANGE_OFFER
                || transaction.getPhasing() != null) {
                return false;
            }
            MSPublishExchangeOfferAttachment attachment = (MSPublishExchangeOfferAttachment) transaction.getAttachment();
            if (attachment.getCurrencyId() != currencyId || attachment.getSellRateATM() > rateATM) {
                return false;
            }
            return true;
        }

    }


}
