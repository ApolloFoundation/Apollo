/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.service;

import com.apollocurrency.aplwallet.apl.core.shuffling.model.ShufflingParticipant;

import java.util.List;

public interface ShufflingParticipantService {

    List<ShufflingParticipant> getParticipants(long shufflingId);

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
