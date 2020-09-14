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

package com.apollocurrency.aplwallet.apl.core.dao.state;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LinkKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.state.Trade;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class TradeTable extends EntityDbTable<Trade> {
    public static final LinkKeyFactory<Trade> TRADE_DB_KEY_FACTORY = new LinkKeyFactory<>("ask_order_id", "bid_order_id") {
        @Override
        public DbKey newKey(Trade trade) {
            if (trade.getDbKey() == null) {
                trade.setDbKey(newKey(trade.getAskOrderId(), trade.getBidOrderId()));
            }
            return newKey(trade.getAskOrderId(), trade.getBidOrderId());
        }
    };

    @Inject
    public TradeTable(DerivedTablesRegistry derivedDbTablesRegistry,
                      DatabaseManager databaseManager,
                      Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super("trade", TRADE_DB_KEY_FACTORY, false, null,
            derivedDbTablesRegistry, databaseManager, null, deleteOnTrimDataEvent);
    }

    public void save(final Connection con, final Trade trade) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO trade (asset_id, block_id, "
            + "ask_order_id, bid_order_id, ask_order_height, bid_order_height, seller_id, buyer_id, quantity, price, is_buy, `timestamp`, height) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, trade.getAssetId());
            pstmt.setLong(++i, trade.getBlockId());
            pstmt.setLong(++i, trade.getAskOrderId());
            pstmt.setLong(++i, trade.getBidOrderId());
            pstmt.setInt(++i, trade.getAskOrderHeight());
            pstmt.setInt(++i, trade.getBidOrderHeight());
            pstmt.setLong(++i, trade.getSellerId());
            pstmt.setLong(++i, trade.getBuyerId());
            pstmt.setLong(++i, trade.getQuantityATU());
            pstmt.setLong(++i, trade.getPriceATM());
            pstmt.setBoolean(++i, trade.isBuy());
            pstmt.setInt(++i, trade.getTimestamp());
            pstmt.setInt(++i, trade.getHeight());
            pstmt.executeUpdate();
        }
    }

    @Override
    public Trade load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new Trade(rs, dbKey);
    }

    public List<Trade> getLastTrades(final DataSource dataSource, final long[] assetIds) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM trade WHERE asset_id = ? ORDER BY asset_id, height DESC LIMIT 1")) {
            final List<Trade> result = new ArrayList<>();
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

    public DbIterator<Trade> getAccountTrades(
        final DataSource dataSource,
        final long accountId,
        final int from,
        final int to
    ) {
        Connection con = null;
        try {
            con = dataSource.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM trade WHERE seller_id = ?"
                + " UNION ALL SELECT * FROM trade WHERE buyer_id = ? AND seller_id <> ? ORDER BY height DESC, db_id DESC"
                + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            DbUtils.setLimits(++i, pstmt, from, to);
            return getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public DbIterator<Trade> getAccountAssetTrades(
        final DataSource dataSource,
        final long accountId,
        final long assetId,
        final int from,
        final int to
    ) {
        Connection con = null;
        try {
            con = dataSource.getConnection();
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
            return getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public Trade getTrade(long askOrderId, long bidOrderId) {
        return get(TRADE_DB_KEY_FACTORY.newKey(askOrderId, bidOrderId));
    }

    public DbKey getDbKey(long askOrderId, long bidOrderId) {
        return TRADE_DB_KEY_FACTORY.newKey(askOrderId, bidOrderId);
    }
}
