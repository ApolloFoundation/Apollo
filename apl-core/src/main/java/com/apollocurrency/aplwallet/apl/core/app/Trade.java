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
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import javax.enterprise.inject.spi.CDI;

import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.db.LinkKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.Listeners;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class Trade {

    public enum Event {
        TRADE
    }

    private static DatabaseManager databaseManager = CDI.current().select(DatabaseManager.class).get();

    private static TransactionalDataSource lookupDataSource() {
        if (databaseManager == null) {
            databaseManager = CDI.current().select(DatabaseManager.class).get();
        }
        return databaseManager.getDataSource();
    }

    private static final Listeners<Trade,Event> listeners = new Listeners<>();

    private static final LinkKeyFactory<Trade> tradeDbKeyFactory = new LinkKeyFactory<Trade>("ask_order_id", "bid_order_id") {

        @Override
        public DbKey newKey(Trade trade) {
            return trade.dbKey;
        }

    };

    private static final EntityDbTable<Trade> tradeTable = new EntityDbTable<Trade>("trade", tradeDbKeyFactory) {

        @Override
        protected Trade load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new Trade(rs, dbKey);
        }

        @Override
        protected void save(Connection con, Trade trade) throws SQLException {
            trade.save(con);
        }

    };

    public static DbIterator<Trade> getAllTrades(int from, int to) {
        return tradeTable.getAll(from, to);
    }

    public static int getCount() {
        return tradeTable.getCount();
    }

    public static boolean addListener(Listener<Trade> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Trade> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static Trade getTrade(long askOrderId, long bidOrderId) {
        return tradeTable.get(tradeDbKeyFactory.newKey(askOrderId, bidOrderId));
    }

    public static DbIterator<Trade> getAssetTrades(long assetId, int from, int to) {
        return tradeTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to);
    }

    public static List<Trade> getLastTrades(long[] assetIds) {
        try (Connection con = lookupDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM trade WHERE asset_id = ? ORDER BY asset_id, height DESC LIMIT 1")) {
            List<Trade> result = new ArrayList<>();
            for (long assetId : assetIds) {
                pstmt.setLong(1, assetId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        result.add(new Trade(rs, null));
                    }
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static DbIterator<Trade> getAccountTrades(long accountId, int from, int to) {
        Connection con = null;
        try {
            con = lookupDataSource().getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM trade WHERE seller_id = ?"
                    + " UNION ALL SELECT * FROM trade WHERE buyer_id = ? AND seller_id <> ? ORDER BY height DESC, db_id DESC"
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            DbUtils.setLimits(++i, pstmt, from, to);
            return tradeTable.getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static DbIterator<Trade> getAccountAssetTrades(long accountId, long assetId, int from, int to) {
        Connection con = null;
        try {
            con = lookupDataSource().getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM trade WHERE seller_id = ? AND asset_id = ?"
                    + " UNION ALL SELECT * FROM trade WHERE buyer_id = ? AND seller_id <> ? AND asset_id = ? ORDER BY height DESC, db_id DESC"
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, assetId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, assetId);
            DbUtils.setLimits(++i, pstmt, from, to);
            return tradeTable.getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static DbIterator<Trade> getAskOrderTrades(long askOrderId, int from, int to) {
        return tradeTable.getManyBy(new DbClause.LongClause("ask_order_id", askOrderId), from, to);
    }

    public static DbIterator<Trade> getBidOrderTrades(long bidOrderId, int from, int to) {
        return tradeTable.getManyBy(new DbClause.LongClause("bid_order_id", bidOrderId), from, to);
    }

    public static int getTradeCount(long assetId) {
        return tradeTable.getCount(new DbClause.LongClause("asset_id", assetId));
    }

    static Trade addTrade(long assetId, Order.Ask askOrder, Order.Bid bidOrder) {
        Trade trade = new Trade(assetId, askOrder, bidOrder);
        tradeTable.insert(trade);
        listeners.notify(trade, Event.TRADE);
        return trade;
    }

    static void init() {}


    private final int timestamp;
    private final long assetId;
    private final long blockId;
    private final int height;
    private final long askOrderId;
    private final long bidOrderId;
    private final int askOrderHeight;
    private final int bidOrderHeight;
    private final long sellerId;
    private final long buyerId;
    private final DbKey dbKey;
    private final long quantityATU;
    private final long priceATM;
    private final boolean isBuy;

    private Trade(long assetId, Order.Ask askOrder, Order.Bid bidOrder) {
        Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();
        Block block = blockchain.getLastBlock();
        this.blockId = block.getId();
        this.height = block.getHeight();
        this.assetId = assetId;
        this.timestamp = block.getTimestamp();
        this.askOrderId = askOrder.getId();
        this.bidOrderId = bidOrder.getId();
        this.askOrderHeight = askOrder.getHeight();
        this.bidOrderHeight = bidOrder.getHeight();
        this.sellerId = askOrder.getAccountId();
        this.buyerId = bidOrder.getAccountId();
        this.dbKey = tradeDbKeyFactory.newKey(this.askOrderId, this.bidOrderId);
        this.quantityATU = Math.min(askOrder.getQuantityATU(), bidOrder.getQuantityATU());
        this.isBuy = askOrderHeight < bidOrderHeight ||
                askOrderHeight == bidOrderHeight &&
                        (askOrder.getTransactionHeight() < bidOrder.getTransactionHeight() ||
                                (askOrder.getTransactionHeight() == bidOrder.getTransactionHeight() && askOrder.getTransactionIndex() < bidOrder.getTransactionIndex()));
        this.priceATM = isBuy ? askOrder.getPriceATM() : bidOrder.getPriceATM();
    }

    private Trade(ResultSet rs, DbKey dbKey) throws SQLException {
        this.assetId = rs.getLong("asset_id");
        this.blockId = rs.getLong("block_id");
        this.askOrderId = rs.getLong("ask_order_id");
        this.bidOrderId = rs.getLong("bid_order_id");
        this.askOrderHeight = rs.getInt("ask_order_height");
        this.bidOrderHeight = rs.getInt("bid_order_height");
        this.sellerId = rs.getLong("seller_id");
        this.buyerId = rs.getLong("buyer_id");
        this.dbKey = dbKey;
        this.quantityATU = rs.getLong("quantity");
        this.priceATM = rs.getLong("price");
        this.timestamp = rs.getInt("timestamp");
        this.height = rs.getInt("height");
        this.isBuy = rs.getBoolean("is_buy");
    }

    public Trade(int timestamp, long assetId, long blockId, int height, long askOrderId, long bidOrderId, int askOrderHeight, int bidOrderHeight, long sellerId, long buyerId, DbKey dbKey, long quantityATU, long priceATM, boolean isBuy) {
        this.timestamp = timestamp;
        this.assetId = assetId;
        this.blockId = blockId;
        this.height = height;
        this.askOrderId = askOrderId;
        this.bidOrderId = bidOrderId;
        this.askOrderHeight = askOrderHeight;
        this.bidOrderHeight = bidOrderHeight;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.dbKey = dbKey;
        this.quantityATU = quantityATU;
        this.priceATM = priceATM;
        this.isBuy = isBuy;
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO trade (asset_id, block_id, "
                + "ask_order_id, bid_order_id, ask_order_height, bid_order_height, seller_id, buyer_id, quantity, price, is_buy, timestamp, height) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.assetId);
            pstmt.setLong(++i, this.blockId);
            pstmt.setLong(++i, this.askOrderId);
            pstmt.setLong(++i, this.bidOrderId);
            pstmt.setInt(++i, this.askOrderHeight);
            pstmt.setInt(++i, this.bidOrderHeight);
            pstmt.setLong(++i, this.sellerId);
            pstmt.setLong(++i, this.buyerId);
            pstmt.setLong(++i, this.quantityATU);
            pstmt.setLong(++i, this.priceATM);
            pstmt.setBoolean(++i, this.isBuy);
            pstmt.setInt(++i, this.timestamp);
            pstmt.setInt(++i, this.height);
            pstmt.executeUpdate();
        }
    }

    public long getBlockId() { return blockId; }

    public long getAskOrderId() { return askOrderId; }

    public long getBidOrderId() { return bidOrderId; }

    public int getAskOrderHeight() {
        return askOrderHeight;
    }

    public int getBidOrderHeight() {
        return bidOrderHeight;
    }

    public long getSellerId() {
        return sellerId;
    }

    public long getBuyerId() {
        return buyerId;
    }

    public long getQuantityATU() { return quantityATU; }

    public long getPriceATM() { return priceATM; }
    
    public long getAssetId() { return assetId; }
    
    public int getTimestamp() { return timestamp; }

    public int getHeight() {
        return height;
    }

    public boolean isBuy() {
        return isBuy;
    }

    @Override
    public String toString() {
        return "Trade asset: " + Long.toUnsignedString(assetId) + " ask: " + Long.toUnsignedString(askOrderId)
                + " bid: " + Long.toUnsignedString(bidOrderId) + " price: " + priceATM + " quantity: " + quantityATU + " height: " + height;
    }

}
