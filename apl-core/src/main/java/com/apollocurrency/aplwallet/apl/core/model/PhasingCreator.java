/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.model;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendixV2;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

public class PhasingCreator {
    private static final long[] EMPTY_WHITE_LIST = Convert.EMPTY_LONG;
    private static final byte[][] EMPTY_LINKED_HASHES = Convert.EMPTY_BYTES;

    public static PhasingPoll createPoll(Transaction transaction, PhasingAppendix appendix) {
        int finishTime = -1;

        if (appendix instanceof PhasingAppendixV2) {
            finishTime = ((PhasingAppendixV2) appendix).getFinishTime();
        }

        return new PhasingPoll(
            null, transaction.getId(), transaction.getSenderId(), appendix.getWhitelist(),
            transaction.getFullHash(), appendix.getFinishHeight(), finishTime, appendix.getQuorum(),
            appendix.getVoteWeighting(), appendix.getHashedSecret(), appendix.getAlgorithm(),
            appendix.getLinkedFullHashes(), transaction.getHeight());
    }

    public static PhasingPoll createPoll(long id, long accountId, byte whiteListSize, int finishHeight, int finishTime, byte votingModel, long quorum,
                                         long minBalance, long holdingId, byte minBalanceModel, byte[] hashedSecret, byte algorithm) {
        return new PhasingPoll(
            null, id, accountId, whiteListSize == 0 ? EMPTY_WHITE_LIST : null, null, finishHeight, finishTime, quorum,
            new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel), hashedSecret, algorithm, null, null);
    }

    public static PhasingPoll createPoll(long dbId, long id, long accountId, long[] whitelist, byte[] fullHash, int finishHeight, int finishTime, byte votingModel, long quorum,
                                         long minBalance, long holdingId, byte minBalanceModel, byte[] hashedSecret, byte algorithm, byte[][] linkedFullhashes, int height) {
        return new PhasingPoll(
            dbId, id, accountId, whitelist == null ? EMPTY_WHITE_LIST : whitelist, fullHash, finishHeight, finishTime, quorum,
            new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel), hashedSecret, algorithm, linkedFullhashes == null ? EMPTY_LINKED_HASHES : linkedFullhashes, height);
    }
}
