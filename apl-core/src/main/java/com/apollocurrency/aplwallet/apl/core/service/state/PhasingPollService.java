/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state;

import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPollResult;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingVote;
import com.apollocurrency.aplwallet.apl.core.model.TransactionDbInfo;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.crypto.HashFunction;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PhasingPollService {

    Set<HashFunction> HASH_FUNCTIONS =
        Collections.unmodifiableSet(EnumSet.of(HashFunction.SHA256, HashFunction.RIPEMD160, HashFunction.RIPEMD160_SHA256));

    static HashFunction getHashFunction(byte code) {
        try {
            HashFunction hashFunction = HashFunction.getHashFunction(code);
            if (HASH_FUNCTIONS.contains(hashFunction)) {
                return hashFunction;
            }
        } catch (IllegalArgumentException ignore) {
        }
        return null;
    }

    PhasingPollResult getResult(long id);

    DbIterator<PhasingPollResult> getApproved(int height);

    List<Long> getApprovedTransactionIds(int height);

    PhasingPoll getPoll(long id);

    List<Transaction> getFinishingTransactions(int height);

    List<Transaction> getFinishingTransactionsByTime(int startTime, int finishTime);

    DbIterator<Transaction> getVoterPhasedTransactions(long voterId, int from, int to);

    DbIterator<Transaction> getHoldingPhasedTransactions(long holdingId, VoteWeighting.VotingModel votingModel,
                                                         long accountId, boolean withoutWhitelist, int from, int to);

    DbIterator<Transaction> getAccountPhasedTransactions(long accountId, int from, int to);

    int getAccountPhasedTransactionCount(long accountId);

    List<Transaction> getLinkedPhasedTransactions(byte[] linkedTransactionFullHash);

    long getSenderPhasedTransactionFees(long accountId);

    void addPoll(Transaction transaction, PhasingAppendix appendix);

    List<byte[]> getAndSetLinkedFullHashes(PhasingPoll phasingPoll);

    void reject(Transaction transaction);

    void countVotesAndRelease(Transaction transaction);

    void tryCountVotes(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates);

    long countVotes(PhasingPoll phasingPoll);

    DbIterator<PhasingVote> getVotes(long phasedTransactionId, int from, int to);

    PhasingVote getVote(long phasedTransactionId, long voterId);

    List<TransactionDbInfo> getActivePhasedTransactionDbInfoAtHeight(int height);

    long getVoteCount(long phasedTransactionId);

    List<PhasingVote> getVotes(long phasedTransactionId);

    void addVote(Transaction transaction, Account voter, long phasedTransactionId);

    int getAllPhasedTransactionsCount();

    boolean verifySecret(PhasingPoll poll, byte[] revealedSecret);

    boolean isTransactionPhased(long id);
}
