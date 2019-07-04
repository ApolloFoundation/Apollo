/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.model;

import com.apollocurrency.aplwallet.apl.core.app.ShufflingService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.monetary.HoldingType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingCreation;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.Data;

@Data
public class Shuffling extends VersionedDerivedEntity {
    private final long id;
    private final long holdingId;
    private final HoldingType holdingType;
    private final long issuerId;
    private final long amount;
    private final byte participantCount;
    private short blocksRemaining;
    private byte registrantCount;

    private ShufflingService.Stage stage;
    private long assigneeAccountId;
    private byte[][] recipientPublicKeys;

    public Shuffling(Transaction transaction, ShufflingCreation attachment) {
        super(null, transaction.getHeight());
        this.id = transaction.getId();
        this.holdingId = attachment.getHoldingId();
        this.holdingType = attachment.getHoldingType();
        this.issuerId = transaction.getSenderId();
        this.amount = attachment.getAmount();
        this.participantCount = attachment.getParticipantCount();
        this.blocksRemaining = attachment.getRegistrationPeriod();
        this.stage = ShufflingService.Stage.REGISTRATION;
        this.assigneeAccountId = issuerId;
        this.recipientPublicKeys = Convert.EMPTY_BYTES;
        this.registrantCount = 1;
    }

    public Shuffling(Long dbId, long id, long holdingId, HoldingType holdingType, long issuerId, long amount, byte participantCount, short blocksRemaining, byte registrantCount, ShufflingService.Stage stage, long assigneeAccountId, byte[][] recipientPublicKeys, Integer height) {
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

    public Shuffling deepCopy() {
        byte[][] recipientPublicKeysCopy = new byte[recipientPublicKeys.length][recipientPublicKeys[0].length];
        for (int i = 0; i < recipientPublicKeys.length; i++) {
            System.arraycopy(recipientPublicKeys[i], 0, recipientPublicKeysCopy[i], 0, recipientPublicKeys[i].length);
        }
        Shuffling copy = new Shuffling(null, id, holdingId, holdingType, issuerId, amount, participantCount, blocksRemaining, registrantCount, stage, assigneeAccountId, recipientPublicKeysCopy, getHeight());
        copy.setLatest(isLatest());
        copy.setDbKey(getDbKey());
        return copy;
    }

}
