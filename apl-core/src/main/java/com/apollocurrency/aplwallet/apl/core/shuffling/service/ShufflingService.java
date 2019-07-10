/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.service;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.Shuffling;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingCancellationAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingCreation;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingProcessingAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingRecipientsAttachment;

import java.util.Arrays;
import java.util.List;
import javax.inject.Singleton;

@Singleton
public interface ShufflingService {


    enum Stage {
        REGISTRATION((byte)0, new byte[]{1,4}),
        PROCESSING((byte)1, new byte[]{2,3,4}),
        VERIFICATION((byte)2, new byte[]{3,4,5}),
        BLAME((byte)3, new byte[]{4}),
        CANCELLED((byte)4, new byte[]{}),
        DONE((byte) 5, new byte[] {});

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

    }

    int getCount();

    int getActiveCount();

    DbIterator<Shuffling> getAll(int from, int to);

    DbIterator<Shuffling> getActiveShufflings(int from, int to);

    DbIterator<Shuffling> getFinishedShufflings(int from, int to);

    Shuffling getShuffling(long shufflingId);

    Shuffling getShuffling(byte[] fullHash);

    int getHoldingShufflingCount(long holdingId, boolean includeFinished);

    DbIterator<Shuffling> getHoldingShufflings(long holdingId, Stage stage, boolean includeFinished, int from, int to);


    DbIterator<Shuffling> getAssignedShufflings(long assigneeAccountId, int from, int to);

    void addShuffling(Transaction transaction, ShufflingCreation attachment);

    byte[] getStateHash(Shuffling shuffling);

    byte[] getFullHash(Shuffling shuffling);

    ShufflingAttachment process(Shuffling shuffling, final long accountId, final byte[] secretBytes, final byte[] recipientPublicKey);

    ShufflingCancellationAttachment revealKeySeeds(Shuffling shuffling, final byte[] secretBytes, long cancellingAccountId, byte[] shufflingStateHash);

    void updateParticipantData(Shuffling shuffling, Transaction transaction, ShufflingProcessingAttachment attachment);

    void updateRecipients(Shuffling shuffling, Transaction transaction, ShufflingRecipientsAttachment attachment);

    void verify(Shuffling shuffling, long accountId);

    void cancelBy(Shuffling shuffling, ShufflingParticipant participant, byte[][] blameData, byte[][] keySeeds);

    void addParticipant(Shuffling shuffling, long participantId);

    List<Shuffling> getAccountShufflings(long accountId, boolean includeFinished, int from, int to);
}
