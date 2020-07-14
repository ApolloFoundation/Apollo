/*
 * Copyright © 2018-2019 Apollo Foundation.
 */
package com.apollocurrency.aplwallet.apl.core.entity.state.account;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Ledger entry
 */
@ToString(callSuper = true)
@Getter
@Setter
public class LedgerEntry extends DerivedEntity {
    /**
     * Ledger event
     */
    private final LedgerEvent event;
    /**
     * Associated event identifier
     */
    private final long eventId;
    /**
     * Account identifier
     */
    private final long accountId;
    /**
     * Holding
     */
    private final LedgerHolding holding;
    /**
     * Holding identifier
     */
    private final Long holdingId;
    /**
     * Block identifier
     */
    private final long blockId;
    /**
     * Block timestamp
     */
    private final int timestamp;
    /**
     * Ledger identifier
     */
    private long ledgerId = -1;
    /**
     * Change in balance
     */
    private long change;
    /**
     * New balance
     */
    private long balance;

    /**
     * Create a ledger entry
     *
     * @param event     Event
     * @param eventId   Event identifier
     * @param accountId Account identifier
     * @param holding   Holding or null
     * @param holdingId Holding identifier or null
     * @param change    Change in balance
     * @param balance   New balance
     * @param lastBlock Last block in blockchain
     */
    public LedgerEntry(LedgerEvent event, long eventId, long accountId, LedgerHolding holding, Long holdingId, long change, long balance, Block lastBlock) {
        this(event, eventId, accountId, holding, holdingId, change, balance, lastBlock.getId(), lastBlock.getTimestamp(), lastBlock.getHeight());
    }

    /**
     * Create a ledger entry
     *
     * @param event          Event
     * @param eventId        Event identifier
     * @param accountId      Account identifier
     * @param holding        Holding or null
     * @param holdingId      Holding identifier or null
     * @param change         Change in balance
     * @param balance        New balance
     * @param lastBlockId    Last block Id in the blockchain
     * @param blockTimeStamp Last block timestamp
     * @param height         the blockchain height
     */
    public LedgerEntry(LedgerEvent event, long eventId, long accountId, LedgerHolding holding, Long holdingId, long change, long balance, long lastBlockId, int blockTimeStamp, int height) {
        super(null, height);
        this.event = event;
        this.eventId = eventId;
        this.accountId = accountId;
        this.holding = holding;
        this.holdingId = holdingId;
        this.change = change;
        this.balance = balance;
        this.blockId = lastBlockId;
        this.timestamp = blockTimeStamp;
    }

    /**
     * Create a ledger entry from a database entry
     *
     * @param rs Result set
     * @throws SQLException Database error occurred
     */
    public LedgerEntry(ResultSet rs) throws SQLException {
        super(rs);
        ledgerId = getDbId();
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
        timestamp = rs.getInt("timestamp");
    }

    /**
     * Update the balance change
     *
     * @param amount Change amount
     */
    public void updateChange(long amount) {
        change += amount;
    }

    /**
     * Return the hash code
     *
     * @return Hash code
     */
    @Override
    public int hashCode() {
        return Long.hashCode(accountId) ^ event.getCode() ^ Long.hashCode(eventId) ^ (holding != null ? holding.getCode() : 0) ^ (holdingId != null ? Long.hashCode(holdingId) : 0);
    }

    /**
     * Check if two ledger events are equal
     *
     * @param obj Ledger event to check
     * @return TRUE if the ledger events are the same
     */
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof LedgerEntry) && accountId == ((LedgerEntry) obj).accountId && event == ((LedgerEntry) obj).event && eventId == ((LedgerEntry) obj).eventId && holding == ((LedgerEntry) obj).holding && (Objects.equals(holdingId, ((LedgerEntry) obj).holdingId));
    }
}
