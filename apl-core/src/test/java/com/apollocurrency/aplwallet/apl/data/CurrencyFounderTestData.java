/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import java.util.List;

import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyFounder;

public class CurrencyFounderTestData {

    public final CurrencyFounder CURRENCY_FOUNDER_0 = createCurrencyFounder(
        10,     6847297283087791598L, -6392448561240417498L,  100000, 98880,  true,  false);
    public final CurrencyFounder CURRENCY_FOUNDER_1 = createCurrencyFounder(
        20,     7766734660591439874L, 2650055114867906720L,   100000, 98990,  true,  false);
    public final CurrencyFounder CURRENCY_FOUNDER_2 = createCurrencyFounder(
        30,     2582192196243262007L, 5122426243196961555L,   100000, 99999,  true,  false);

    public final CurrencyFounder CURRENCY_FOUNDER_NEW = createNewCurrencyFounder(
        6847297283087791598L, 5122426243196961555L,   1000, 100990,  true, false);

    public List<CurrencyFounder> ALL_CURRENCY_FOUNDER_ORDERED_BY_DBID =
        List.of(CURRENCY_FOUNDER_0, CURRENCY_FOUNDER_1, CURRENCY_FOUNDER_2);

    public CurrencyFounder createCurrencyFounder(long dbId, long currencyId, long accountId, long amountPerUnitATM,
                                                 int height, boolean latest, boolean deleted) {
        CurrencyFounder assetDelete = new CurrencyFounder(currencyId, accountId, amountPerUnitATM, height, latest, deleted);
        assetDelete.setDbId(dbId);
        return assetDelete;
    }

    public CurrencyFounder createNewCurrencyFounder(long currencyId, long accountId, long amountPerUnitATM,
                                                    int height, boolean latest, boolean deleted) {
        return new CurrencyFounder(currencyId, accountId, amountPerUnitATM, height, latest, deleted);
    }


}
