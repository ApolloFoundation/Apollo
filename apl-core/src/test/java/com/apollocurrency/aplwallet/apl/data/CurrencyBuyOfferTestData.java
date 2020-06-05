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
        2L,     3697010724017064611L,    -4132128809614485872L,    7477442401604846627L, 1,
        999,        1,      999999999,           (short)1383322,         (short)0,
        1383322,         1383324, true, false);
    public CurrencyBuyOffer OFFER_2 = createOffer(
        3L,     1046772637338198685L,    -4132128809614485872L,   -208393164898941117L, 1,
        999,        1,      999999999,          (short)1383359,        (short)0,
        1383359,         1383360, true, false);
    public CurrencyBuyOffer OFFER_3 = createOffer(
        4L,     -9125532320230757097L,   -4649061333745309738L,   7477442401604846627L, 1,
        999,        1,      999999999,          (short)1383373,        (short)0,
        1383373,         1383376, true, false);
    public CurrencyBuyOffer OFFER_4 = createOffer(
        5L,     -4337072943953941839L,   4231781207816121683L,    -208393164898941117L, 1,
        999,        1,      999999999,          (short)1383416,        (short)0,
        1383416,         1383418, true, false);
    public CurrencyBuyOffer OFFER_5 = createOffer(
        6L,     8038994817996483094L,    -8186806310139197L,      7477442401604846627L, 1,
        999,        1,      999999999,          (short)1383432,        (short)0,
        1383432,         1383435, true, false);
    public CurrencyBuyOffer OFFER_6 = createOffer(
        7L,     5036980205787824994L,    -3205373316822570812L,   -208393164898941117L, 1,
        999,        1,      999999999,          (short)1400531,        (short)0,
        1400531,         1400532, true, false);
    public CurrencyBuyOffer OFFER_7 = createOffer(
        8L,     7703713759586800965L,    -3773624717939326451L,   7477442401604846627L, 1,
        999,        1,      999999999,          (short)1400544,        (short)0,
        1400544,         1400545, true, false);
    public CurrencyBuyOffer OFFER_8 = createOffer(
        9L,     -4255505590921443908L,   -3205373316822570812L,   -208393164898941117L, 1,
        999,        1,      999999999,          (short)1400579,        (short)0,
        1400579,         1400580, true, false);

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
