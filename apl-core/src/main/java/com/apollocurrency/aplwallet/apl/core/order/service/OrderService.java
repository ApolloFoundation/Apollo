/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.order.service;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.order.entity.Order;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsOrderPlacementAttachment;

import java.util.stream.Stream;

/**
 * @author silaev-firstbridge on 4/8/2020
 */
public interface OrderService<T extends Order, C extends ColoredCoinsOrderPlacementAttachment> {

    default void insertOrDeleteOrder(VersionedDeletableEntityDbTable<T> table, long quantityATU, T order, int height) {
        if (quantityATU > 0) {
            table.insert(order);
        } else if (quantityATU == 0) {
            table.deleteAtHeight(order, height);
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
