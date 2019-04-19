/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.model;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import java.util.Objects;

public class PhasingCreator {
    private static final long[] EMPTY_WHITE_LIST = Convert.EMPTY_LONG;
    private static final byte[][] EMPTY_LINKED_HASHES = Convert.EMPTY_BYTES;

    public static PhasingPoll createPoll(Transaction transaction, PhasingAppendix appendix) {
        return new PhasingPoll(null, transaction.getId(), transaction.getSenderId(), appendix.getWhitelist(), transaction.getFullHash(), appendix.getFinishHeight(), appendix.getQuorum(), appendix.getVoteWeighting(), appendix.getHashedSecret(), appendix.getAlgorithm(), appendix.getLinkedFullHashes(), transaction.getHeight());
    }

    public static PhasingPoll createPoll(long id, long accountId, byte whiteListSize, int finishHeight, byte votingModel, long quorum,
                                         long minBalance, long holdingId, byte minBalanceModel, byte[] hashedSecret, byte algorithm) {
        return new PhasingPoll(null, id, accountId, whiteListSize == 0 ? EMPTY_WHITE_LIST : null, null, finishHeight, quorum, new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel), hashedSecret, algorithm, EMPTY_LINKED_HASHES, null);
    }

    public static PhasingPoll createPoll(long dbId, long id, long accountId, long[] whitelist, byte[] fullHash, int finishHeight, byte votingModel, long quorum,
                                         long minBalance, long holdingId, byte minBalanceModel, byte[] hashedSecret, byte algorithm, byte[][] linkedFullhashes, int height) {
        return new PhasingPoll(dbId, id, accountId, whitelist == null ? EMPTY_WHITE_LIST : whitelist, fullHash, finishHeight, quorum, new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel), hashedSecret, algorithm, linkedFullhashes == null ? EMPTY_LINKED_HASHES : linkedFullhashes, height);
    }

    public static PhasingPollLinkedTransaction createLinkedTx(PhasingPoll poll, byte[] fullHash) {
        Objects.requireNonNull(poll, "Poll cannot be null");
        Objects.requireNonNull(fullHash, "Fullhash cannot be null");

        return new PhasingPollLinkedTransaction(null, poll.getHeight(), poll.getId(), Convert.fullHashToId(fullHash), fullHash);
    }

    public static PhasingPollResult createResult(PhasingPoll poll, long result, int height) {
        return new PhasingPollResult(null, height, poll.getId(), result, result >= poll.getQuorum());
    }

    public static PhasingPollVoter createVoter(PhasingPoll poll, long voterId) {
        return new PhasingPollVoter(null, poll.getHeight(), poll.getId(), voterId);
    }

    public static PhasingVote createVote(Transaction transaction, Account voter, long phasedTransactionId) {
        return new PhasingVote(null, transaction.getHeight(), phasedTransactionId, voter.getId(), transaction.getId());
    }


}
