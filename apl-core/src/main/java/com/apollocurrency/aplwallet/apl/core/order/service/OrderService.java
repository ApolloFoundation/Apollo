/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.order.service;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.order.entity.Order;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsOrderPlacementAttachment;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Stream;

/**
 * @author silaev-firstbridge on 4/8/2020
 */
public interface OrderService<T extends Order, C extends ColoredCoinsOrderPlacementAttachment> {
    @Slf4j
    final class LogHolder {}

    default void insertOrDeleteOrder(VersionedDeletableEntityDbTable<T> table, long quantityATU, T order, int height) {
        LogHolder.log.trace(">> insertOrDeleteOrder: '{}' order: {}, height: {}", table, order, height);
        if (quantityATU > 0) {
            table.insert(order);
            LogHolder.log.trace("<< Update POSITIVE quantity = {}, height={}", order, height);
        } else if (quantityATU == 0) {
            table.deleteAtHeight(order, height);
            LogHolder.log.trace("<< Delete ZERO quantity = {}, height={}", order, height);
        } else {
            throw new IllegalArgumentException("Negative quantity: " + quantityATU
                + " for order: " + Long.toUnsignedString(order.getId()));
        }
    }

    int getCount();

    T getOrder(long orderId);

    Stream<T> getAll(int from, int to);

    Stream<T> getOrdersByAccount(long accountId, int from, int to);

    Stream<T> getOrdersByAccountAsset(final long accountId, final long assetId, int from, int to);

    Stream<T> getSortedOrders(long assetId, int from, int to);

    T getNextOrder(long assetId);

    void addOrder(Transaction transaction, C attachment);

    void removeOrder(long orderId);

    void updateQuantityATU(long quantityATU, T order);
}
