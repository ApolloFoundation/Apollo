/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.currency;

import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyFounder;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;

import java.util.stream.Stream;

public interface CurrencyFounderService {

    void addOrUpdateFounder(long currencyId, long accountId, long amount);

    CurrencyFounder getFounder(long currencyId, long accountId);

    /**
     * @deprecated see corresponding Stream method
     */
    DbIterator<CurrencyFounder> getCurrencyFounders(long currencyId, int from, int to);

    Stream<CurrencyFounder> getCurrencyFoundersStream(long currencyId, int from, int to);

    /**
     * @deprecated see corresponding Stream method
     */
    DbIterator<CurrencyFounder> getFounderCurrencies(long accountId, int from, int to);

    Stream<CurrencyFounder> getFounderCurrenciesStream(long accountId, int from, int to);

    void remove(long currencyId);

}
