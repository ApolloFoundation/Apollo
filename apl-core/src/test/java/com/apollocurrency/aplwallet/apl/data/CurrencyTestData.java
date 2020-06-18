/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import java.util.List;

import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencySupply;

public class CurrencyTestData {

    private CurrencySupplyTestData supplyTestData = new CurrencySupplyTestData();

    public final Currency CURRENCY_0 = createCurrency(
        1,     6847297283087791598L, -6392448561240417498L, "Gold",      "gold",
        "GLD", "A new token allowing the easy trade of gold bullion.", 3,
        9900000000000000L, 0,            9900000000000000L, 3015,
        0,              0,                         0,               0,
        (byte)0,      (byte)0,          (byte)5,     supplyTestData.CURRENCY_SUPPLY_0,  3015,   true,   false);
    public final Currency CURRENCY_1 = createCurrency(
        2,     7766734660591439874L, 2650055114867906720L, "APOLOCENK", "apolocenk",
        "APLCN","CENK",                                                7,
        10000000L,         20000000L,     20000000L,         98888,
        98890,          100000,                    0,               0,
        (byte)0,      (byte)0,          (byte)2,      supplyTestData.CURRENCY_SUPPLY_1, 98888,  true,   false);
    public final Currency CURRENCY_2 = createCurrency(
        3,     2582192196243262007L, 5122426243196961555L, "MarioCoin", "mariocoin",
        "SMB", "Currency used in the Mushroom Kingdom, . 1UP",         1,
        25000L,            0,            25000,            104087,
        0,              0,                         0,               0,
        (byte)0,      (byte)0,          (byte)0, supplyTestData.CURRENCY_SUPPLY_2,     104087, true,   false);
    public final Currency CURRENCY_3 = createCurrency(
        4,     834406670971597912L,  6869755601928778675L, "BitcoinX",  "bitcoinx",
        "BTCX", "Bitcoins distant relative Bitcoin X",                 1,
        2100000000L,       0,            2100000000,       104613,
        0,              0,                         0,               0,
        (byte)0,      (byte)0,          (byte)2, supplyTestData.CURRENCY_SUPPLY_3,      104613, true,   false);

    public final Currency CURRENCY_NEW = createNewCurrency(
        999406670971597912L,  6869755601928778675L, "BitcoinX",  "bitcoinx",
        "BTCX", "Bitcoins distant relative Bitcoin X",                 1,
        2100000000L,       0,            2100000000,       104613,
        0,              0,                         0,               0,
        (byte)0,      (byte)0,          (byte)2, supplyTestData.CURRENCY_SUPPLY_3,      104613, true,   false);

    public List<Currency> ALL_CURRENCY_BY_ID = List.of(CURRENCY_0, CURRENCY_1, CURRENCY_2, CURRENCY_3);

    public Currency createCurrency(long dbId, long currencyId, long accountId, String name, String nameLower, String code, String description,
                                   int type, long initialSupply, long reserveSupply, long maxSupply, int creationHeight,
                                   int issuanceHeight, long minReservePerUnitATM, int minDifficulty, int maxDifficulty,
                                   byte ruleset, byte algorithm, byte decimals, CurrencySupply currencySupply, int height,
                                   boolean latest, boolean deleted) {
        Currency currency = new Currency(currencyId, accountId, name, nameLower, code, description,
        type, initialSupply, reserveSupply, maxSupply, creationHeight,
        issuanceHeight, minReservePerUnitATM, minDifficulty, maxDifficulty,
        ruleset, algorithm, decimals, currencySupply, height, latest, deleted);
        currency.setDbId(dbId);
        currency.setLatest(latest);
        return currency;
    }

    public Currency createNewCurrency(long currencyId, long accountId, String name, String nameLower, String code, String description,
                                      int type, long initialSupply, long reserveSupply, long maxSupply, int creationHeight,
                                      int issuanceHeight, long minReservePerUnitATM, int minDifficulty, int maxDifficulty,
                                      byte ruleset, byte algorithm, byte decimals, CurrencySupply currencySupply, int height,
                                      boolean latest, boolean deleted) {
        Currency currency = new Currency(currencyId, accountId, name, nameLower, code, description,
            type, initialSupply, reserveSupply, maxSupply, creationHeight,
            issuanceHeight, minReservePerUnitATM, minDifficulty, maxDifficulty,
            ruleset, algorithm, decimals, currencySupply, height, latest, deleted);
        currency.setLatest(latest);
        return currency;
    }

}
