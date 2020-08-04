/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.impl;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TxEventType;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollLinkedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollResultTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollVoterTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingVoteTable;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlPhasing;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlType;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPollLinkedTransaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPollResult;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPollVoter;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingVote;
import com.apollocurrency.aplwallet.apl.core.model.PhasingCreator;
import com.apollocurrency.aplwallet.apl.core.model.PhasingParams;
import com.apollocurrency.aplwallet.apl.core.model.TransactionDbInfo;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountControlPhasingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.HashFunction;
import com.apollocurrency.aplwallet.apl.util.Constants;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Event;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final AccountService accountService;
    private AccountControlPhasingService accountControlPhasingService; // lazy initalization only!
    private final CurrencyService currencyService;

    @Inject
    public PhasingPollServiceImpl(PhasingPollResultTable resultTable, PhasingPollTable phasingPollTable,
                                  PhasingPollVoterTable voterTable, PhasingPollLinkedTransactionTable linkedTransactionTable,
                                  PhasingVoteTable phasingVoteTable, Blockchain blockchain, Event<Transaction> event,
                                  AccountService accountService, CurrencyService currencyService) {
        this.resultTable = resultTable;
        this.phasingPollTable = phasingPollTable;
        this.voterTable = voterTable;
        this.linkedTransactionTable = linkedTransactionTable;
        this.phasingVoteTable = phasingVoteTable;
        this.blockchain = blockchain;
        this.event = Objects.requireNonNull(event);
        this.accountService = Objects.requireNonNull(accountService, "accountService is null");
        this.currencyService = currencyService;
    }

    private AccountControlPhasingService lookupAccountControlPhasingService() {
        if (accountControlPhasingService == null) {
            accountControlPhasingService = CDI.current().select(AccountControlPhasingService.class).get();
        }
        return accountControlPhasingService;
    }

    @Override
    public PhasingPollResult getResult(long id) {
        return resultTable.get(id);
    }

    @Override
    public List<PhasingPollResult> getApproved(int height) {
        return CollectionUtil.toList(resultTable.getManyBy(new DbClause.IntClause("height", height).and(new DbClause.BooleanClause("approved", true)),
            0, -1, " ORDER BY db_id ASC "));
    }

    @Override
    public List<Long> getApprovedTransactionIds(int height) {
        List<Long> transactionIdList = new ArrayList<>();
        try (DbIterator<PhasingPollResult> result = resultTable.getManyBy(new DbClause.IntClause("height", height).and(new DbClause.BooleanClause("approved", true)),
            0, -1, " ORDER BY db_id ASC ")) {
            result.forEach(phasingPollResult -> transactionIdList.add(phasingPollResult.getId()));
        }
        return transactionIdList;
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
    public List<Transaction> getFinishingTransactionsByTime(int startTime, int finishTime) {
        return phasingPollTable.getFinishingTransactionsByTime(startTime, finishTime);
    }

    @Override
    public List<Transaction> getVoterPhasedTransactions(long voterId, int from, int to) {
        try {
            return voterTable.getVoterPhasedTransactions(voterId, from, to);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public List<Transaction> getHoldingPhasedTransactions(long holdingId, VoteWeighting.VotingModel votingModel,
                                                                long accountId, boolean withoutWhitelist, int from, int to) {
        try {
            return phasingPollTable.getHoldingPhasedTransactions(holdingId, votingModel, accountId, withoutWhitelist, from, to, blockchain.getHeight());
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public List<Transaction> getAccountPhasedTransactions(long accountId, int from, int to) {
        try {
            return phasingPollTable.getAccountPhasedTransactions(accountId, from, to, blockchain.getHeight());
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public int getAccountPhasedTransactionCount(long accountId) {
        try {
            return phasingPollTable.getAccountPhasedTransactionCount(accountId, blockchain.getHeight());
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public List<Transaction> getLinkedPhasedTransactions(byte[] linkedTransactionFullHash) {
        try {
            return linkedTransactionTable.getLinkedPhasedTransactions(linkedTransactionFullHash);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }


    @Override
    public long getSenderPhasedTransactionFees(long accountId) {
        try {
            return phasingPollTable.getSenderPhasedTransactionFees(accountId, blockchain.getHeight());
        } catch (SQLException e) {
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
                .map(v -> new PhasingPollVoter(null, poll.getHeight(), poll.getId(), v))
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

        Account senderAccount = accountService.getAccount(transaction.getSenderId());
        Account recipientAccount = transaction.getRecipientId() == 0 ? null : accountService.getAccount(transaction.getRecipientId());
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
        Account senderAccount = accountService.getAccount(transaction.getSenderId());
        transaction.getType().undoAttachmentUnconfirmed(transaction, senderAccount);
        accountService.addToUnconfirmedBalanceATM(senderAccount, LedgerEvent.REJECT_PHASED_TRANSACTION, transaction.getId(),
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
                log.error("Failed to release phased transaction " + transaction.getId(), e);
                reject(transaction);
            }
        } else {
            reject(transaction);
        }
    }

    @Override
    public void tryCountVotes(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        PhasingPoll poll = getPoll(transaction.getId());
        long result = countVotes(poll);
        if (result >= poll.getQuorum()) {
            // prefetch data for duplicate validation
            Account senderAccount = accountService.getAccount(transaction.getSenderId());
            Set<AccountControlType> senderAccountControls = senderAccount.getControls();
            AccountControlPhasing accountControlPhasing = lookupAccountControlPhasingService().get(transaction.getSenderId());
            if (!transaction.attachmentIsDuplicate(duplicates, false, senderAccountControls, accountControlPhasing)) {
                try {
                    release(transaction);
                    finish(poll, result);
                    log.debug("Early finish of transaction " + transaction.getStringId() + " at height " + blockchain.getHeight());
                } catch (RuntimeException e) {
                    log.error("Failed to release phased transaction " + transaction.getId(), e);
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
        } catch (SQLException e) {
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
            phasingVote = new PhasingVote(null, transaction.getHeight(), phasedTransactionId, voter.getId(), transaction.getId());
            phasingVoteTable.insert(phasingVote);
        }
    }


    @Override
    public int getAllPhasedTransactionsCount() {
        try {
            return phasingPollTable.getAllPhasedTransactionsCount();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public boolean isTransactionPhased(long id) {
        try {
            return phasingPollTable.isTransactionPhased(id);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public void validate(PhasingParams phasingParams) throws AplException.ValidationException {
        long[] whitelist = phasingParams.getWhitelist();
        if (whitelist.length > Constants.MAX_PHASING_WHITELIST_SIZE) {
            throw new AplException.NotValidException("Whitelist is too big");
        }
        VoteWeighting voteWeighting = phasingParams.getVoteWeighting();

        long previousAccountId = 0;
        for (long accountId : whitelist) {
            if (accountId == 0) {
                throw new AplException.NotValidException("Invalid accountId 0 in whitelist");
            }
            if (previousAccountId != 0 && accountId < previousAccountId) {
                throw new AplException.NotValidException("Whitelist not sorted " + Arrays.toString(whitelist));
            }
            if (accountId == previousAccountId) {
                throw new AplException.NotValidException("Duplicate accountId " + Long.toUnsignedString(accountId) + " in whitelist");
            }
            previousAccountId = accountId;
        }
        long quorum = phasingParams.getQuorum();
        if (quorum <= 0 && voteWeighting.getVotingModel() != VoteWeighting.VotingModel.NONE) {
            throw new AplException.NotValidException("quorum <= 0");
        }

        if (voteWeighting.getVotingModel() == VoteWeighting.VotingModel.NONE) {
            if (quorum != 0) {
                throw new AplException.NotValidException("Quorum must be 0 for no-voting phased transaction");
            }
            if (whitelist.length != 0) {
                throw new AplException.NotValidException("No whitelist needed for no-voting phased transaction");
            }
        }

        if (voteWeighting.getVotingModel() == VoteWeighting.VotingModel.ACCOUNT && whitelist.length > 0 && quorum > whitelist.length) {
            throw new AplException.NotValidException("Quorum of " + quorum + " cannot be achieved in by-account voting with whitelist of length "
                + whitelist.length);
        }

        voteWeighting.validate();
        AssetService assetService = CDI.current().select(AssetService.class).get();

        if (voteWeighting.getVotingModel() == VoteWeighting.VotingModel.CURRENCY) {
            Currency currency = currencyService.getCurrency(voteWeighting.getHoldingId());
            if (currency == null) {
                throw new AplException.NotCurrentlyValidException("Currency " + Long.toUnsignedString(voteWeighting.getHoldingId()) + " not found");
            }
            if (quorum > currency.getMaxSupply()) {
                throw new AplException.NotCurrentlyValidException("Quorum of " + quorum
                    + " exceeds max currency supply " + currency.getMaxSupply());
            }
            if (voteWeighting.getMinBalance() > currency.getMaxSupply()) {
                throw new AplException.NotCurrentlyValidException("MinBalance of " + voteWeighting.getMinBalance()
                    + " exceeds max currency supply " + currency.getMaxSupply());
            }
        } else if (voteWeighting.getVotingModel() == VoteWeighting.VotingModel.ASSET) {
            Asset asset = assetService.getAsset(voteWeighting.getHoldingId());
            if (quorum > asset.getInitialQuantityATU()) {
                throw new AplException.NotCurrentlyValidException("Quorum of " + quorum
                    + " exceeds total initial asset quantity " + asset.getInitialQuantityATU());
            }
            if (voteWeighting.getMinBalance() > asset.getInitialQuantityATU()) {
                throw new AplException.NotCurrentlyValidException("MinBalance of " + voteWeighting.getMinBalance()
                    + " exceeds total initial asset quantity " + asset.getInitialQuantityATU());
            }
        } else if (voteWeighting.getMinBalance() > 0) {
            if (voteWeighting.getMinBalanceModel() == VoteWeighting.MinBalanceModel.ASSET) {
                Asset asset = assetService.getAsset(voteWeighting.getHoldingId());
                if (voteWeighting.getMinBalance() > asset.getInitialQuantityATU()) {
                    throw new AplException.NotCurrentlyValidException("MinBalance of " + voteWeighting.getMinBalance()
                        + " exceeds total initial asset quantity " + asset.getInitialQuantityATU());
                }
            } else if (voteWeighting.getMinBalanceModel() == VoteWeighting.MinBalanceModel.CURRENCY) {
                Currency currency = currencyService.getCurrency(voteWeighting.getHoldingId());
                if (currency == null) {
                    throw new AplException.NotCurrentlyValidException("Currency " + Long.toUnsignedString(voteWeighting.getHoldingId()) + " not found");
                }
                if (voteWeighting.getMinBalance() > currency.getMaxSupply()) {
                    throw new AplException.NotCurrentlyValidException("MinBalance of " + voteWeighting.getMinBalance()
                        + " exceeds max currency supply " + currency.getMaxSupply());
                }
            }
        }

    }

    @Override
    public void checkApprovable(PhasingParams phasingParams) throws AplException.NotCurrentlyValidException {

        VoteWeighting voteWeighting = phasingParams.getVoteWeighting();
        if (voteWeighting.getVotingModel() == VoteWeighting.VotingModel.CURRENCY
            && currencyService.getCurrency(voteWeighting.getHoldingId()) == null) {
            throw new AplException.NotCurrentlyValidException("Currency " + Long.toUnsignedString(voteWeighting.getHoldingId()) + " not found");
        }
        if (voteWeighting.getMinBalance() > 0 && voteWeighting.getMinBalanceModel() == VoteWeighting.MinBalanceModel.CURRENCY
            && currencyService.getCurrency(voteWeighting.getHoldingId()) == null) {
            throw new AplException.NotCurrentlyValidException("Currency " + Long.toUnsignedString(voteWeighting.getHoldingId()) + " not found");
        }
    }
}
