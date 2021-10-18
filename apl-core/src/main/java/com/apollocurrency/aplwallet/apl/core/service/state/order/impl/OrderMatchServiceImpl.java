/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.order.impl;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.Trade;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.AskOrder;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.BidOrder;
import com.apollocurrency.aplwallet.apl.core.service.state.TradeService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.order.OrderMatchService;
import com.apollocurrency.aplwallet.apl.core.service.state.order.OrderService;
import com.apollocurrency.aplwallet.apl.core.service.state.qualifier.AskOrderService;
import com.apollocurrency.aplwallet.apl.core.service.state.qualifier.BidOrderService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.CCAskOrderPlacementAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.CCBidOrderPlacementAttachment;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author silaev-firstbridge on 4/8/2020
 */
@Slf4j
@Singleton
public class OrderMatchServiceImpl implements OrderMatchService {
    private final AccountService accountService;
    private final AccountAssetService accountAssetService;
    private final OrderService<AskOrder, CCAskOrderPlacementAttachment> orderAskService;
    private final OrderService<BidOrder, CCBidOrderPlacementAttachment> orderBidService;
    private final TradeService tradeService;

    @Inject
    public OrderMatchServiceImpl(
        final AccountService accountService,
        final AccountAssetService accountAssetService,
        @AskOrderService final OrderService<AskOrder, CCAskOrderPlacementAttachment> orderAskService,
        @BidOrderService final OrderService<BidOrder, CCBidOrderPlacementAttachment> orderBidService,
        final TradeService tradeService
    ) {
        this.accountService = accountService;
        this.accountAssetService = accountAssetService;
        this.orderAskService = orderAskService;
        this.orderBidService = orderBidService;
        this.tradeService = tradeService;
    }

    void matchOrders(long assetId) {
        AskOrder askOrder;
        BidOrder bidOrder;
        log.trace(">> match orders, assetId={}, stack={}", assetId, ThreadUtils.last5Stacktrace());
        int index = 0;
        while ((askOrder = orderAskService.getNextOrder(assetId)) != null
            && (bidOrder = orderBidService.getNextOrder(assetId)) != null) {

            log.trace(">> match orders LOOP, assetId={}, index={}, askOrder={}, bidOrder={}",
                assetId, index, askOrder, bidOrder);
            if (askOrder.getPriceATM() > bidOrder.getPriceATM()) {
                log.trace(">> match orders, STOP LOOP, assetId={}", assetId);
                break;
            }

            Trade trade = tradeService.addTrade(assetId, askOrder, bidOrder);
            log.trace("match orders TRADE, assetId={}, trade={}", assetId, trade);

            orderAskService.updateQuantityATU(Math.subtractExact(askOrder.getQuantityATU(), trade.getQuantityATU()), askOrder);
            Account askAccount = accountService.getAccount(askOrder.getAccountId());
            accountService.addToBalanceAndUnconfirmedBalanceATM(askAccount, LedgerEvent.ASSET_TRADE, askOrder.getId(),
                Math.multiplyExact(trade.getQuantityATU(), trade.getPriceATM()));
            accountAssetService.addToAssetBalanceATU(askAccount, LedgerEvent.ASSET_TRADE, askOrder.getId(), assetId, -trade.getQuantityATU());

            orderBidService.updateQuantityATU(Math.subtractExact(bidOrder.getQuantityATU(), trade.getQuantityATU()), bidOrder);
            Account bidAccount = accountService.getAccount(bidOrder.getAccountId());
            accountAssetService.addToAssetAndUnconfirmedAssetBalanceATU(bidAccount, LedgerEvent.ASSET_TRADE, bidOrder.getId(),
                assetId, trade.getQuantityATU());
            accountService.addToBalanceATM(bidAccount, LedgerEvent.ASSET_TRADE, bidOrder.getId(),
                -Math.multiplyExact(trade.getQuantityATU(), trade.getPriceATM()));
            accountService.addToUnconfirmedBalanceATM(bidAccount, LedgerEvent.ASSET_TRADE, bidOrder.getId(),
                Math.multiplyExact(trade.getQuantityATU(), (bidOrder.getPriceATM() - trade.getPriceATM())));
            log.trace("<< match orders, END LOOP, assetId={}, index={}", assetId, index);
            index++;
        }
        log.trace("<< DONE match orders, assetId={}", assetId);
    }

    @Override
    public void addAskOrder(Transaction transaction, CCAskOrderPlacementAttachment attachment) {
        orderAskService.addOrder(transaction, attachment);
        matchOrders(attachment.getAssetId());
    }

    @Override
    public void addBidOrder(Transaction transaction, CCBidOrderPlacementAttachment attachment) {
        orderBidService.addOrder(transaction, attachment);
        matchOrders(attachment.getAssetId());
    }
}