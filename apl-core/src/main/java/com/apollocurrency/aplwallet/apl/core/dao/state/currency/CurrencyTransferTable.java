/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.currency;

import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyTransfer;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Singleton
public class CurrencyTransferTable extends EntityDbTable<CurrencyTransfer> {

    public static final LongKeyFactory<CurrencyTransfer> currencyTransferDbKeyFactory = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(CurrencyTransfer transfer) {
            if (transfer.getDbKey() == null) {
                transfer.setDbKey(super.newKey(transfer.getId()));
            }
            return transfer.getDbKey();
        }
    };

    @Inject
    public CurrencyTransferTable(DerivedTablesRegistry derivedDbTablesRegistry,
                                 DatabaseManager databaseManager,
                                 Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super("currency_transfer", currencyTransferDbKeyFactory, false, null,
            derivedDbTablesRegistry, databaseManager, null, deleteOnTrimDataEvent);
    }

    @Override
    public CurrencyTransfer load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new CurrencyTransfer(rs, dbKey);
    }

    @Override
    public void save(Connection con, CurrencyTransfer transfer) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO currency_transfer (id, currency_id, "
            + "sender_id, recipient_id, units, `timestamp`, height) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, transfer.getId());
            pstmt.setLong(++i, transfer.getCurrencyId());
            pstmt.setLong(++i, transfer.getSenderId());
            pstmt.setLong(++i, transfer.getRecipientId());
            pstmt.setLong(++i, transfer.getUnits());
            pstmt.setInt(++i, transfer.getTimestamp());
            pstmt.setInt(++i, transfer.getHeight());
            pstmt.executeUpdate();
        }
    }

    public DbIterator<CurrencyTransfer> getAccountCurrencyTransfers(long accountId, int from, int to) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        Connection con = null;
        try {
            con = dataSource.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM currency_transfer WHERE sender_id = ?"
                + " UNION ALL SELECT * FROM currency_transfer WHERE recipient_id = ? AND sender_id <> ? ORDER BY height DESC, db_id DESC"
                + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            DbUtils.setLimits(++i, pstmt, from, to);
            return this.getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public DbIterator<CurrencyTransfer> getAccountCurrencyTransfers(long accountId, long currencyId, int from, int to) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        Connection con = null;
        try {
            con = dataSource.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM currency_transfer WHERE sender_id = ? AND currency_id = ?"
                + " UNION ALL SELECT * FROM currency_transfer WHERE recipient_id = ? AND sender_id <> ? AND currency_id = ? ORDER BY height DESC, db_id DESC"
                + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, currencyId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, currencyId);
            DbUtils.setLimits(++i, pstmt, from, to);
            return this.getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

}
