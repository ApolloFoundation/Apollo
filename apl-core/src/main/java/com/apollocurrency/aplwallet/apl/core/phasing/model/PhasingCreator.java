/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.model;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

public class PhasingCreator {
    private static final long[] EMPTY_WHITE_LIST = Convert.EMPTY_LONG;
    private static final byte[][] EMPTY_LINKED_HASHES = Convert.EMPTY_BYTES;

    public static PhasingPoll createPoll(Transaction transaction, PhasingAppendix appendix) {
        return new PhasingPoll(null, transaction.getId(), transaction.getSenderId(), appendix.getWhitelist(), transaction.getFullHash(), appendix.getFinishHeight(), appendix.getQuorum(), appendix.getVoteWeighting(), appendix.getHashedSecret(), appendix.getAlgorithm(), appendix.getLinkedFullHashes(), transaction.getHeight());
    }

    public static PhasingPoll createPoll(long id, long accountId, byte whiteListSize, int finishHeight, byte votingModel, long quorum,
                                         long minBalance, long holdingId, byte minBalanceModel, byte[] hashedSecret, byte algorithm) {
        return new PhasingPoll(null, id, accountId, whiteListSize == 0 ? EMPTY_WHITE_LIST : null, null, finishHeight, quorum, new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel), hashedSecret, algorithm, null, null);
    }

    public static PhasingPoll createPoll(long dbId, long id, long accountId, long[] whitelist, byte[] fullHash, int finishHeight, byte votingModel, long quorum,
                                         long minBalance, long holdingId, byte minBalanceModel, byte[] hashedSecret, byte algorithm, byte[][] linkedFullhashes, int height) {
        return new PhasingPoll(dbId, id, accountId, whitelist == null ? EMPTY_WHITE_LIST : whitelist, fullHash, finishHeight, quorum, new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel), hashedSecret, algorithm, linkedFullhashes == null ? EMPTY_LINKED_HASHES : linkedFullhashes, height);
    }
}
