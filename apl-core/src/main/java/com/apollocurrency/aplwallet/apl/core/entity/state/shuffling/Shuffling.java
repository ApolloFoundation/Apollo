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

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.shuffling.ShufflingTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDeletableEntity;
import com.apollocurrency.aplwallet.apl.core.model.HoldingType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingCreationAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.db.DbUtils;
import lombok.EqualsAndHashCode;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
public final class Shuffling extends VersionedDeletableEntity {

    private final long id;
    private final long holdingId;
    private final HoldingType holdingType;
    private final long issuerId;
    private final long amount;
    private final byte participantCount;
    private short blocksRemaining;
    private byte registrantCount;
    private ShufflingStage stage;
    private long assigneeAccountId;
    private byte[][] recipientPublicKeys;

    public Shuffling(Transaction transaction, ShufflingCreationAttachment attachment, int height) {
        super(null, height);
        this.id = transaction.getId();
        this.holdingId = attachment.getHoldingId();
        this.holdingType = attachment.getHoldingType();
        this.issuerId = transaction.getSenderId();
        this.amount = attachment.getAmount();
        this.participantCount = attachment.getParticipantCount();
        this.blocksRemaining = attachment.getRegistrationPeriod();
        this.stage = ShufflingStage.REGISTRATION;
        this.assigneeAccountId = issuerId;
        this.recipientPublicKeys = Convert.EMPTY_BYTES;
        this.registrantCount = 1;

        setDbKey(ShufflingTable.dbKeyFactory.newKey(transaction.getId()));
    }

    public Shuffling(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        setDbKey(dbKey);
        this.id = rs.getLong("id");
        this.holdingId = rs.getLong("holding_id");
        this.holdingType = HoldingType.get(rs.getByte("holding_type"));
        this.issuerId = rs.getLong("issuer_id");
        this.amount = rs.getLong("amount");
        this.participantCount = rs.getByte("participant_count");
        this.blocksRemaining = rs.getShort("blocks_remaining");
        this.stage = ShufflingStage.get(rs.getByte("stage"));
        this.assigneeAccountId = rs.getLong("assignee_account_id");
        this.recipientPublicKeys = DbUtils.get2dByteArray(rs, "recipient_public_keys", Convert.EMPTY_BYTES);
        this.registrantCount = rs.getByte("registrant_count");
    }

    public Shuffling(Long dbId, long id, long holdingId, HoldingType holdingType, long issuerId, long amount, byte participantCount, short blocksRemaining, byte registrantCount, ShufflingStage stage, long assigneeAccountId, byte[][] recipientPublicKeys, Integer height) {
        super(dbId, height);
        this.id = id;
        this.holdingId = holdingId;
        this.holdingType = holdingType;
        this.issuerId = issuerId;
        this.amount = amount;
        this.participantCount = participantCount;
        this.blocksRemaining = blocksRemaining;
        this.registrantCount = registrantCount;
        this.stage = stage;
        this.assigneeAccountId = assigneeAccountId;
        this.recipientPublicKeys = recipientPublicKeys;
    }

    public long getId() {
        return id;
    }

    public long getHoldingId() {
        return holdingId;
    }

    public HoldingType getHoldingType() {
        return holdingType;
    }

    public long getIssuerId() {
        return issuerId;
    }

    public long getAmount() {
        return amount;
    }

    public byte getParticipantCount() {
        return participantCount;
    }

    public byte getRegistrantCount() {
        return registrantCount;
    }

    public short getBlocksRemaining() {
        return blocksRemaining;
    }

    public ShufflingStage getStage() {
        return stage;
    }

    public void setStage(ShufflingStage stage) {
        this.stage = stage;
    }

    /*
     * Meaning of assigneeAccountId in each shuffling stage:
     *  REGISTRATION: last currently registered participant
     *  PROCESSING: next participant in turn to submit processing data
     *  VERIFICATION: 0, not assigned to anyone
     *  BLAME: the participant who initiated the blame phase
     *  CANCELLED: the participant who got blamed for the shuffling failure, if any
     *  DONE: 0, not assigned to anyone
     */
    public long getAssigneeAccountId() {
        return assigneeAccountId;
    }

    public byte[][] getRecipientPublicKeys() {
        return recipientPublicKeys;
    }

    public void setAssigneeAccountId(long assigneeAccountId) {
        this.assigneeAccountId = assigneeAccountId;
    }

    public void setBlocksRemaining(short blocksRemaining) {
        this.blocksRemaining = blocksRemaining;
    }

    public void setRecipientPublicKeys(byte[][] recipientPublicKeys) {
        this.recipientPublicKeys = recipientPublicKeys;
    }

    public void setRegistrantCount(byte registrantCount) {
        this.registrantCount = registrantCount;
    }

    public Shuffling deepCopy() {
        return (Shuffling) super.deepCopy();
    }

    @Override
    public String toString() {
        return "Shuffling{" +
            "id=" + id +
            ", holdingId=" + holdingId +
            ", holdingType=" + holdingType +
            ", issuerId=" + issuerId +
            ", amount=" + amount +
            ", participantCount=" + participantCount +
            ", blocksRemaining=" + blocksRemaining +
            ", registrantCount=" + registrantCount +
            ", stage=" + stage +
            ", assigneeAccountId=" + assigneeAccountId +
            ", recipientPublicKeys=" + Arrays.stream(recipientPublicKeys).map(Convert::toHexString).collect(Collectors.joining(",")) +
            ", deleted=" + isDeleted() +
            ", latest=" + isLatest() +
            ", dbId=" + getDbId() +
            ", height=" + getHeight() +
            '}';
    }
}
