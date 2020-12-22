/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import java.util.List;

import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyTransfer;

public class CurrencyTransferTestData {

    public final CurrencyTransfer TRANSFER_0 = createCurrencyTransfer(
        1,    -3137476791973044178L, 4218591999071029966L, -7396849795322372927L,
        -7099351498114210634L, 100000000000L, 45754723, 615771);
    public final CurrencyTransfer TRANSFER_1 = createCurrencyTransfer(
        2,    -5531815795332533947L, -5453448652141572559L, -208393164898941117L,
        9211698109297098287L,    100,        59691252, 1400759);
    public final CurrencyTransfer TRANSFER_2 = createCurrencyTransfer(
        3,    -4785197605511459631L, -5127181510543094263L, -7396849795322372927L,
        -208393164898941117L,    200,        59691265, 1400761);
    public final CurrencyTransfer TRANSFER_3 = createCurrencyTransfer(
        4,    5100021336113941260L, -1742414430179786871L, -208393164898941117L,
        9211698109297098287L,     300,        59691275, 1400763);

    public final CurrencyTransfer TRANSFER_NEW = createNewCurrencyTransfer(
        6100021336113940060L, -1742414430179786871L, -208393164898941117L,
        9211698109297098287L,     300,        59691275, 1400763);

    public List<CurrencyTransfer> ALL_TRANSFER_ORDERED_BY_DBID = List.of(TRANSFER_0, TRANSFER_1, TRANSFER_2, TRANSFER_3);

    public CurrencyTransfer createCurrencyTransfer(long dbId, long id, long currencyId, long senderId, long recipientId, long units, int timestamp, int height) {
        CurrencyTransfer assetDelete = new CurrencyTransfer(id, currencyId, senderId, recipientId, units, timestamp, height);
        assetDelete.setDbId(dbId);
        return assetDelete;
    }

    public CurrencyTransfer createNewCurrencyTransfer(long id, long currencyId, long senderId, long recipientId, long units, int timestamp, int height) {
        return new CurrencyTransfer(id, currencyId, senderId, recipientId, units, timestamp, height);
    }

}
