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

package com.apollocurrency.aplwallet.apl.core.order.service.impl;

import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.order.entity.AskOrder;
import com.apollocurrency.aplwallet.apl.core.order.entity.BidOrder;
import com.apollocurrency.aplwallet.apl.core.order.service.OrderMatchService;
import com.apollocurrency.aplwallet.apl.core.order.service.OrderService;
import com.apollocurrency.aplwallet.apl.core.order.service.qualifier.AskOrderService;
import com.apollocurrency.aplwallet.apl.core.order.service.qualifier.BidOrderService;
import com.apollocurrency.aplwallet.apl.core.trade.entity.Trade;
import com.apollocurrency.aplwallet.apl.core.trade.service.TradeService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAskOrderPlacement;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsBidOrderPlacement;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author silaev-firstbridge on 4/8/2020
 */
@Singleton
public class OrderMatchServiceImpl implements OrderMatchService {
    private final AccountService accountService;
    private final AccountAssetService accountAssetService;
    private final OrderService<AskOrder, ColoredCoinsAskOrderPlacement> orderAskService;
    private final OrderService<BidOrder, ColoredCoinsBidOrderPlacement> orderBidService;
    private final TradeService tradeService;

    @Inject
    public OrderMatchServiceImpl(
        final AccountService accountService,
        final AccountAssetService accountAssetService,
        @AskOrderService final OrderService<AskOrder, ColoredCoinsAskOrderPlacement> orderAskService,
        @BidOrderService final OrderService<BidOrder, ColoredCoinsBidOrderPlacement> orderBidService,
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

        while ((askOrder = orderAskService.getNextOrder(assetId)) != null
            && (bidOrder = orderBidService.getNextOrder(assetId)) != null) {

            if (askOrder.getPriceATM() > bidOrder.getPriceATM()) {
                break;
            }

            Trade trade = tradeService.addTrade(assetId, askOrder, bidOrder);

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
        }
    }

    @Override
    public void addAskOrder(Transaction transaction, ColoredCoinsAskOrderPlacement attachment) {
        orderAskService.addOrder(transaction, attachment);
        matchOrders(attachment.getAssetId());
    }

    @Override
    public void addBidOrder(Transaction transaction, ColoredCoinsBidOrderPlacement attachment) {
        orderBidService.addOrder(transaction, attachment);
        matchOrders(attachment.getAssetId());
    }
}