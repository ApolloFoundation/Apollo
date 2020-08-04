/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.exchange;

import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.exchange.ExchangeRequest;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemExchangeBuyAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemExchangeSell;

import java.util.stream.Stream;

public interface ExchangeRequestService {

    /**
     * @deprecated see corresponding Stream method
     */
    DbIterator<ExchangeRequest> getAllExchangeRequests(int from, int to);

    Stream<ExchangeRequest> getAllExchangeRequestsStream(int from, int to);

    int getCount();

    ExchangeRequest getExchangeRequest(long transactionId);

    /**
     * @deprecated see corresponding Stream method
     */
    DbIterator<ExchangeRequest> getCurrencyExchangeRequests(long currencyId, int from, int to);

    Stream<ExchangeRequest> getCurrencyExchangeRequestsStream(long currencyId, int from, int to);

    /**
     * @deprecated see corresponding Stream method
     */
    DbIterator<ExchangeRequest> getAccountExchangeRequests(long accountId, int from, int to);

    Stream<ExchangeRequest> getAccountExchangeRequestsStream(long accountId, int from, int to);

    /**
     * @deprecated see corresponding Stream method
     */
    DbIterator<ExchangeRequest> getAccountCurrencyExchangeRequests(long accountId, long currencyId, int from, int to);

    Stream<ExchangeRequest> getAccountCurrencyExchangeRequestsStream(long accountId, long currencyId, int from, int to);

    void addExchangeRequest(Transaction transaction, MonetarySystemExchangeBuyAttachment attachment);

    void addExchangeRequest(Transaction transaction, MonetarySystemExchangeSell attachment);

}
