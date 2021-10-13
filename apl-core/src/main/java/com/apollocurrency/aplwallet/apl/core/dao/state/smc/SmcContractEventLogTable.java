/*
 * Copyright © 2018-2019 Apollo Foundation.
 */
package com.apollocurrency.aplwallet.apl.core.dao.state.smc;

import com.apollocurrency.aplwallet.api.v2.model.SmcContractEvent;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.SmcContractEventLogDetailsRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.SmcContractEventLogRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractEventLogEntry;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import com.apollocurrency.aplwallet.apl.util.db.DbUtils;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.Getter;

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
public class SmcContractEventLogTable extends DerivedDbTable<SmcContractEventLogEntry> {

    private static final String TABLE_NAME = "smc_event_log";

    private static final SmcContractEventLogRowMapper MAPPER = new SmcContractEventLogRowMapper();
    private static final SmcContractEventLogDetailsRowMapper detailsRowMapper = new SmcContractEventLogDetailsRowMapper();

    private final PropertiesHolder propertiesHolder;

    /**
     * Number of blocks to keep when trimming
     */
    @Getter
    private final int trimKeep;

    private final int batchCommitSize;

    /**
     * Create the account ledger table
     */
    @Inject
    public SmcContractEventLogTable(PropertiesHolder propertiesHolder,
                                    DatabaseManager databaseManager) {
        super(TABLE_NAME, databaseManager, null);
        this.propertiesHolder = propertiesHolder;
        this.batchCommitSize = propertiesHolder.BATCH_COMMIT_SIZE();
        trimKeep = propertiesHolder.getIntProperty("apl.smcEventLogTrimKeep", -1);
    }

    /**
     * Insert an entry into the table
     *
     * @param logEntry Smc event log entry
     */
    @Override
    public void insert(SmcContractEventLogEntry logEntry) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection()) {

            save(con, logEntry);

        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    protected SmcContractEventLogEntry load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return MAPPER.map(rs, null);
    }

    @Override
    public void trim(int height) {
        if (trimKeep <= 0)
            return;
        TransactionalDataSource dataSource = getDatabaseManager().getDataSource();
        try (Connection con = dataSource.getConnection();
             @DatabaseSpecificDml(DmlMarker.DELETE_WITH_LIMIT)
             PreparedStatement pstmt = con.prepareStatement("DELETE FROM " + TABLE_NAME + " WHERE height <= ? LIMIT " + batchCommitSize)) {
            pstmt.setInt(1, Math.max(height - trimKeep, 0));
            int trimmed;
            do {
                trimmed = pstmt.executeUpdate();
                dataSource.commit(false);
            } while (trimmed >= batchCommitSize);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    /**
     * Save the event log entry
     *
     * @param con Database connection
     * @throws SQLException Database error occurred
     */
    private void save(Connection con, SmcContractEventLogEntry logEntry) throws SQLException {
        try (final PreparedStatement stmt = con.prepareStatement("INSERT INTO " + TABLE_NAME +
            " (event_id, transaction_id, signature, state, tx_idx, height) " +
            "VALUES(?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            stmt.setLong(++i, logEntry.getEventId());
            stmt.setLong(++i, logEntry.getTransactionId());
            stmt.setBytes(++i, logEntry.getSignature());
            stmt.setString(++i, logEntry.getState());
            stmt.setInt(++i, logEntry.getTxIdx());
            stmt.setInt(++i, logEntry.getHeight());
            stmt.executeUpdate();
            try (final ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    logEntry.setDbId(rs.getLong(1));
                }
            }
        }
    }

    /**
     * Return a single entry identified by the log entry identifier
     *
     * @param logId Log entry identifier
     * @return Ledger entry or null if entry not found
     */
    public SmcContractEventLogEntry getEntry(long logId) {
        SmcContractEventLogEntry entry;
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE db_id = ? ";
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, logId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    entry = MAPPER.map(rs, null);
                else
                    entry = null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return entry;
    }

    /**
     * Return the event log entries sorted in descending insert order
     *
     * @param eventId   Event identifier
     * @param signature Event signature
     * @param from      First matching entry index, inclusive
     * @param to        Last matching entry index, inclusive
     * @return List of event log entries
     */
    public List<SmcContractEventLogEntry> getEntries(long eventId, byte[] signature, int from, int to) {

        List<SmcContractEventLogEntry> entryList = new ArrayList<>();
        //
        // Build the SELECT statement to search the entries
        StringBuilder sb = new StringBuilder(128);
        sb.append("SELECT * FROM " + TABLE_NAME + " WHERE event_id = ? ");
        if (signature != null) {
            sb.append("AND signature = ? ");
        }
        sb.append("ORDER BY db_id DESC ");
        sb.append(DbUtils.limitsClause(from, to));
        //
        // Get the event log entries
        //
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sb.toString())) {
            int i = 0;
            pstmt.setLong(++i, eventId);
            if (signature != null) {
                pstmt.setBytes(++i, signature);
            }
            DbUtils.setLimits(++i, pstmt, from, to);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    entryList.add(MAPPER.map(rs, null));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return entryList;
    }

    public List<SmcContractEvent> getContractsByFilter(Long contract, String name, int heightFrom, int heightTo, int from, int to, String order) {
        StringBuilder sql = new StringBuilder(
            "SELECT el.*, " +
                "e.contract, e.name, e.spec " +
                "FROM smc_event_log el " +
                "LEFT JOIN smc_event AS e ON el.event_id = e.id " +
                "WHERE e.contract = ? AND e.name = ? AND el.height <= ? ");

        if (heightTo > 0) {
            sql.append(" AND el.height <= ? ");
        }

        sql.append("ORDER BY el.db_id ").append(order);
        sql.append(DbUtils.limitsClause(from, to));

        try (Connection con = databaseManager.getDataSource().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql.toString())) {
            int i = 0;
            pstm.setLong(++i, contract);
            pstm.setString(++i, name);
            pstm.setInt(++i, heightFrom);
            if (heightTo > 0) {
                pstm.setInt(++i, heightTo);
            }
            DbUtils.setLimits(++i, pstm, from, to);
            pstm.setFetchSize(50);
            try (ResultSet rs = pstm.executeQuery()) {
                List<SmcContractEvent> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(detailsRowMapper.map(rs, null));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
