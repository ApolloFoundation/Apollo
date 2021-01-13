/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencySupply;

import java.util.List;

public class CurrencySupplyTestData {

    public final CurrencySupply CURRENCY_SUPPLY_0 = createCurrencySupply(
        1,     6847297283087791598L, 20000000,      100000,
        3015,      true, false);
    public final CurrencySupply CURRENCY_SUPPLY_1 = createCurrencySupply(
        2,     7766734660591439874L, 999000000,     10,
        98888,     true, false);
    public final CurrencySupply CURRENCY_SUPPLY_2 = createCurrencySupply(
        3,     2582192196243262007L,  50000,         200,
        104087,     true, false);
    public final CurrencySupply CURRENCY_SUPPLY_3 = createCurrencySupply(
        4,     834406670971597912L, 100000,        0,
        104613,     true, false);

    public final CurrencySupply CURRENCY_SUPPLY_NEW = createNewCurrencySupply(
        6052636255477747488L, 100000,        0,
        127756,     true, false);

    public List<CurrencySupply> ALL_CURRENCY_SUPPLY_BY_ID = List.of(CURRENCY_SUPPLY_0, CURRENCY_SUPPLY_1, CURRENCY_SUPPLY_2, CURRENCY_SUPPLY_3);

    public CurrencySupply createCurrencySupply(long dbId, long currencyId, long currentSupply, long currentReservePerUnitATM, int height,
                                               boolean latest, boolean deleted) {
        CurrencySupply currencySupply = new CurrencySupply(currencyId, currentSupply, currentReservePerUnitATM, height, latest, deleted);
        currencySupply.setDbId(dbId);
        currencySupply.setLatest(latest);
        return currencySupply;
    }

    public CurrencySupply createNewCurrencySupply(long currencyId, long currentSupply, long currentReservePerUnitATM, int height,
                                                  boolean latest, boolean deleted) {
        CurrencySupply currencySupply = new CurrencySupply(currencyId, currentSupply, currentReservePerUnitATM, height, latest, deleted);
        currencySupply.setLatest(latest);
        return currencySupply;
    }

}
