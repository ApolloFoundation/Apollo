/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.service;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.Shuffling;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingCancellationAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingCreation;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingProcessingAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingRecipientsAttachment;

import java.util.List;
import javax.inject.Singleton;

@Singleton
public interface ShufflingService {


    int getCount();

    int getActiveCount();

    List<Shuffling> getAll(int from, int to);

    List<Shuffling> getActiveShufflings(int from, int to);

    List<Shuffling> getFinishedShufflings(int from, int to);

    Shuffling getShuffling(long shufflingId);

    Shuffling getShuffling(byte[] fullHash);

    int getHoldingShufflingCount(long holdingId, boolean includeFinished);

    List<Shuffling> getHoldingShufflings(long holdingId, Stage stage, boolean includeFinished, int from, int to);


    List<Shuffling> getAssignedShufflings(long assigneeAccountId, int from, int to);

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
