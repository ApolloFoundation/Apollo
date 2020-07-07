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

import com.apollocurrency.aplwallet.apl.core.dao.state.shuffling.ShufflingTable;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.core.monetary.HoldingType;
import com.apollocurrency.aplwallet.apl.core.service.state.ShufflingService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingCreation;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import javax.enterprise.inject.spi.CDI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public final class Shuffling extends DerivedEntity {

    private final long id;
    private final long holdingId;
    private final HoldingType holdingType;
    private final long issuerId;
    private final long amount;
    private final byte participantCount;
    private short blocksRemaining;
    private byte registrantCount;
    private Stage stage;
    private long assigneeAccountId;
    private byte[][] recipientPublicKeys;

    public Shuffling(Transaction transaction, ShufflingCreation attachment, int height) {
        super(null, height);
        this.id = transaction.getId();
        this.holdingId = attachment.getHoldingId();
        this.holdingType = attachment.getHoldingType();
        this.issuerId = transaction.getSenderId();
        this.amount = attachment.getAmount();
        this.participantCount = attachment.getParticipantCount();
        this.blocksRemaining = attachment.getRegistrationPeriod();
        this.stage = Stage.REGISTRATION;
        this.assigneeAccountId = issuerId;
        this.recipientPublicKeys = Convert.EMPTY_BYTES;
        this.registrantCount = 1;

        setHeight(transaction.getHeight());
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
        this.stage = Stage.get(rs.getByte("stage"));
        this.assigneeAccountId = rs.getLong("assignee_account_id");
        this.recipientPublicKeys = DbUtils.getArray(rs, "recipient_public_keys", byte[][].class, Convert.EMPTY_BYTES);
        this.registrantCount = rs.getByte("registrant_count");
        setHeight(rs.getInt("height"));
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

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
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

    public byte[] getStateHash() {
        return stage.getHash(this);
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




    public enum Event {
        SHUFFLING_CREATED, SHUFFLING_PROCESSING_ASSIGNED, SHUFFLING_PROCESSING_FINISHED, SHUFFLING_BLAME_STARTED, SHUFFLING_CANCELLED, SHUFFLING_DONE
    }

    // TODO: YL remove static instance later
    private static ShufflingService shufflingService = CDI.current().select(ShufflingService.class).get();

    public enum Stage {
        REGISTRATION((byte) 0, new byte[]{1, 4}) {
            @Override
            byte[] getHash(Shuffling shuffling) {
                return shufflingService.getFullHash(shuffling.getId());
            }
        },
        PROCESSING((byte) 1, new byte[]{2, 3, 4}) {
            @Override
            byte[] getHash(Shuffling shuffling) {
                if (shuffling.assigneeAccountId == shuffling.issuerId) {
                    try (DbIterator<ShufflingParticipant> participants = shufflingService.getParticipants(shuffling.id)) {
                        return shufflingService.getParticipantsHash(participants);
                    }
                } else {
                    ShufflingParticipant participant = shufflingService.getParticipant(shuffling.getId(), shuffling.assigneeAccountId);
                    return shufflingService.getPreviousParticipant(participant).getDataTransactionFullHash();
                }

            }
        },
        VERIFICATION((byte) 2, new byte[]{3, 4, 5}) {
            @Override
            byte[] getHash(Shuffling shuffling) {
                return shufflingService.getLastParticipant(shuffling.getId()).getDataTransactionFullHash();
            }
        },
        BLAME((byte) 3, new byte[]{4}) {
            @Override
            byte[] getHash(Shuffling shuffling) {
                return shufflingService.getParticipant(shuffling.getId(), shuffling.assigneeAccountId).getDataTransactionFullHash();
            }
        },
        CANCELLED((byte) 4, new byte[]{}) {
            @Override
            byte[] getHash(Shuffling shuffling) {
                byte[] hash = shufflingService.getLastParticipant(shuffling.getId()).getDataTransactionFullHash();
                if (hash != null && hash.length > 0) {
                    return hash;
                }
                try (DbIterator<ShufflingParticipant> participants = shufflingService.getParticipants(shuffling.id)) {
                    return shufflingService.getParticipantsHash(participants);
                }
            }
        },
        DONE((byte) 5, new byte[]{}) {
            @Override
            byte[] getHash(Shuffling shuffling) {
                return shufflingService.getLastParticipant(shuffling.getId()).getDataTransactionFullHash();
            }
        };

        private final byte code;
        private final byte[] allowedNext;

        Stage(byte code, byte[] allowedNext) {
            this.code = code;
            this.allowedNext = allowedNext;
        }

        public static Stage get(byte code) {
            for (Stage stage : Stage.values()) {
                if (stage.code == code) {
                    return stage;
                }
            }
            throw new IllegalArgumentException("No matching stage for " + code);
        }

        public byte getCode() {
            return code;
        }

        public boolean canBecome(Stage nextStage) {
            return Arrays.binarySearch(allowedNext, nextStage.code) >= 0;
        }

        abstract byte[] getHash(Shuffling shuffling);

    }

}
