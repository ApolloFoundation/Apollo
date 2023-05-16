package com.apollocurrency.aplwallet.apl.core.service.state.order.impl;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.AskOrder;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.BidOrder;
import com.apollocurrency.aplwallet.apl.core.service.state.TradeService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.order.OrderService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.CCAskOrderPlacementAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.CCBidOrderPlacementAttachment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    private OrderService<AskOrder, CCAskOrderPlacementAttachment> orderAskService;

    @Mock
    private OrderService<BidOrder, CCBidOrderPlacementAttachment> orderBidService;

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
        final CCAskOrderPlacementAttachment attachment = mock(CCAskOrderPlacementAttachment.class);
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
        final CCBidOrderPlacementAttachment attachment = mock(CCBidOrderPlacementAttachment.class);
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
        verifyNoInteractions(tradeService);
        verifyNoInteractions(accountService);
        verifyNoInteractions(accountAssetService);
    }
}