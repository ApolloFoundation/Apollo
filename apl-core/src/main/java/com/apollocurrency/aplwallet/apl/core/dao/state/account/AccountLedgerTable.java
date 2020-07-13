/*
 * Copyright © 2018-2019 Apollo Foundation.
 */
package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.model.account.LedgerHolding;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEntry;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Account ledger table
 */
@Singleton
public class AccountLedgerTable extends DerivedDbTable<LedgerEntry> {

    private final PropertiesHolder propertiesHolder;

    /**
     * Number of blocks to keep when trimming
     */
    private int trimKeep;

    /**
     * Create the account ledger table
     */
    @Inject
    public AccountLedgerTable(PropertiesHolder propertiesHolder) {
        super("account_ledger");
        this.propertiesHolder = propertiesHolder;
        trimKeep = propertiesHolder.getIntProperty("apl.ledgerTrimKeep", 30000);
    }

    /**
     * Insert an entry into the table
     *
     * @param ledgerEntry Ledger entry
     */
    @Override
    public void insert(LedgerEntry ledgerEntry) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection()) {

            save(con, ledgerEntry);

        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    protected LedgerEntry load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        throw new UnsupportedOperationException("Method is not implemented yet");
    }


    /**
     * Trim the account ledger table
     *
     * @param height Trim height
     */
    @Override
    public void trim(int height) {
        if (trimKeep <= 0)
            return;
        TransactionalDataSource dataSource = getDatabaseManager().getDataSource();
        try (Connection con = dataSource.getConnection();
             @DatabaseSpecificDml(DmlMarker.DELETE_WITH_LIMIT)
             PreparedStatement pstmt = con.prepareStatement("DELETE FROM account_ledger WHERE height <= ? LIMIT " + propertiesHolder.BATCH_COMMIT_SIZE())) {
            pstmt.setInt(1, Math.max(height - trimKeep, 0));
            int trimmed;
            do {
                trimmed = pstmt.executeUpdate();
                dataSource.commit(false);
            } while (trimmed >= propertiesHolder.BATCH_COMMIT_SIZE());
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    /**
     * Save the ledger entry
     *
     * @param con Database connection
     * @throws SQLException Database error occurred
     */
    private void save(Connection con, LedgerEntry ledgerEntry) throws SQLException {
        try (final PreparedStatement stmt = con.prepareStatement("INSERT INTO account_ledger " +
            "(account_id, event_type, event_id, holding_type, holding_id, change, balance, block_id, height, timestamp) " +
            "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            stmt.setLong(++i, ledgerEntry.getAccountId());
            stmt.setByte(++i, (byte) ledgerEntry.getEvent().getCode());
            stmt.setLong(++i, ledgerEntry.getEventId());
            if (ledgerEntry.getHolding() != null) {
                stmt.setByte(++i, (byte) ledgerEntry.getHolding().getCode());
            } else {
                stmt.setByte(++i, (byte) -1);
            }
            DbUtils.setLong(stmt, ++i, ledgerEntry.getHoldingId());
            stmt.setLong(++i, ledgerEntry.getChange());
            stmt.setLong(++i, ledgerEntry.getBalance());
            stmt.setLong(++i, ledgerEntry.getBlockId());
            stmt.setInt(++i, ledgerEntry.getHeight());
            stmt.setInt(++i, ledgerEntry.getTimestamp());
            stmt.executeUpdate();
            try (final ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    ledgerEntry.setLedgerId(rs.getLong(1));
                }
            }
        }
    }

    public int getTrimKeep() {
        return trimKeep;
    }

    /**
     * Return a single entry identified by the ledger entry identifier
     *
     * @param ledgerId     Ledger entry identifier
     * @param allowPrivate Allow requested ledger entry to belong to private transaction or not
     * @return Ledger entry or null if entry not found
     */
    public LedgerEntry getEntry(long ledgerId, boolean allowPrivate) {

        LedgerEntry entry;
        String sql = "SELECT * FROM account_ledger WHERE db_id = ? ";
        if (!allowPrivate) {
            sql += " AND event_id NOT IN (select event_id from account_ledger where event_type = ? ) ";
        }
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, ledgerId);
            if (!allowPrivate) {
                stmt.setInt(2, LedgerEvent.PRIVATE_PAYMENT.code);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    entry = new LedgerEntry(rs);
                else
                    entry = null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return entry;
    }

    /**
     * Return the ledger entries sorted in descending insert order
     *
     * @param accountId      Account identifier or zero if no account identifier
     * @param event          Ledger event or null
     * @param eventId        Ledger event identifier or zero if no event identifier
     * @param holding        Ledger holding or null
     * @param holdingId      Ledger holding identifier or zero if no holding identifier
     * @param firstIndex     First matching entry index, inclusive
     * @param lastIndex      Last matching entry index, inclusive
     * @param includePrivate Boolean flag that specifies, should response include private ledger entries or not
     * @return List of ledger entries
     */
    public List<LedgerEntry> getEntries(long accountId, LedgerEvent event, long eventId,
                                        LedgerHolding holding, long holdingId,
                                        int firstIndex, int lastIndex, boolean includePrivate) {

        List<LedgerEntry> entryList = new ArrayList<>();
        //
        // Build the SELECT statement to search the entries
        StringBuilder sb = new StringBuilder(128);
        sb.append("SELECT * FROM account_ledger ");
        if (accountId != 0 || event != null || holding != null || !includePrivate) {
            sb.append("WHERE ");
        }
        if (accountId != 0) {
            sb.append("account_id = ? ");
        }
        if (!includePrivate && event == LedgerEvent.PRIVATE_PAYMENT) {
            throw new RuntimeException("None of private ledger entries should be retrieved!");
        }
        if (!includePrivate) {
            if (accountId != 0) {
                sb.append("AND ");
            }
            sb.append("event_id not in (select event_id from account_ledger where ");
            if (accountId != 0) {
                sb.append("account_id = ? AND ");
            }
            sb.append("event_type = ? ) ");
        }
        if (event != null) {
            if (accountId != 0 || !includePrivate) {
                sb.append("AND ");
            }
            sb.append("event_type = ? ");
            if (eventId != 0)
                sb.append("AND event_id = ? ");
        }
        if (holding != null) {
            if (accountId != 0 || event != null || !includePrivate) {
                sb.append("AND ");
            }
            sb.append("holding_type = ? ");
            if (holdingId != 0)
                sb.append("AND holding_id = ? ");
        }
        sb.append("ORDER BY db_id DESC ");
        sb.append(DbUtils.limitsClause(firstIndex, lastIndex));
        //
        // Get the ledger entries
        //
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sb.toString())) {
            int i = 0;
            if (accountId != 0) {
                pstmt.setLong(++i, accountId);
            }
            if (!includePrivate) {
                if (accountId != 0) {
                    pstmt.setLong(++i, accountId);
                }
                pstmt.setInt(++i, LedgerEvent.PRIVATE_PAYMENT.code);
            }
            if (event != null) {
                pstmt.setByte(++i, (byte) event.getCode());
                if (eventId != 0) {
                    pstmt.setLong(++i, eventId);
                }
            }
            if (holding != null) {
                pstmt.setByte(++i, (byte) holding.getCode());
                if (holdingId != 0) {
                    pstmt.setLong(++i, holdingId);
                }
            }
            DbUtils.setLimits(++i, pstmt, firstIndex, lastIndex);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    entryList.add(new LedgerEntry(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return entryList;
    }
}
