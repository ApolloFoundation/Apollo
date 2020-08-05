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

package com.apollocurrency.aplwallet.apl.core.entity.state.shuffling;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.shuffling.ShufflingParticipantTable;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDeletableEntity;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
public final class ShufflingParticipant extends VersionedDeletableEntity {
    private final long shufflingId;
    private final long accountId; // sender account
    private final int index;
    private long nextAccountId; // pointer to the next shuffling participant updated during registration
    private ShufflingParticipantState state; // tracks the state of the participant in the process
    private byte[][] blameData; // encrypted data saved as intermediate result in the shuffling process
    private byte[][] keySeeds; // to be revealed only if shuffle is being cancelled
    private byte[] dataTransactionFullHash;
    private byte[] dataHash; // hash of the processing data from ShufflingProcessingAttachment

    public ShufflingParticipant(long shufflingId, long accountId, int index, int height) {
        super(null, height);

        this.shufflingId = shufflingId;
        this.accountId = accountId;
        this.index = index;
        this.state = ShufflingParticipantState.REGISTERED;
        this.blameData = Convert.EMPTY_BYTES;
        this.keySeeds = Convert.EMPTY_BYTES;
        setDbKey(ShufflingParticipantTable.dbKeyFactory.newKey(shufflingId, accountId));
    }

    public ShufflingParticipant(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        setDbKey(dbKey);
        this.shufflingId = rs.getLong("shuffling_id");
        this.accountId = rs.getLong("account_id");
        this.nextAccountId = rs.getLong("next_account_id");
        this.index = rs.getInt("participant_index");
        this.state = ShufflingParticipantState.get(rs.getByte("state"));
        this.blameData = DbUtils.getArray(rs, "blame_data", byte[][].class, Convert.EMPTY_BYTES);
        this.keySeeds = DbUtils.getArray(rs, "key_seeds", byte[][].class, Convert.EMPTY_BYTES);
        this.dataTransactionFullHash = rs.getBytes("data_transaction_full_hash");
        this.dataHash = rs.getBytes("data_hash");
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

    public void setNextAccountId(long nextAccountId) {
        this.nextAccountId = nextAccountId;
    }

    public int getIndex() {
        return index;
    }

    public ShufflingParticipantState getState() {
        return state;
    }

    // caller must update database
    public void setState(ShufflingParticipantState state) {
        if (!this.state.canBecome(state)) {
            throw new IllegalStateException(String.format("Shuffling participant in state %s cannot go to state %s", this.state, state));
        }
        this.state = state;
        log.debug("Shuffling participant {} changed state to {}", Long.toUnsignedString(accountId), this.state);
    }

    public byte[][] getBlameData() {
        return blameData;
    }

    public byte[][] getKeySeeds() {
        return keySeeds;
    }

    public byte[] getDataTransactionFullHash() {
        return dataTransactionFullHash;
    }

    public byte[] getDataHash() {
        return dataHash;
    }

    public void setDataHash(byte[] dataHash) {
        this.dataHash = dataHash;
    }

    public void setDataTransactionFullHash(byte[] dataTransactionFullHash) {
        this.dataTransactionFullHash = dataTransactionFullHash;
    }

    public void setBlameData(byte[][] blameData) {
        this.blameData = blameData;
    }

    public void setKeySeeds(byte[][] keySeeds) {
        this.keySeeds = keySeeds;
    }
}
