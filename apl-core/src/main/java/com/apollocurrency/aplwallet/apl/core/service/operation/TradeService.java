/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.operation;

import com.apollocurrency.aplwallet.apl.core.entity.operation.order.AskOrder;
import com.apollocurrency.aplwallet.apl.core.entity.operation.order.BidOrder;
import com.apollocurrency.aplwallet.apl.core.entity.operation.Trade;

import java.util.List;
import java.util.stream.Stream;

public interface TradeService {

    Stream<Trade> getAllTrades(int from, int to);

    int getCount();

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
