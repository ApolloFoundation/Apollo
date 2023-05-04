/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.currency;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencySellOffer;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MSPublishExchangeOfferAttachment;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;

import java.util.stream.Stream;

public interface CurrencySellOfferService {

    int getCount();

    CurrencySellOffer getOffer(long offerId);

    /**
     * @deprecated use Stream version instead
     */
    DbIterator<CurrencySellOffer> getAll(int from, int to);

    Stream<CurrencySellOffer> getAllStream(int from, int to);

    /**
     * @deprecated use Stream version instead
     */
    DbIterator<CurrencySellOffer> getOffers(Currency currency, int from, int to);

    Stream<CurrencySellOffer> getOffersStream(Currency currency, int from, int to);

    /**
     * @deprecated use Stream version instead
     */
    DbIterator<CurrencySellOffer> getCurrencyOffers(long currencyId, boolean availableOnly, int from, int to);

    Stream<CurrencySellOffer> getCurrencyOffersStream(long currencyId, boolean availableOnly, int from, int to);

    /**
     * @deprecated use Stream version instead
     */
    DbIterator<CurrencySellOffer> getAccountOffers(long accountId, boolean availableOnly, int from, int to);

    Stream<CurrencySellOffer> getAccountOffersStream(long accountId, boolean availableOnly, int from, int to);

    CurrencySellOffer getOffer(Currency currency, Account account);

    CurrencySellOffer getOffer(long currencyId, long accountId);

    /**
     * @deprecated use Stream version instead
     */
    DbIterator<CurrencySellOffer> getOffers(DbClause dbClause, int from, int to);

    Stream<CurrencySellOffer> getOffersStream(DbClause dbClause, int from, int to);

    /**
     * @deprecated use Stream version instead
     */
    DbIterator<CurrencySellOffer> getOffers(DbClause dbClause, int from, int to, String sort);

    Stream<CurrencySellOffer> getOffersStream(DbClause dbClause, int from, int to, String sort);

    void addOffer(Transaction transaction, MSPublishExchangeOfferAttachment attachment);

    void remove(CurrencySellOffer buyOffer);

    void insert(CurrencySellOffer currencySellOffer);

}
