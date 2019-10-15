/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TxEventType;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollLinkedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollResultTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollVoterTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingVoteTable;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingCreator;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPollLinkedTransaction;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPollResult;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPollVoter;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingVote;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.HashFunction;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Slf4j
public class PhasingPollServiceImpl implements PhasingPollService {
    private final PhasingPollResultTable resultTable;
    private final PhasingPollTable phasingPollTable;
    private final PhasingPollVoterTable voterTable;
    private final PhasingPollLinkedTransactionTable linkedTransactionTable;
    private final Event<Transaction> event;
    private final PhasingVoteTable phasingVoteTable;
    private final Blockchain blockchain;

    @Inject
    public PhasingPollServiceImpl(PhasingPollResultTable resultTable, PhasingPollTable phasingPollTable,
                                  PhasingPollVoterTable voterTable, PhasingPollLinkedTransactionTable linkedTransactionTable,
                                  PhasingVoteTable phasingVoteTable, Blockchain blockchain, Event<Transaction> event) {
        this.resultTable = resultTable;
        this.phasingPollTable = phasingPollTable;
        this.voterTable = voterTable;
        this.linkedTransactionTable = linkedTransactionTable;
        this.phasingVoteTable = phasingVoteTable;
        this.blockchain = blockchain;
        this.event = Objects.requireNonNull(event);
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
            long phasingPollId = phasingPoll.getId();
            byte[] fullHash = blockchain.getFullHash(phasingPollId);
            phasingPoll.setFullHash(fullHash);
            if (phasingPoll.getWhitelist() == null) {
                List<Long> voteIds = voterTable.get(phasingPollId)
                        .stream()
                        .map(PhasingPollVoter::getVoterId)
                        .collect(Collectors.toList());
                phasingPoll.setWhitelist(Convert.toArray(voteIds));
            }
        }
        return phasingPoll;
    }

    @Override
    public List<Transaction> getFinishingTransactions(int height) {
        return phasingPollTable.getFinishingTransactions(height);
    }

    @Override
    public List<Transaction> getFinishingTransactionsByTime(int time) {
        return phasingPollTable.getFinishingTransactionsByTime(time);
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
            return phasingPollTable.getHoldingPhasedTransactions(holdingId, votingModel, accountId, withoutWhitelist, from, to, blockchain.getHeight());
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<Transaction> getAccountPhasedTransactions(long accountId, int from, int to) {
        try {
            return phasingPollTable.getAccountPhasedTransactions(accountId, from, to, blockchain.getHeight());
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public int getAccountPhasedTransactionCount(long accountId) {
        try {
            return phasingPollTable.getAccountPhasedTransactionCount(accountId, blockchain.getHeight());
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
            return phasingPollTable.getSenderPhasedTransactionFees(accountId, blockchain.getHeight());
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public void addPoll(Transaction transaction, PhasingAppendix appendix) {
        PhasingPoll poll = PhasingCreator.createPoll(transaction, appendix);
        phasingPollTable.insert(poll);
        long[] voters = poll.getWhitelist();
        if (voters.length > 0) {
            List<PhasingPollVoter> voterList = Convert.toList(voters)
                    .stream()
                    .map(v -> new PhasingPollVoter(null, poll.getHeight(), poll.getId(),  v))
                    .collect(Collectors.toList());
            voterTable.insert(voterList);
        }
        if (appendix.getLinkedFullHashes().length > 0) {
            List<byte[]> linkedFullHashes = new ArrayList<>();
            Collections.addAll(linkedFullHashes, appendix.getLinkedFullHashes());
            List<PhasingPollLinkedTransaction> phasingPollLinkedTransactions = linkedFullHashes
                    .stream()
                    .map(fullHash -> new PhasingPollLinkedTransaction(null, poll.getHeight(), poll.getId(), Convert.fullHashToId(fullHash), fullHash))
                    .collect(Collectors.toList());
            linkedTransactionTable.insert(phasingPollLinkedTransactions);
        }
    }

    void finish(PhasingPoll phasingPoll, long result) {
        int height = blockchain.getHeight();
        PhasingPollResult phasingPollResult = new PhasingPollResult(null, height, phasingPoll.getId(), result, result >= phasingPoll.getQuorum());
        resultTable.insert(phasingPollResult);
    }

    public List<byte[]> getAndSetLinkedFullHashes(PhasingPoll phasingPoll) {
        if (phasingPoll.getLinkedFullHashes() == null) {
            List<PhasingPollLinkedTransaction> phasingPollLinkedTransactions = linkedTransactionTable.get(phasingPoll.getId());
            List<byte[]> linkedFullHashes = phasingPollLinkedTransactions.stream().map(PhasingPollLinkedTransaction::getFullHash).collect(Collectors.toList());
            phasingPoll.setLinkedFullHashes(linkedFullHashes);
            return linkedFullHashes;
        } else {
            return phasingPoll.getLinkedFullHashes();
        }
    }

    private void release(Transaction transaction) {

        Account senderAccount = Account.getAccount(transaction.getSenderId());
        Account recipientAccount = transaction.getRecipientId() == 0 ? null : Account.getAccount(transaction.getRecipientId());
        transaction.getAppendages().forEach(appendage -> {
            if (appendage.isPhasable()) {
                appendage.apply(transaction, senderAccount, recipientAccount);
            }
        });
        event.select(TxEventType.literal(TxEventType.RELEASE_PHASED_TRANSACTION)).fire(transaction);
        log.trace("Phased transaction " + transaction.getStringId() + " has been released");
    }

    @Override
    public void reject(Transaction transaction) {
        Account senderAccount = Account.getAccount(transaction.getSenderId());
        transaction.getType().undoAttachmentUnconfirmed(transaction, senderAccount);
        senderAccount.addToUnconfirmedBalanceATM(LedgerEvent.REJECT_PHASED_TRANSACTION, transaction.getId(),
                transaction.getAmountATM());
        event.select(TxEventType.literal(TxEventType.REJECT_PHASED_TRANSACTION)).fire(transaction);
        log.trace("Phased transaction " + transaction.getStringId() + " has been rejected");
    }

    @Override
    public void countVotesAndRelease(Transaction transaction) {
        if (getResult(transaction.getId()) != null) {
            return;
        }
        PhasingPoll poll = getPoll(transaction.getId());
        long result = countVotes(poll);
        finish(poll, result);
        if (result >= poll.getQuorum()) {
            try {
                release(transaction);
            } catch (RuntimeException e) {
                log.error("Failed to release phased transaction " + transaction.getJSONObject().toJSONString(), e);
                reject(transaction);
            }
        } else {
            reject(transaction);
        }
    }

    @Override
    public void tryCountVotes(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        PhasingPoll poll = getPoll(transaction.getId());
        long result = countVotes(poll);
        if (result >= poll.getQuorum()) {
            if (!transaction.attachmentIsDuplicate(duplicates, false)) {
                try {
                    release(transaction);
                    finish(poll, result);
                    log.debug("Early finish of transaction " + transaction.getStringId() + " at height " + blockchain.getHeight());
                } catch (RuntimeException e) {
                    log.error("Failed to release phased transaction " + transaction.getJSONObject().toJSONString(), e);
                }
            } else {
                log.debug("At height " + blockchain.getHeight() + " phased transaction " + transaction.getStringId()
                        + " is duplicate, cannot finish early");
            }
        } else {
            log.debug("At height " + blockchain.getHeight() + " phased transaction " + transaction.getStringId()
                    + " does not yet meet quorum, cannot finish early");
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

    public boolean verifySecret(PhasingPoll poll, byte[] revealedSecret) {
        HashFunction hashFunction = PhasingPollService.getHashFunction(poll.getAlgorithm());
        return hashFunction != null && Arrays.equals(poll.getHashedSecret(), hashFunction.hash(revealedSecret));
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
    public List<TransactionDbInfo> getActivePhasedTransactionDbInfoAtHeight(int height) {
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
    public List<PhasingVote> getVotes(long phasedTransactionId) {
        return phasingVoteTable.get(phasedTransactionId);
    }

    @Override
    public void addVote(Transaction transaction, Account voter, long phasedTransactionId) {
        PhasingVote phasingVote = phasingVoteTable.get(phasedTransactionId, voter.getId());
        if (phasingVote == null) {
            phasingVote =  new PhasingVote(null, transaction.getHeight(),phasedTransactionId, voter.getId(), transaction.getId());
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
