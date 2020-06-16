/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import java.util.List;

import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyMint;

public class CurrencyMintTestData {

    public final CurrencyMint CURRENCY_MINT_0 = createCurrencyMint(
        11,    1000,           100,            10,       820,      true,   false);
    public final CurrencyMint CURRENCY_MINT_1 = createCurrencyMint(
        23,    1000,           200,            20,       930,      true ,  false);
    public final CurrencyMint CURRENCY_MINT_2 = createCurrencyMint(
        34,    2000,           200,            30,       1004,     true,   false);
    public final CurrencyMint CURRENCY_MINT_3 = createCurrencyMint(
        41,    2000,           100,            40,       1101,     true,   false);
    public final CurrencyMint CURRENCY_MINT_4 = createCurrencyMint(
        59,    2000,           300,            50,       1209,     true,   false);

    public final CurrencyMint CURRENCY_MINT_NEW = createNewCurrencyMint(
        3000,           400,            500,       1300,     true,   false);

    public List<CurrencyMint> ALL_TRANSFER_ORDERED_BY_DBID = List.of(CURRENCY_MINT_0, CURRENCY_MINT_1, CURRENCY_MINT_2, CURRENCY_MINT_3, CURRENCY_MINT_4);

    public CurrencyMint createCurrencyMint(long dbId, long currencyId, long accountId, long counter, int height, boolean latest, boolean deleted) {
        CurrencyMint assetDelete = new CurrencyMint(currencyId, accountId, counter, height, latest, deleted);
        assetDelete.setDbId(dbId);
        return assetDelete;
    }

    public CurrencyMint createNewCurrencyMint(long currencyId, long accountId, long counter, int height, boolean latest, boolean deleted) {
        return new CurrencyMint(currencyId, accountId, counter, height, latest, deleted);
    }

}
