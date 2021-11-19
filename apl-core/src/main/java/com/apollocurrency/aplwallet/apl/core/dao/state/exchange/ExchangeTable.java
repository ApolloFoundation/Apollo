/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.exchange;

import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LinkKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LinkKeyFactory;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import com.apollocurrency.aplwallet.apl.util.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.state.exchange.Exchange;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class ExchangeTable extends EntityDbTable<Exchange> {

    public static final LinkKeyFactory<Exchange> exchangeDbKeyFactory = new LinkKeyFactory<>("transaction_id", "offer_id") {
        @Override
        public DbKey newKey(Exchange exchange) {
            if (exchange.getDbKey() == null) {
                exchange.setDbKey(new LinkKey(exchange.getTransactionId(), exchange.getOfferId()));
            }
            return exchange.getDbKey();
        }
    };

    @Inject
    public ExchangeTable(DatabaseManager databaseManager,
                         Event<FullTextOperationData> fullTextOperationDataEvent) {
        super("exchange", exchangeDbKeyFactory, false, null,
                databaseManager, fullTextOperationDataEvent);
    }

    @Override
    public Exchange load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new Exchange(rs, dbKey);
    }

    @Override
    public void save(Connection con, Exchange exchange) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO exchange (transaction_id, currency_id, block_id, "
            + "offer_id, seller_id, buyer_id, units, rate, `timestamp`, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, exchange.getTransactionId());
            pstmt.setLong(++i, exchange.getCurrencyId());
            pstmt.setLong(++i, exchange.getBlockId());
            pstmt.setLong(++i, exchange.getOfferId());
            pstmt.setLong(++i, exchange.getSellerId());
            pstmt.setLong(++i, exchange.getBuyerId());
            pstmt.setLong(++i, exchange.getUnits());
            pstmt.setLong(++i, exchange.getRate());
            pstmt.setInt(++i, exchange.getTimestamp());
            pstmt.setInt(++i, exchange.getHeight());
            pstmt.executeUpdate();
        }
    }

    public List<Exchange> getLastExchanges(long[] currencyIds) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM exchange WHERE currency_id = ? ORDER BY height DESC, db_id DESC LIMIT 1")) {
            List<Exchange> result = new ArrayList<>();
            for (long currencyId : currencyIds) {
                pstmt.setLong(1, currencyId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        result.add(new Exchange(rs, null));
                    }
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public DbIterator<Exchange> getAccountExchanges(long accountId, int from, int to) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        Connection con = null;
        try {
            con = dataSource.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM exchange WHERE seller_id = ?"
                + " UNION ALL SELECT * FROM exchange WHERE buyer_id = ? AND seller_id <> ? ORDER BY height DESC, db_id DESC"
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

    public DbIterator<Exchange> getAccountCurrencyExchanges(long accountId, long currencyId, int from, int to) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        Connection con = null;
        try {
            con = dataSource.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM exchange WHERE seller_id = ? AND currency_id = ?"
                + " UNION ALL SELECT * FROM exchange WHERE buyer_id = ? AND seller_id <> ? AND currency_id = ? ORDER BY height DESC, db_id DESC"
                + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, currencyId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, currencyId);
            DbUtils.setLimits(++i, pstmt, from, to);
            return getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }


}
