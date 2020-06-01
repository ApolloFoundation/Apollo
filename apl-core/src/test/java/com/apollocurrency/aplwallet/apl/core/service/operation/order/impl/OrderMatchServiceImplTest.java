package com.apollocurrency.aplwallet.apl.core.service.operation.order.impl;

import com.apollocurrency.aplwallet.apl.core.service.operation.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.operation.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.operation.order.AskOrder;
import com.apollocurrency.aplwallet.apl.core.entity.operation.order.BidOrder;
import com.apollocurrency.aplwallet.apl.core.service.operation.order.OrderService;
import com.apollocurrency.aplwallet.apl.core.service.operation.TradeService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAskOrderPlacement;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsBidOrderPlacement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author silaev-firstbridge on 4/14/2020
 */
@ExtendWith(MockitoExtension.class)
class OrderMatchServiceImplTest {
    @Mock
    private AccountService accountService;

    @Mock
    private AccountAssetService accountAssetService;

    @Mock
    private OrderService<AskOrder, ColoredCoinsAskOrderPlacement> orderAskService;

    @Mock
    private OrderService<BidOrder, ColoredCoinsBidOrderPlacement> orderBidService;

    @Mock
    private TradeService tradeService;

    private OrderMatchServiceImpl orderMatchService;

    @BeforeEach
    void setUp() {
        this.orderMatchService = spy(
            new OrderMatchServiceImpl(
                accountService,
                accountAssetService,
                orderAskService,
                orderBidService,
                tradeService
            )
        );
    }

    @Test
    void shouldAddAskOrder() {
        //GIVEN
        final Transaction transaction = mock(Transaction.class);
        final ColoredCoinsAskOrderPlacement attachment = mock(ColoredCoinsAskOrderPlacement.class);
        final long assetId = 10L;
        when(attachment.getAssetId()).thenReturn(assetId);

        //WHEN
        orderMatchService.addAskOrder(transaction, attachment);

        //THEN
        verify(orderAskService).addOrder(transaction, attachment);
        verify(orderMatchService).matchOrders(assetId);
    }

    @Test
    void shouldAddBidOrder() {
        //GIVEN
        final Transaction transaction = mock(Transaction.class);
        final ColoredCoinsBidOrderPlacement attachment = mock(ColoredCoinsBidOrderPlacement.class);
        final long assetId = 10L;
        when(attachment.getAssetId()).thenReturn(assetId);

        //WHEN
        orderMatchService.addBidOrder(transaction, attachment);

        //THEN
        verify(orderBidService).addOrder(transaction, attachment);
        verify(orderMatchService).matchOrders(assetId);
    }

    @Test
    void shouldNotMatchOrders() {
        //GIVEN
        final long assetId = 10L;
        final AskOrder askOrder = mock(AskOrder.class);
        final BidOrder bidOrder = mock(BidOrder.class);
        when(orderAskService.getNextOrder(assetId)).thenReturn(askOrder);
        when(orderBidService.getNextOrder(assetId)).thenReturn(bidOrder);
        final long priceATMAsk = 100L;
        final long priceATMBid = 50L;
        when(askOrder.getPriceATM()).thenReturn(priceATMAsk);
        when(askOrder.getPriceATM()).thenReturn(priceATMBid);


        //WHEN
        orderMatchService.matchOrders(assetId);

        //THEN
        verifyZeroInteractions(tradeService);
        verifyZeroInteractions(accountService);
        verifyZeroInteractions(accountAssetService);
    }
}