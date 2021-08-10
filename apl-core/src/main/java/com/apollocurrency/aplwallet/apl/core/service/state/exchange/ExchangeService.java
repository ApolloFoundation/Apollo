/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.exchange;

import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyExchangeOffer;
import com.apollocurrency.aplwallet.apl.core.entity.state.exchange.Exchange;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;

import java.util.List;
import java.util.stream.Stream;

public interface ExchangeService {

    Exchange addExchange(Transaction transaction, long currencyId, CurrencyExchangeOffer offer,
                         long sellerId, long buyerId, long units,
                         Block lastBlock);

    Exchange addExchange(Transaction transaction, long currencyId,
                         long offerId,
                         long sellerId, long buyerId, long units,
//                         Block lastBlock,
                         long rateATM);

    /**
     * @deprecated use corresponding Stream method version
     */
    DbIterator<Exchange> getAllExchanges(int from, int to);

    Stream<Exchange> getAllExchangesStream(int from, int to);

    int getCount();

    /**
     * @deprecated use corresponding Stream method version
     */
    DbIterator<Exchange> getCurrencyExchanges(long currencyId, int from, int to);

    Stream<Exchange> getCurrencyExchangesStream(long currencyId, int from, int to);

    List<Exchange> getLastExchanges(long[] currencyIds);

    /**
     * @deprecated use corresponding Stream method version
     */
    DbIterator<Exchange> getAccountExchanges(long accountId, int from, int to);

    Stream<Exchange> getAccountExchangesStream(long accountId, int from, int to);

    /**
     * @deprecated use corresponding Stream method version
     */
    DbIterator<Exchange> getAccountCurrencyExchanges(long accountId, long currencyId, int from, int to);

    Stream<Exchange> getAccountCurrencyExchangesStream(long accountId, long currencyId, int from, int to);

    /**
     * @deprecated use corresponding Stream method version
     */
    DbIterator<Exchange> getExchanges(long transactionId);

    Stream<Exchange> getExchangesStream(long transactionId);

    /**
     * @deprecated use corresponding Stream method version
     */
    DbIterator<Exchange> getOfferExchanges(long offerId, int from, int to);

    Stream<Exchange> getOfferExchangesStream(long offerId, int from, int to);

    int getExchangeCount(long currencyId);

}
