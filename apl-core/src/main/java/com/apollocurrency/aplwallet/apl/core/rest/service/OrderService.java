/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.apl.core.app.Order;

import javax.inject.Singleton;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.app.CollectionUtil.toList;

@Singleton
public class OrderService {

    public List<Order.Ask> getAskOrdersByAccount(long accountId, int from, int to) {
        return toList(Order.Ask.getAskOrdersByAccount(accountId, from, to));
    }

    public List<Order.Ask> getAskOrdersByAccountAsset(long accountId, long assetId, int from, int to) {
        return toList(Order.Ask.getAskOrdersByAccountAsset(accountId, assetId, from, to));
    }
}
