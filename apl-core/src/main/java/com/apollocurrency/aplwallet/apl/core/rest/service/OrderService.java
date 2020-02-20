/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.apl.core.app.Order;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class OrderService {

    public List<Order.Ask> getAskOrdersByAccount(long accountId, int from, int to){
        List<Order.Ask> orders = new ArrayList<>();
        try(DbIterator<Order.Ask> askOrders = Order.Ask.getAskOrdersByAccount(accountId, from, to)){
            askOrders.forEach(orders::add);
        }
        return orders;
    }

    public List<Order.Ask> getAskOrdersByAccountAsset(long accountId, long assetId, int from, int to){
        List<Order.Ask> orders = new ArrayList<>();
        try(DbIterator<Order.Ask> askOrders = Order.Ask.getAskOrdersByAccountAsset(accountId, assetId, from, to)){
            askOrders.forEach(orders::add);
        }
        return orders;
    }
}
