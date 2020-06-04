/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;


import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyBuyOffer;

public class CurrencyBuyOfferTestData {

    public CurrencyBuyOffer OFFER_0 = createOffer(
        1L,     -5520700017789034517L,   9017193931881541951L,    -208393164898941117L, 1L,
        999L,        1L,      999999999,          (short)1383307,        (short)0,
        1383307,         1383308, true, false);
    public CurrencyBuyOffer OFFER_1 = createOffer(
        2L,     3697010724017064611L,    1829902366663355623L,    7477442401604846627L, 1,
        999,        1,      999999999,           (short)1383322,         (short)0,
        1383322,         1383324, true, false);

    public CurrencyBuyOffer OFFER_NEW = createNewOffer(
        3697010724017000011L,    1029902366663005623L,    7000442401604846627L, 1,
        999,        1,      999999999,           (short)1383322,         (short)0,
        1383322,         1383324, true, false);


    private CurrencyBuyOffer createOffer(
        long dbId, long id, long currencyId, long accountId, long rateATM, long limit, long supply,
        int expirationHeight, int creationHeight, short transactionIndex, int transactionHeight, int height,
        boolean latest, boolean deleted) {
        CurrencyBuyOffer offer = new CurrencyBuyOffer(id, currencyId, accountId, rateATM, limit, supply,
            expirationHeight, transactionHeight, transactionIndex, height);
        offer.setCreationHeight(creationHeight);
        offer.setDbId(dbId);
        offer.setLatest(latest);
        offer.setDeleted(deleted);
        return offer;
    }

    private CurrencyBuyOffer createNewOffer(
        long id, long currencyId, long accountId, long rateATM, long limit, long supply,
        int expirationHeight, int creationHeight, short transactionIndex, int transactionHeight, int height,
        boolean latest, boolean deleted) {
        CurrencyBuyOffer offer = new CurrencyBuyOffer(id, currencyId, accountId, rateATM, limit, supply,
            expirationHeight, transactionHeight, transactionIndex, height);
        offer.setCreationHeight(creationHeight);
//        offer.setDbId(dbId);
        offer.setLatest(latest);
        offer.setDeleted(deleted);
        return offer;
    }

}
