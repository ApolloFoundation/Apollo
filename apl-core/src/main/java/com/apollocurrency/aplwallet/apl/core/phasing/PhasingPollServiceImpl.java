/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollLinkedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollResultTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollVoterTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingVoteTable;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPollResult;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingVote;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PhasingPollServiceImpl implements PhasingPollService {
    private final PhasingPollResultTable resultTable;
    private final PhasingPollTable phasingPollTable;
    private final PhasingPollVoterTable voterTable;
    private final PhasingPollLinkedTransactionTable linkedTransactionTable;
    private final PhasingVoteTable phasingVoteTable;
    private final Blockchain blockchain;

    @Inject
    public PhasingPollServiceImpl(PhasingPollResultTable resultTable, PhasingPollTable phasingPollTable, PhasingPollVoterTable voterTable, PhasingPollLinkedTransactionTable linkedTransactionTable, PhasingVoteTable phasingVoteTable, Blockchain blockchain) {
        this.resultTable = resultTable;
        this.phasingPollTable = phasingPollTable;
        this.voterTable = voterTable;
        this.linkedTransactionTable = linkedTransactionTable;
        this.phasingVoteTable = phasingVoteTable;
        this.blockchain = blockchain;
    }

    @Override
    public PhasingPollResult getResult(long id) {
        return resultTable.get(id);
    }

    @Override
    public DbIterator<PhasingPollResult> getApproved(int height) {
        return resultTable.getManyBy(new DbClause.IntClause("height", height).and(new DbClause.BooleanClause("approved", true)),
                0, -1, " ORDER BY db_id ASC ");
    }

    @Override
    public PhasingPoll getPoll(long id) {
        PhasingPoll phasingPoll = phasingPollTable.get(id);
        if (phasingPoll != null) {
            getAndSetLinkedFullHashes(phasingPoll);
            byte[] fullHash = blockchain.getFullHash(phasingPoll.getId());
            phasingPoll.setFullHash(fullHash);
            if (phasingPoll.getWhitelist() == null) {
                phasingPoll.setWhitelist(Convert.toArray(voterTable.get(phasingPoll.getId())));
            }
        }
        return phasingPoll;
    }

    @Override
    public DbIterator<Transaction> getFinishingTransactions(int height) {
        try {
            return phasingPollTable.getFinishingTransactions(height);
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<Transaction> getVoterPhasedTransactions(long voterId, int from, int to) {
        try {
            return voterTable.getVoterPhasedTransactions(voterId, from, to);
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<Transaction> getHoldingPhasedTransactions(long holdingId, VoteWeighting.VotingModel votingModel,
                                                                long accountId, boolean withoutWhitelist, int from, int to) {
        try {
            return phasingPollTable.getHoldingPhasedTransactions(holdingId, votingModel, accountId, withoutWhitelist, from, to);
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<Transaction> getAccountPhasedTransactions(long accountId, int from, int to) {
        try {
            return phasingPollTable.getAccountPhasedTransactions(accountId, from, to);
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public int getAccountPhasedTransactionCount(long accountId) {
        try {
            return phasingPollTable.getAccountPhasedTransactionCount(accountId);
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public List<Transaction> getLinkedPhasedTransactions(byte[] linkedTransactionFullHash) {
        try {
            return linkedTransactionTable.getLinkedPhasedTransactions(linkedTransactionFullHash);
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }


    @Override
    public long getSenderPhasedTransactionFees(long accountId) {
        try {
            return phasingPollTable.getSenderPhasedTransactionFees(accountId);
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public void addPoll(Transaction transaction, PhasingAppendix appendix) {
        PhasingPoll poll = new PhasingPoll(transaction, appendix);
        phasingPollTable.insert(poll);
        long[] voters = poll.getWhitelist();
        if (voters.length > 0) {
            voterTable.insert(poll, Convert.toList(voters));
        }
        if (appendix.getLinkedFullHashes().length > 0) {
            List<byte[]> linkedFullHashes = new ArrayList<>(appendix.getLinkedFullHashes().length);
            Collections.addAll(linkedFullHashes, appendix.getLinkedFullHashes());
            linkedTransactionTable.insert(poll, linkedFullHashes);
        }
    }

    @Override
    public void finish(PhasingPoll phasingPoll, long result) {
        PhasingPollResult phasingPollResult = new PhasingPollResult(phasingPoll, result, blockchain.getHeight() + 1);
        resultTable.insert(phasingPollResult);
    }

    public List<byte[]> getAndSetLinkedFullHashes(PhasingPoll phasingPoll) {
        if (phasingPoll.getLinkedFullHashes() == null) {
            List<byte[]> linkedFullHashes = linkedTransactionTable.get(phasingPoll.getId());
            phasingPoll.setLinkedFullHashes(linkedFullHashes);
            return linkedFullHashes;
        } else {
            return phasingPoll.getLinkedFullHashes();
        }
    }

    @Override
    public long countVotes(PhasingPoll phasingPoll) {
        VoteWeighting voteWeighting = phasingPoll.getVoteWeighting();
        if (voteWeighting.getVotingModel() == VoteWeighting.VotingModel.NONE) {
            return 0;
        }
        int height = Math.min(phasingPoll.getFinishHeight(), blockchain.getHeight());
        if (voteWeighting.getVotingModel() == VoteWeighting.VotingModel.TRANSACTION) {
            int count = 0;
            for (byte[] hash : getAndSetLinkedFullHashes(phasingPoll)) {
                if (blockchain.hasTransactionByFullHash(hash, height)) {
                    count += 1;
                }
            }
            return count;
        }
        if (voteWeighting.isBalanceIndependent()) {
            return getVoteCount(phasingPoll.getId());
        }
        VoteWeighting.VotingModel votingModel = voteWeighting.getVotingModel();
        long cumulativeWeight = 0;
        try (DbIterator<PhasingVote> votes = getVotes(phasingPoll.getId(), 0, Integer.MAX_VALUE)) {
            for (PhasingVote vote : votes) {
                cumulativeWeight += votingModel.calcWeight(voteWeighting, vote.getVoterId(), height);
            }
        }
        return cumulativeWeight;
    }

    @Override
    public DbIterator<PhasingVote> getVotes(long phasedTransactionId, int from, int to) {
        return phasingVoteTable.getManyBy(new DbClause.LongClause("transaction_id", phasedTransactionId), from, to);
    }

    @Override
    public PhasingVote getVote(long phasedTransactionId, long voterId) {
        return phasingVoteTable.get(phasedTransactionId, voterId);
    }

    @Override
    public List<Long> getActivePhasedTransactionDbIdsAtHeight(int height) {
        try {
            return phasingPollTable.getActivePhasedTransactionDbIds(height);
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public long getVoteCount(long phasedTransactionId) {
        return phasingVoteTable.getCount(new DbClause.LongClause("transaction_id", phasedTransactionId));
    }

    @Override
    public void addVote(Transaction transaction, Account voter, long phasedTransactionId) {
        PhasingVote phasingVote = phasingVoteTable.get(phasedTransactionId, voter.getId());
        if (phasingVote == null) {
            phasingVote = new PhasingVote(transaction, voter, phasedTransactionId);
            phasingVoteTable.insert(phasingVote);
        }
    }


    @Override
    public int getAllPhasedTransactionsCount() {
        try {
            return phasingPollTable.getAllPhasedTransactionsCount();
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public boolean isTransactionPhased(long id){
        try {
            return phasingPollTable.isTransactionPhased(id);
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
