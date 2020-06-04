/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.service.state;

import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.AskOrder;
import com.apollocurrency.aplwallet.apl.core.service.state.order.impl.AskOrderServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.order.OrderService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAskOrderPlacement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author silaev-firstbridge on 4/15/2020
 */
class OrderServiceTest {
    private final OrderService<AskOrder, ColoredCoinsAskOrderPlacement> orderService =
        new AskOrderServiceImpl(
            null, null, null, null
        );

    @Test
    void shouldInsertOrDeleteOrderWhenQuantityATUMoreThanZero() {
        //GIVEN
        final long quantityATU = 300;
        final AskOrder order = mock(AskOrder.class);
        final int height = 1040;
        @SuppressWarnings("unchecked") final VersionedDeletableEntityDbTable<AskOrder> table =
            mock(VersionedDeletableEntityDbTable.class);

        //WHEN
        orderService.insertOrDeleteOrder(table, quantityATU, order, height);

        //THEN
        verify(table).insert(order);
    }

    @Test
    void shouldInsertOrDeleteOrderWhenQuantityATUIsZero() {
        //GIVEN
        final long quantityATU = 0;
        final AskOrder order = mock(AskOrder.class);
        final int height = 1040;
        @SuppressWarnings("unchecked") final VersionedDeletableEntityDbTable<AskOrder> table =
            mock(VersionedDeletableEntityDbTable.class);

        //WHEN
        orderService.insertOrDeleteOrder(table, quantityATU, order, height);

        //THEN
        verify(table).deleteAtHeight(order, height);
    }

    @Test
    void shouldNotInsertOrDeleteOrderWhenQuantityLessThanZero() {
        //GIVEN
        final long quantityATU = -300;
        final AskOrder order = mock(AskOrder.class);
        final int height = 1040;
        @SuppressWarnings("unchecked") final VersionedDeletableEntityDbTable<AskOrder> table =
            mock(VersionedDeletableEntityDbTable.class);

        //WHEN
        final Executable executable = () -> orderService.insertOrDeleteOrder(table, quantityATU, order, height);

        //THEN
        assertThrows(IllegalArgumentException.class, executable);
    }
}