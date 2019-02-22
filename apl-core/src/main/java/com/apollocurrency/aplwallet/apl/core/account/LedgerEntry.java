/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.enterprise.inject.spi.CDI;

/**
 * Ledger entry
 */
public class LedgerEntry {
    private static final Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();
    /** Ledger identifier */
    private long ledgerId = -1;
    /** Ledger event */
    private final LedgerEvent event;
    /** Associated event identifier */
    private final long eventId;
    /** Account identifier */
    private final long accountId;
    /** Holding */
    private final LedgerHolding holding;
    /** Holding identifier */
    private final Long holdingId;
    /** Change in balance */
    private long change;
    /** New balance */
    private long balance;
    /** Block identifier */
    private final long blockId;
    /** Blockchain height */
    private final int height;
    /** Block timestamp */
    private final int timestamp;

    /**
     * Create a ledger entry
     *
     * @param   event                   Event
     * @param   eventId                 Event identifier
     * @param   accountId               Account identifier
     * @param   holding                 Holding or null
     * @param   holdingId               Holding identifier or null
     * @param   change                  Change in balance
     * @param   balance                 New balance
     */
    public LedgerEntry(LedgerEvent event, long eventId, long accountId, LedgerHolding holding, Long holdingId, long change, long balance) {
        this.event = event;
        this.eventId = eventId;
        this.accountId = accountId;
        this.holding = holding;
        this.holdingId = holdingId;
        this.change = change;
        this.balance = balance;
        Block block = blockchain.getLastBlock();
        this.blockId = block.getId();
        this.height = block.getHeight();
        this.timestamp = block.getTimestamp();
    }

    /**
     * Create a ledger entry
     *
     * @param   event                   Event
     * @param   eventId                 Event identifier
     * @param   accountId               Account identifier
     * @param   change                  Change in balance
     * @param   balance                 New balance
     */
    public LedgerEntry(LedgerEvent event, long eventId, long accountId, long change, long balance) {
        this(event, eventId, accountId, null, null, change, balance);
    }

    /**
     * Create a ledger entry from a database entry
     *
     * @param   rs                      Result set
     * @throws  SQLException            Database error occurred
     */
    LedgerEntry(ResultSet rs) throws SQLException {
        ledgerId = rs.getLong("db_id");
        event = LedgerEvent.fromCode(rs.getByte("event_type"));
        eventId = rs.getLong("event_id");
        accountId = rs.getLong("account_id");
        int holdingType = rs.getByte("holding_type");
        if (holdingType >= 0) {
            holding = LedgerHolding.fromCode(holdingType);
        } else {
            holding = null;
        }
        long id = rs.getLong("holding_id");
        if (rs.wasNull()) {
            holdingId = null;
        } else {
            holdingId = id;
        }
        change = rs.getLong("change");
        balance = rs.getLong("balance");
        blockId = rs.getLong("block_id");
        height = rs.getInt("height");
        timestamp = rs.getInt("timestamp");
    }

    /**
     * Return the ledger identifier
     *
     * @return                          Ledger identifier or -1 if not set
     */
    public long getLedgerId() {
        return ledgerId;
    }

    /**
     * Return the ledger event
     *
     * @return                          Ledger event
     */
    public LedgerEvent getEvent() {
        return event;
    }

    /**
     * Return the associated event identifier
     *
     * @return                          Event identifier
     */
    public long getEventId() {
        return eventId;
    }

    /**
     * Return the account identifier
     *
     * @return                          Account identifier
     */
    public long getAccountId() {
        return accountId;
    }

    /**
     * Return the holding
     *
     * @return                          Holding or null if there is no holding
     */
    public LedgerHolding getHolding() {
        return holding;
    }

    /**
     * Return the holding identifier
     *
     * @return                          Holding identifier or null if there is no holding identifier
     */
    public Long getHoldingId() {
        return holdingId;
    }

    /**
     * Update the balance change
     *
     * @param   amount                  Change amount
     */
    void updateChange(long amount) {
        change += amount;
    }

    /**
     * Return the balance change
     *
     * @return                          Balance changes
     */
    public long getChange() {
        return change;
    }

    /**
     * Set the new balance
     *
     * @param balance                   New balance
     */
    void setBalance(long balance) {
        this.balance = balance;
    }

    /**
     * Return the new balance
     *
     * @return                          New balance
     */
    public long getBalance() {
        return balance;
    }

    /**
     * Return the block identifier
     *
     * @return                          Block identifier
     */
    public long getBlockId() {
        return blockId;
    }

    /**
     * Return the height
     *
     * @return                          Height
     */
    public int getHeight() {
        return height;
    }

    /**
     * Return the timestamp
     *
     * @return                          Timestamp
     */
    public int getTimestamp() {
        return timestamp;
    }

    /**
     * Return the hash code
     *
     * @return                          Hash code
     */
    @Override
    public int hashCode() {
        return Long.hashCode(accountId) ^ event.getCode() ^ Long.hashCode(eventId) ^ (holding != null ? holding.getCode() : 0) ^ (holdingId != null ? Long.hashCode(holdingId) : 0);
    }

    /**
     * Check if two ledger events are equal
     *
     * @param   obj                     Ledger event to check
     * @return                          TRUE if the ledger events are the same
     */
    @Override
    public boolean equals(Object obj) {
        return obj != null && (obj instanceof LedgerEntry) && accountId == ((LedgerEntry) obj).accountId && event == ((LedgerEntry) obj).event && eventId == ((LedgerEntry) obj).eventId && holding == ((LedgerEntry) obj).holding && (holdingId != null ? holdingId.equals(((LedgerEntry) obj).holdingId) : ((LedgerEntry) obj).holdingId == null);
    }

    /**
     * Save the ledger entry
     *
     * @param   con                     Database connection
     * @throws  SQLException            Database error occurred
     */
    void save(Connection con) throws SQLException {
        try (final PreparedStatement stmt = con.prepareStatement("INSERT INTO account_ledger " + "(account_id, event_type, event_id, holding_type, holding_id, change, balance, " + "block_id, height, timestamp) " + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            stmt.setLong(++i, accountId);
            stmt.setByte(++i, (byte) event.getCode());
            stmt.setLong(++i, eventId);
            if (holding != null) {
                stmt.setByte(++i, (byte) holding.getCode());
            } else {
                stmt.setByte(++i, (byte) -1);
            }
            DbUtils.setLong(stmt, ++i, holdingId);
            stmt.setLong(++i, change);
            stmt.setLong(++i, balance);
            stmt.setLong(++i, blockId);
            stmt.setInt(++i, height);
            stmt.setInt(++i, timestamp);
            stmt.executeUpdate();
            try (final ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    ledgerId = rs.getLong(1);
                }
            }
        }
    }
    
}
