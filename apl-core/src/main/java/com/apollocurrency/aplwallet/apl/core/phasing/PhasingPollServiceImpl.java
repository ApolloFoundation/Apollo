/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollLinkedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollResultTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollVoterTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingVoteTable;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Filter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PhasingPollServiceImpl implements PhasingPollService {
    private final PhasingPollResultTable resultTable;
    private final PhasingPollTable phasingPollTable;
    private final PhasingPollVoterTable voterTable;
    private final PhasingPollLinkedTransactionTable linkedTransactionTable;
    private final PhasingVoteTable phasingVoteTable;
    private Blockchain blockchain; //TODO init in constructor
    private GlobalSync globalSync;
    private TransactionValidator transactionValidator;

    @Inject
    public PhasingPollServiceImpl(PhasingPollResultTable resultTable, PhasingPollTable phasingPollTable, PhasingPollVoterTable voterTable, PhasingPollLinkedTransactionTable linkedTransactionTable, PhasingVoteTable phasingVoteTable) {
        this.resultTable = resultTable;
        this.phasingPollTable = phasingPollTable;
        this.voterTable = voterTable;
        this.linkedTransactionTable = linkedTransactionTable;
        this.phasingVoteTable = phasingVoteTable;
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
        getAndSetLinkedFullHashes(phasingPoll);
        byte[] fullHash = blockchain.getFullHash(phasingPoll.getId());
        phasingPoll.setFullHash(fullHash);
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

    public void finish(PhasingPoll phasingPoll, long result) {
        PhasingPollResult phasingPollResult = new PhasingPollResult(phasingPoll, result, blockchain.getHeight() + 1);
        resultTable.insert(phasingPollResult);
    }

    public List<byte[]> getAndSetLinkedFullHashes(PhasingPoll phasingPoll) {
        List<byte[]> linkedFullHashes = linkedTransactionTable.get(phasingPoll.getId());
        phasingPoll.setLinkedFullHashes(linkedFullHashes);
        return linkedFullHashes;
    }

    public long countVotes(PhasingPoll phasingPoll) {
        VoteWeighting voteWeighting = phasingPoll.getVoteWeighting();
        if (voteWeighting.getVotingModel() == VoteWeighting.VotingModel.NONE) {
            return 0;
        }
        int height = Math.min(phasingPoll.getFinishHeight(), blockchain.getHeight());
        if (voteWeighting.getVotingModel() == VoteWeighting.VotingModel.TRANSACTION) {
            int count = 0;
            for (byte[] hash : getAndSetLinkedFullHashes(phasingPoll)) {
                if (blockchain.hasTransaction(Convert.fullHashToId(hash), height)) {
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

    public DbIterator<PhasingVote> getVotes(long phasedTransactionId, int from, int to) {
        return phasingVoteTable.getManyBy(new DbClause.LongClause("transaction_id", phasedTransactionId), from, to);
    }

    public PhasingVote getVote(long phasedTransactionId, long voterId) {
        return phasingVoteTable.get(phasedTransactionId, voterId);
    }

    public long getVoteCount(long phasedTransactionId) {
        return phasingVoteTable.getCount(new DbClause.LongClause("transaction_id", phasedTransactionId));
    }

    public void addVote(Transaction transaction, Account voter, long phasedTransactionId) {
        PhasingVote phasingVote = phasingVoteTable.get(phasedTransactionId, voter.getId());
        if (phasingVote == null) {
            phasingVote = new PhasingVote(transaction, voter, phasedTransactionId);
            phasingVoteTable.insert(phasingVote);
        }
    }
    @Override
    public List<Transaction> getExpectedTransactions(Filter<Transaction> filter) {
        Map<TransactionType, Map<String, Integer>> duplicates = new HashMap<>();
        BlockchainProcessor blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
        List<Transaction> result = new ArrayList<>();
        globalSync.readLock();
        try {
            try (DbIterator<Transaction> phasedTransactions = getFinishingTransactions(blockchain.getHeight() + 1)) {
                for (Transaction phasedTransaction : phasedTransactions) {
                    try {
                        transactionValidator.validate(phasedTransaction);
                        if (!phasedTransaction.attachmentIsDuplicate(duplicates, false) && filter.test(phasedTransaction)) {
                            result.add(phasedTransaction);
                        }
                    } catch (AplException.ValidationException ignore) {
                    }
                }
            }

            blockchainProcessor.selectUnconfirmedTransactions(duplicates, blockchain.getLastBlock(), -1).forEach(
                    unconfirmedTransaction -> {
                        Transaction transaction = unconfirmedTransaction.getTransaction();
                        if (transaction.getPhasing() == null && filter.test(transaction)) {
                            result.add(transaction);
                        }
                    }
            );
        } finally {
            globalSync.readUnlock();
        }
        return result;
    }

}
