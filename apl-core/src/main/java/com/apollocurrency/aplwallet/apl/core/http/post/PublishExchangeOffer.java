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

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.monetary.Currency;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemPublishExchangeOffer;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

/**
 * Publish exchange offer for {@link com.apollocurrency.aplwallet.apl.CurrencyType#EXCHANGEABLE} currency
 * <p>
 * Parameters
 * <ul>
 * <li>currency - currency id of an active currency
 * <li>buyRateATM - APL amount for buying a currency unit specified in ATM
 * <li>sellRateATM - APL amount for selling a currency unit specified in ATM
 * <li>initialBuySupply - Initial number of currency units offered to buy by the publisher
 * <li>initialSellSupply - Initial number of currency units offered for sell by the publisher
 * <li>totalBuyLimit - Total number of currency units which can be bought from the offer
 * <li>totalSellLimit - Total number of currency units which can be sold from the offer
 * <li>expirationHeight - Blockchain height at which the offer is expired
 * </ul>
 *
 * <p>
 * Publishing an exchange offer internally creates a buy offer and a counter sell offer linked together.
 * Typically the buyRateATM specified would be less than the sellRateATM thus allowing the publisher to make profit
 *
 * <p>
 * Each {@link CurrencyBuy} transaction which matches this offer reduces the sell supply and increases the buy supply
 * Similarly, each {@link CurrencySell} transaction which matches this offer reduces the buy supply and increases the sell supply
 * Therefore the multiple buy/sell transaction can be issued against this offer during it's lifetime.
 * However, the total buy limit and sell limit stops exchanging based on this offer after the accumulated buy/sell limit is reached
 * after possibly multiple exchange operations.
 *
 * <p>
 * Only one exchange offer is allowed per account. Publishing a new exchange offer when another exchange offer exists
 * for the account, removes the existing exchange offer and publishes the new exchange offer
 */
public final class PublishExchangeOffer extends CreateTransaction {

    private static class PublishExchangeOfferHolder {
        private static final PublishExchangeOffer INSTANCE = new PublishExchangeOffer();
    }

    public static PublishExchangeOffer getInstance() {
        return PublishExchangeOfferHolder.INSTANCE;
    }

    private PublishExchangeOffer() {
        super(new APITag[]{APITag.MS, APITag.CREATE_TRANSACTION}, "currency", "buyRateATM", "sellRateATM",
                "totalBuyLimit", "totalSellLimit", "initialBuySupply", "initialSellSupply", "expirationHeight");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        Currency currency = ParameterParser.getCurrency(req);
        long buyRateATM = ParameterParser.getLong(req, "buyRateATM", 0, Long.MAX_VALUE, true);
        long sellRateATM = ParameterParser.getLong(req, "sellRateATM", 0, Long.MAX_VALUE, true);
        long totalBuyLimit = ParameterParser.getLong(req, "totalBuyLimit", 0, Long.MAX_VALUE, true);
        long totalSellLimit = ParameterParser.getLong(req, "totalSellLimit", 0, Long.MAX_VALUE, true);
        long initialBuySupply = ParameterParser.getLong(req, "initialBuySupply", 0, Long.MAX_VALUE, true);
        long initialSellSupply = ParameterParser.getLong(req, "initialSellSupply", 0, Long.MAX_VALUE, true);
        int expirationHeight = ParameterParser.getInt(req, "expirationHeight", 0, Integer.MAX_VALUE, true);
        Account account = ParameterParser.getSenderAccount(req);

        Attachment attachment = new MonetarySystemPublishExchangeOffer(currency.getId(), buyRateATM, sellRateATM,
                totalBuyLimit, totalSellLimit, initialBuySupply, initialSellSupply, expirationHeight);
        try {
            return createTransaction(req, account, attachment);
        } catch (AplException.InsufficientBalanceException e) {
            return JSONResponses.NOT_ENOUGH_FUNDS;
        }
    }

}
