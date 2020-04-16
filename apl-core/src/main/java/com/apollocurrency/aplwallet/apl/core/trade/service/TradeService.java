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

package com.apollocurrency.aplwallet.apl.core.trade.service;

import com.apollocurrency.aplwallet.apl.core.order.entity.AskOrder;
import com.apollocurrency.aplwallet.apl.core.order.entity.BidOrder;
import com.apollocurrency.aplwallet.apl.core.trade.entity.Trade;
import com.apollocurrency.aplwallet.apl.core.trade.model.TradeEvent;
import com.apollocurrency.aplwallet.apl.util.Listener;

import java.util.List;
import java.util.stream.Stream;

public interface TradeService {

    Stream<Trade> getAllTrades(int from, int to);

    int getCount();

    boolean addListener(Listener<Trade> listener, TradeEvent eventType);

    boolean removeListener(Listener<Trade> listener, TradeEvent eventType);

    Trade getTrade(long askOrderId, long bidOrderId);

    Stream<Trade> getAssetTrades(long assetId, int from, int to);

    List<Trade> getLastTrades(long[] assetIds);

    Stream<Trade> getAccountTrades(long accountId, int from, int to);

    Stream<Trade> getAccountAssetTrades(long accountId, long assetId, int from, int to);

    Stream<Trade> getAskOrderTrades(long askOrderId, int from, int to);

    Stream<Trade> getBidOrderTrades(long bidOrderId, int from, int to);

    int getTradeCount(long assetId);

    Trade addTrade(long assetId, AskOrder askOrder, BidOrder bidOrder);
}
