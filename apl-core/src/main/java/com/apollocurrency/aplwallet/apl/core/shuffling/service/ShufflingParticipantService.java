/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.service;

import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.ShufflingParticipant;

import java.util.Arrays;

public interface ShufflingParticipantService {

    enum State {
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

        public static State get(byte code) {
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

    enum Event {
        PARTICIPANT_REGISTERED, PARTICIPANT_PROCESSED, PARTICIPANT_VERIFIED, PARTICIPANT_CANCELLED
    }

    DbIterator<ShufflingParticipant> getParticipants(long shufflingId);

    ShufflingParticipant getParticipant(long shufflingId, long accountId);

    ShufflingParticipant getLastParticipant(long shufflingId);

    void addParticipant(long shufflingId, long accountId, int index);

    int getVerifiedCount(long shufflingId);

    void setNextAccountId(ShufflingParticipant participant, long nextAccountId);

    byte[][] getData(ShufflingParticipant participant);

    byte[][] getData(long shufflingId, long accountId);

    void setData(ShufflingParticipant participant, byte[][] data, int timestamp);

    void restoreData(long shufflingId, long accountId, byte[][] data, int timestamp, int height);

    void cancel(ShufflingParticipant participant, byte[][] blameData, byte[][] keySeeds);


    void setProcessed(ShufflingParticipant participant, byte[] dataTransactionFullHash, byte[] dataHash);

    ShufflingParticipant getPreviousParticipant(ShufflingParticipant participant);

    void verify(ShufflingParticipant participant);

    void delete(ShufflingParticipant participant);

}
