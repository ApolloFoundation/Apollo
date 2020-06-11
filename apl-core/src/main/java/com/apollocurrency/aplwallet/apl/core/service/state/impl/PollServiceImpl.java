/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.impl;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.Vote;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.poll.PollResultTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.poll.PollTable;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.state.poll.Poll;
import com.apollocurrency.aplwallet.apl.core.entity.state.poll.PollOptionResult;
import com.apollocurrency.aplwallet.apl.core.service.state.PollService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingPollCreation;
import com.apollocurrency.aplwallet.apl.util.Constants;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
@Slf4j
public class PollServiceImpl implements PollService {
    private final BlockchainProcessor blockchainProcessor;
    private final Blockchain blockchain;
    private final DatabaseManager databaseManager;
    private final PollTable pollTable;
    private final PollResultTable pollResultTable;
    private final IteratorToStreamConverter<Poll> converter;

    /**
     * Constructor for unit tests.
     *
     * @param blockchainProcessor
     * @param blockchain
     * @param databaseManager
     * @param pollTable
     * @param pollResultTable
     */
    public PollServiceImpl(
        final BlockchainProcessor blockchainProcessor,
        final Blockchain blockchain,
        final DatabaseManager databaseManager,
        final PollTable pollTable,
        final PollResultTable pollResultTable,
        final IteratorToStreamConverter<Poll> converter
    ) {
        this.blockchainProcessor = blockchainProcessor;
        this.blockchain = blockchain;
        this.databaseManager = databaseManager;
        this.pollTable = pollTable;
        this.pollResultTable = pollResultTable;
        this.converter = converter;
    }

    @Inject
    public PollServiceImpl(
        final BlockchainProcessor blockchainProcessor,
        final Blockchain blockchain,
        final DatabaseManager databaseManager,
        final PollTable pollTable,
        final PollResultTable pollResultTable
    ) {
        this.blockchainProcessor = blockchainProcessor;
        this.blockchain = blockchain;
        this.databaseManager = databaseManager;
        this.pollTable = pollTable;
        this.pollResultTable = pollResultTable;
        this.converter = new IteratorToStreamConverter<>();
    }

    @Override
    public void checkPolls(int currentHeight) {
        // select all Polls where 'finish_height' is EQUAL (DbClause.Op.EQ) then specified height value
        try (DbIterator<Poll> polls = pollTable.getPollsFinishingAtHeight(currentHeight)) {
            int index = 0;
            for (Poll poll : polls) {
                try {
                    List<PollOptionResult> results = countResults(
                        poll.getVoteWeighting(),
                        currentHeight,
                        poll.getId(),
                        poll.getAccountId(),
                        poll.getOptions().length
                    );
                    log.trace("checkPolls: height = {}, [{}] PollId = {} has = {}", currentHeight, index, poll.getId(), results.size());
                    pollResultTable.insert(results);
                    log.trace("checkPolls: height = {}, [{}] PollId = {} checked : {}", currentHeight, index, poll.getId(), results);
                    index++;
                } catch (RuntimeException e) {
                    log.error("Couldn't count RollResult for poll {} at height = {}", poll.getId(), currentHeight, e);
                }
            }
        }
    }

    @Override
    public Poll getPoll(long id) {
        return pollTable.getPoll(id);
    }

    @Override
    public Stream<Poll> getPollsFinishingAtOrBefore(int height, int from, int to) {
        return converter.convert(pollTable.getPollsFinishingAtOrBefore(height, from, to));
    }

    @Override
    public DbIterator<Poll> getPollsFinishingBelowHeight(int height, int from, int to) {
        return pollTable.getPollsFinishingBelowHeight(height, from, to);
    }

    @Override
    public Stream<Poll> getAllPolls(int from, int to) {
        return converter.convert(pollTable.getAll(from, to));
    }

    @Override
    public Stream<Poll> getActivePolls(int from, int to) {
        return converter.convert(pollTable.getActivePolls(from, to, blockchain.getHeight()));
    }

    @Override
    public Stream<Poll> getPollsByAccount(long accountId, boolean includeFinished, boolean finishedOnly, int from, int to) {
        return converter.convert(
            pollTable.getPollsByAccount(accountId, includeFinished, finishedOnly, from, to, blockchain.getHeight())
        );
    }

    @Override
    public Stream<Poll> getVotedPollsByAccount(long accountId, int from, int to) throws AplException.NotValidException {
        return converter.convert(
            pollTable.getVotedPollsByAccount(databaseManager.getDataSource(), accountId, from, to)
        );
    }

    @Override
    public Stream<Poll> searchPolls(String query, boolean includeFinished, int from, int to) {
        return converter.convert(
            pollTable.searchPolls(query, includeFinished, from, to, blockchain.getHeight())
        );
    }

    @Override
    public int getCount() {
        return pollTable.getCount();
    }

    @Override
    public void addPoll(Transaction transaction, MessagingPollCreation attachment) {
        pollTable.addPoll(transaction, attachment, blockchain.getLastBlockTimestamp(), blockchain.getHeight());
    }

    @Override
    public List<PollOptionResult> getResults(final VoteWeighting voteWeighting, final Poll poll) {
        Objects.requireNonNull(voteWeighting, "voteWeighting is not supposed to be null.");

        if (voteWeighting.equals(poll.getVoteWeighting())) {
            return getResults(poll);
        } else {
            return countResults(voteWeighting, poll);
        }
    }

    @Override
    public List<PollOptionResult> getResults(final Poll poll) {
        if (isFinished(poll.getFinishHeight())) {
            return pollResultTable.get(pollTable.getDbKey(poll)).stream().filter(r -> !r.isUndefined()).collect(Collectors.toList());
        } else {
            return countResults(poll.getVoteWeighting(), poll);
        }
    }

    private List<PollOptionResult> countResults(VoteWeighting voteWeighting, Poll poll) {
        int countHeight = Math.min(poll.getFinishHeight(), blockchain.getHeight());
        if (countHeight < blockchainProcessor.getMinRollbackHeight()) {
            return null;
        }
        return countResults(voteWeighting, countHeight, poll.getId(), poll.getAccountId(), poll.getOptions().length);
    }

    private List<PollOptionResult> countResults(
        final VoteWeighting voteWeighting,
        final int height,
        final long id,
        final long accountId,
        int optionsLength
    ) {
        final PollOptionResult[] result = new PollOptionResult[optionsLength];
        log.trace("count RollResult: START h={}, pollId={}, accountId = {}, {}, voteList = [{}]",
            height, id, accountId, voteWeighting, optionsLength);
        for (int i = 0; i < result.length; i++) {
            result[i] = new PollOptionResult(id, blockchain.getHeight());
        }
        VoteWeighting.VotingModel votingModel = voteWeighting.getVotingModel();
        try (DbIterator<Vote> votes = Vote.getVotes(id, 0, -1)) {
            List<Vote> voteList = CollectionUtil.toList(votes);
            if (voteList.isEmpty()) {
                // stop further processing because there are no votes found
                log.trace("count RollResult: END 1. pollId={}, accountId={} PollOptionResult = {}", id, accountId, result);
                return Arrays.asList(result);
            }
            log.trace("count RollResult: h={}, pollId={}, votingModel={}, voteList = [{}]",
                height, id, votingModel, voteList.size());
            for (Vote vote : voteList) {
                long weight = votingModel.calcWeight(voteWeighting, vote.getVoterId(), height);
                if (weight <= 0) {
                    continue;
                }
                long[] partialResult = countVote(vote, weight, optionsLength);
                for (int i = 0; i < partialResult.length; i++) {
                    if (partialResult[i] != Long.MIN_VALUE) {
                        if (result[i].isUndefined()) {
                            result[i] = new PollOptionResult(id, partialResult[i], weight, blockchain.getHeight());
                        } else {
                            result[i].add(partialResult[i], weight);
                        }
                    }
                }
            }
        }
        log.trace("count RollResult: END 2. pollId={}, accountId={} PollOptionResult = {}", id, accountId, result);
        return Arrays.asList(result);
    }

    private long[] countVote(Vote vote, long weight, int optionsLength) {
        final long[] partialResult = new long[optionsLength];
        final byte[] optionValues = vote.getVoteBytes();
        for (int i = 0; i < optionValues.length; i++) {
            if (optionValues[i] != Constants.NO_VOTE_VALUE) {
                partialResult[i] = (long) optionValues[i] * weight;
            } else {
                partialResult[i] = Long.MIN_VALUE;
            }
        }
        return partialResult;
    }

    @Override
    public boolean isFinished(final int finishHeight) {
        return finishHeight <= blockchain.getHeight();
    }
}
