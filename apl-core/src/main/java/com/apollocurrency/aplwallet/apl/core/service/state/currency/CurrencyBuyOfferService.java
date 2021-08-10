/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.currency;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyBuyOffer;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemPublishExchangeOffer;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;

import java.util.stream.Stream;

public interface CurrencyBuyOfferService {

    int getCount();

    CurrencyBuyOffer getOffer(long offerId);

    /**
     * @deprecated use Stream version instead
     */
    DbIterator<CurrencyBuyOffer> getAll(int from, int to);

    Stream<CurrencyBuyOffer> getAllStream(int from, int to);

    /**
     * @deprecated use Stream version instead
     */
    DbIterator<CurrencyBuyOffer> getOffers(Currency currency, int from, int to);

    Stream<CurrencyBuyOffer> getOffersStream(Currency currency, int from, int to);

    /**
     * @deprecated use Stream version instead
     */
    DbIterator<CurrencyBuyOffer> getCurrencyOffers(long currencyId, boolean availableOnly, int from, int to);

    Stream<CurrencyBuyOffer> getCurrencyOffersStream(long currencyId, boolean availableOnly, int from, int to);

    /**
     * @deprecated use Stream version instead
     */
    DbIterator<CurrencyBuyOffer> getAccountOffers(long accountId, boolean availableOnly, int from, int to);

    Stream<CurrencyBuyOffer> getAccountOffersStream(long accountId, boolean availableOnly, int from, int to);

    CurrencyBuyOffer getOffer(Currency currency, Account account);

    CurrencyBuyOffer getOffer(long currencyId, long accountId);

    /**
     * @deprecated use Stream version instead
     */
    DbIterator<CurrencyBuyOffer> getOffers(DbClause dbClause, int from, int to);

    Stream<CurrencyBuyOffer> getOffersStream(DbClause dbClause, int from, int to);

    /**
     * @deprecated use Stream version instead
     */
    DbIterator<CurrencyBuyOffer> getOffers(DbClause dbClause, int from, int to, String sort);

    Stream<CurrencyBuyOffer> getOffersStream(DbClause dbClause, int from, int to, String sort);

    void addOffer(Transaction transaction, MonetarySystemPublishExchangeOffer attachment);

    void remove(CurrencyBuyOffer buyOffer);

    void insert(CurrencyBuyOffer currencyBuyOffer);

}
