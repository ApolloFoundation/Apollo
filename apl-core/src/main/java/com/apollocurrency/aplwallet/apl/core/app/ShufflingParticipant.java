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

import com.apollocurrency.aplwallet.apl.core.app.shuffling.ShufflingData;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.LinkKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.PrunableDbTable;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.Listeners;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.spi.CDI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

@Slf4j
public final class ShufflingParticipant {

    private BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
    private Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();
    private volatile TimeService timeService = CDI.current().select(TimeService.class).get();


    public enum State {
        REGISTERED((byte)0, new byte[]{1}),
        PROCESSED((byte)1, new byte[]{2,3}),
        VERIFIED((byte)2, new byte[]{3}),
        CANCELLED((byte)3, new byte[]{});

        private final byte code;
        private final byte[] allowedNext;

        State(byte code, byte[] allowedNext) {
            this.code = code;
            this.allowedNext = allowedNext;
        }

        static State get(byte code) {
            for (State state : State.values()) {
                if (state.code == code) {
                    return state;
                }
            }
            throw new IllegalArgumentException("No matching state for " + code);
        }

        public byte getCode() {
            return code;
        }

        public boolean canBecome(State nextState) {
            return Arrays.binarySearch(allowedNext, nextState.code) >= 0;
        }
    }

    public enum Event {
        PARTICIPANT_REGISTERED, PARTICIPANT_PROCESSED, PARTICIPANT_VERIFIED, PARTICIPANT_CANCELLED
    }

    private static final Listeners<ShufflingParticipant, Event> listeners = new Listeners<>();

    private static final LinkKeyFactory<ShufflingParticipant> shufflingParticipantDbKeyFactory = new LinkKeyFactory<ShufflingParticipant>("shuffling_id", "account_id") {

        @Override
        public DbKey newKey(ShufflingParticipant participant) {
            return participant.dbKey;
        }

    };

    private static final VersionedDeletableEntityDbTable<ShufflingParticipant> shufflingParticipantTable = new VersionedDeletableEntityDbTable<ShufflingParticipant>("shuffling_participant", shufflingParticipantDbKeyFactory) {

        @Override
        public ShufflingParticipant load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new ShufflingParticipant(rs, dbKey);
        }

        @Override
        public void save(Connection con, ShufflingParticipant participant) throws SQLException {
            participant.save(con);
        }

    };

    public static final LinkKeyFactory<ShufflingData> shufflingDataDbKeyFactory = new LinkKeyFactory<ShufflingData>("shuffling_id", "account_id") {

        @Override
        public DbKey newKey(ShufflingData shufflingData) {
            return shufflingData.getDbKey();
        }

    };

    private static final PrunableDbTable<ShufflingData> shufflingDataTable = new PrunableDbTable<>("shuffling_data", shufflingDataDbKeyFactory) {
        @Override
        public boolean isScanSafe() {
            return false; // shuffling data cannot be recovered from transactions (only by downloading/generating blocks)
        }

        @Override
        public ShufflingData load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new ShufflingData(rs, dbKey);
        }

        @Override
        public void save(Connection con, ShufflingData shufflingData) throws SQLException {
            try (
                    @DatabaseSpecificDml(DmlMarker.SET_ARRAY)
                    PreparedStatement pstmt = con.prepareStatement(
                    "INSERT INTO shuffling_data (shuffling_id, account_id, data, "
                            + "transaction_timestamp, height) "
                            + "VALUES (?, ?, ?, ?, ?)")
            ) {
                int i = 0;
                pstmt.setLong(++i, shufflingData.getShufflingId());
                pstmt.setLong(++i, shufflingData.getAccountId());
                DbUtils.setArrayEmptyToNull(pstmt, ++i, shufflingData.getData());
                pstmt.setInt(++i, shufflingData.getTransactionTimestamp());
                pstmt.setInt(++i, shufflingData.getHeight());
                pstmt.executeUpdate();
            }
        }

    };

    public static boolean addListener(Listener<ShufflingParticipant> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<ShufflingParticipant> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static DbIterator<ShufflingParticipant> getParticipants(long shufflingId) {
        return shufflingParticipantTable.getManyBy(new DbClause.LongClause("shuffling_id", shufflingId), 0, -1, " ORDER BY participant_index ");
    }

    public static ShufflingParticipant getParticipant(long shufflingId, long accountId) {
        return shufflingParticipantTable.get(shufflingParticipantDbKeyFactory.newKey(shufflingId, accountId));
    }

    static ShufflingParticipant getLastParticipant(long shufflingId) {
        return shufflingParticipantTable.getBy(new DbClause.LongClause("shuffling_id", shufflingId).and(new DbClause.NullClause("next_account_id")));
    }

    static void addParticipant(long shufflingId, long accountId, int index) {
        ShufflingParticipant participant = new ShufflingParticipant(shufflingId, accountId, index);
        shufflingParticipantTable.insert(participant);
        listeners.notify(participant, Event.PARTICIPANT_REGISTERED);
    }

    static int getVerifiedCount(long shufflingId) {
        return shufflingParticipantTable.getCount(new DbClause.LongClause("shuffling_id", shufflingId).and(
                new DbClause.ByteClause("state", State.VERIFIED.getCode())));
    }

    static void init() {}

    private final long shufflingId;
    private final long accountId; // sender account
    private final DbKey dbKey;
    private final int index;

    private long nextAccountId; // pointer to the next shuffling participant updated during registration
    private State state; // tracks the state of the participant in the process
    private byte[][] blameData; // encrypted data saved as intermediate result in the shuffling process
    private byte[][] keySeeds; // to be revealed only if shuffle is being cancelled
    private byte[] dataTransactionFullHash;
    private byte[] dataHash; // hash of the processing data from ShufflingProcessingAttachment

    private ShufflingParticipant(long shufflingId, long accountId, int index) {
        this.shufflingId = shufflingId;
        this.accountId = accountId;
        this.dbKey = shufflingParticipantDbKeyFactory.newKey(shufflingId, accountId);
        this.index = index;
        this.state = State.REGISTERED;
        this.blameData = Convert.EMPTY_BYTES;
        this.keySeeds = Convert.EMPTY_BYTES;
    }

    private ShufflingParticipant(ResultSet rs, DbKey dbKey) throws SQLException {
        this.shufflingId = rs.getLong("shuffling_id");
        this.accountId = rs.getLong("account_id");
        this.dbKey = dbKey;
        this.nextAccountId = rs.getLong("next_account_id");
        this.index = rs.getInt("participant_index");
        this.state = State.get(rs.getByte("state"));
        this.blameData = DbUtils.getArray(rs, "blame_data", byte[][].class, Convert.EMPTY_BYTES);
        this.keySeeds = DbUtils.getArray(rs, "key_seeds", byte[][].class, Convert.EMPTY_BYTES);
        this.dataTransactionFullHash = rs.getBytes("data_transaction_full_hash");
        this.dataHash = rs.getBytes("data_hash");
    }

    private void save(Connection con) throws SQLException {
        try (
                @DatabaseSpecificDml(DmlMarker.MERGE)
                PreparedStatement pstmt = con.prepareStatement("MERGE INTO shuffling_participant (shuffling_id, "
                + "account_id, next_account_id, participant_index, state, blame_data, key_seeds, data_transaction_full_hash, data_hash, height, latest) "
                + "KEY (shuffling_id, account_id, height) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")
        ) {
            int i = 0;
            pstmt.setLong(++i, this.shufflingId);
            pstmt.setLong(++i, this.accountId);
            DbUtils.setLongZeroToNull(pstmt, ++i, this.nextAccountId);
            pstmt.setInt(++i, this.index);
            pstmt.setByte(++i, this.getState().getCode());
            DbUtils.setArrayEmptyToNull(pstmt, ++i, this.blameData);
            DbUtils.setArrayEmptyToNull(pstmt, ++i, this.keySeeds);
            DbUtils.setBytes(pstmt, ++i, this.dataTransactionFullHash);
            DbUtils.setBytes(pstmt, ++i, this.dataHash);
            pstmt.setInt(++i, blockchain.getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getShufflingId() {
        return shufflingId;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getNextAccountId() {
        return nextAccountId;
    }

    void setNextAccountId(long nextAccountId) {
        if (this.nextAccountId != 0) {
            throw new IllegalStateException("nextAccountId already set to " + Long.toUnsignedString(this.nextAccountId));
        }
        this.nextAccountId = nextAccountId;
        shufflingParticipantTable.insert(this);
    }

    public int getIndex() {
        return index;
    }

    public State getState() {
        return state;
    }

    // caller must update database
    private void setState(State state) {
        if (!this.state.canBecome(state)) {
            throw new IllegalStateException(String.format("Shuffling participant in state %s cannot go to state %s", this.state, state));
        }
        this.state = state;
        log.debug("Shuffling participant {} changed state to {}", Long.toUnsignedString(accountId), this.state);
    }

    public byte[][] getData() {
        return getData(shufflingId, accountId);
    }

    public static byte[][] getData(long shufflingId, long accountId) {
        ShufflingData shufflingData = shufflingDataTable.get(shufflingDataDbKeyFactory.newKey(shufflingId, accountId));
        return shufflingData != null ? shufflingData.getData() : null;
    }

    void setData(byte[][] data, int timestamp) {
        if (data != null && timeService.getEpochTime() - timestamp < blockchainConfig.getMaxPrunableLifetime() && getData() == null) {
            shufflingDataTable.insert(new ShufflingData(shufflingId, accountId, data, timestamp, blockchain.getHeight()));
        }
    }

    public static void restoreData(long shufflingId, long accountId, byte[][] data, int timestamp, int height) {
        if (data != null && getData(shufflingId, accountId) == null) {
            shufflingDataTable.insert(new ShufflingData(shufflingId, accountId, data, timestamp, height));
        }
    }

    public byte[][] getBlameData() {
        return blameData;
    }

    public byte[][] getKeySeeds() {
        return keySeeds;
    }

    void cancel(byte[][] blameData, byte[][] keySeeds) {
        if (this.keySeeds.length > 0) {
            throw new IllegalStateException("keySeeds already set");
        }
        this.blameData = blameData;
        this.keySeeds = keySeeds;
        setState(State.CANCELLED);
        shufflingParticipantTable.insert(this);
        listeners.notify(this, Event.PARTICIPANT_CANCELLED);
    }

    public byte[] getDataTransactionFullHash() {
        return dataTransactionFullHash;
    }

    public byte[] getDataHash() {
        return dataHash;
    }

    void setProcessed(byte[] dataTransactionFullHash, byte[] dataHash) {
        if (this.dataTransactionFullHash != null) {
            throw new IllegalStateException("dataTransactionFullHash already set");
        }
        setState(State.PROCESSED);
        this.dataTransactionFullHash = dataTransactionFullHash;
        if (dataHash != null) {
            setDataHash(dataHash);
        }
        shufflingParticipantTable.insert(this);
        listeners.notify(this, Event.PARTICIPANT_PROCESSED);
    }

    private void setDataHash(byte[] dataHash) {
        if (this.dataHash != null) {
            throw new IllegalStateException("dataHash already set");
        }
        this.dataHash = dataHash;
    }

    public ShufflingParticipant getPreviousParticipant() {
        if (index == 0) {
            return null;
        }
        return shufflingParticipantTable.getBy(new DbClause.LongClause("shuffling_id", shufflingId).and(new DbClause.IntClause("participant_index", index - 1)));
    }

    void verify() {
        setState(State.VERIFIED);
        shufflingParticipantTable.insert(this);
        listeners.notify(this, Event.PARTICIPANT_VERIFIED);
    }

    void delete() {
        shufflingParticipantTable.delete(this);
    }

}
