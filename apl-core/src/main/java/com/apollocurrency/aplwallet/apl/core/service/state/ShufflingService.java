/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state;

import com.apollocurrency.aplwallet.apl.core.app.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;

public interface ShufflingService {

    byte[][] getData(long shufflingId, long accountId);

    void restoreData(long shufflingId, long accountId, byte[][] data, int timestamp, int height);

    void setData(ShufflingParticipant participants, byte[][] data, int timestamp);



    DbIterator<ShufflingParticipant> getParticipants(long shufflingId);

    ShufflingParticipant getParticipant(long shufflingId, long accountId);

    ShufflingParticipant getLastParticipant(long shufflingId);

    void addParticipant(long shufflingId, long accountId, int index);

    int getVerifiedCount(long shufflingId);

    void changeStatusToProcessed(ShufflingParticipant participant, byte[] dataTransactionFullHash, byte[] dataHash);

    void changeStatusToVerified(ShufflingParticipant participant);

    void changeStatusToCancel(ShufflingParticipant participant, byte[][] blameData, byte[][] keySeeds);

    ShufflingParticipant getPreviousParticipant(ShufflingParticipant participant);

    boolean delete(ShufflingParticipant participant);

    void setNextAccountId(ShufflingParticipant participant, long nextAccountId);

}
