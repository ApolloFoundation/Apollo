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
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.impl;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.Vote;
import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.poll.PollResultTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.poll.PollTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.poll.VoteTable;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.state.poll.Poll;
import com.apollocurrency.aplwallet.apl.core.entity.state.poll.PollOptionResult;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.PollOptionResultService;
import com.apollocurrency.aplwallet.apl.core.service.state.PollService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingPollCreation;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingVoteCasting;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Stream;

@Singleton
@Slf4j
public class PollServiceImpl implements PollService {
    private final BlockChainInfoService blockChainInfoService;
    private final PollTable pollTable;
    private final PollResultTable pollResultTable;
    private final IteratorToStreamConverter<Poll> converter;
    private final PollOptionResultService pollOptionResultService;
    private final VoteTable voteTable;
    private final BlockchainImpl blockchain;

    /**
     * Constructor for unit tests.
     *
     * @param blockChainInfoService
     * @param pollTable
     * @param pollResultTable
     * @param converter
     * @param pollOptionResultService
     */
    public PollServiceImpl(
        final BlockChainInfoService blockChainInfoService,
        final PollTable pollTable,
        final PollResultTable pollResultTable,
        final IteratorToStreamConverter<Poll> converter,
        final PollOptionResultService pollOptionResultService,
        final VoteTable voteTable,
        BlockchainImpl blockchain
    ) {
        this.blockChainInfoService = blockChainInfoService;
        this.pollTable = pollTable;
        this.pollResultTable = pollResultTable;
        this.converter = converter;
        this.pollOptionResultService = pollOptionResultService;
        this.voteTable = voteTable;
        this.blockchain = blockchain;
    }

    @Inject
    public PollServiceImpl(
        final BlockChainInfoService blockChainInfoService,
        final DatabaseManager databaseManager,
        final PollTable pollTable,
        final PollResultTable pollResultTable,
        final PollOptionResultService pollOptionResultService,
        final VoteTable voteTable,
        final BlockchainImpl blockchain
        ) {
        this.blockChainInfoService = blockChainInfoService;
        this.pollTable = pollTable;
        this.pollResultTable = pollResultTable;
        this.converter = new IteratorToStreamConverter<>();
        this.pollOptionResultService = pollOptionResultService;
        this.voteTable = voteTable;
        this.blockchain = blockchain;
    }

    @Override
    public void checkPolls(int currentHeight) {
        // select all Polls where 'finish_height' is EQUAL (DbClause.Op.EQ) then specified height value
        try (DbIterator<Poll> polls = pollTable.getPollsFinishingAtHeight(currentHeight)) {
            int index = 0;
            for (Poll poll : polls) {
                try {
                    List<PollOptionResult> results = pollOptionResultService.countResults(
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
    public Stream<Poll> getAllPolls(int from, int to) {
        return converter.convert(pollTable.getAll(from, to));
    }

    @Override
    public Stream<Poll> getActivePolls(int from, int to) {
        return converter.convert(pollTable.getActivePolls(from, to, blockChainInfoService.getHeight()));
    }

    @Override
    public Stream<Poll> getPollsByAccount(long accountId, boolean includeFinished, boolean finishedOnly, int from, int to) {
        return converter.convert(
            pollTable.getPollsByAccount(accountId, includeFinished, finishedOnly, from, to, blockChainInfoService.getHeight())
        );
    }

    @Override
    public Stream<Poll> getVotedPollsByAccount(long accountId, int from, int to) throws AplException.NotValidException {
        return converter.convert(
            pollTable.getVotedPollsByAccount(accountId, from, to)
        );
    }

    @Override
    public Stream<Poll> searchPolls(String query, boolean includeFinished, int from, int to) {
        return converter.convert(
            pollTable.searchPolls(query, includeFinished, from, to, blockChainInfoService.getHeight())
        );
    }

    @Override
    public int getCount() {
        return pollTable.getCount();
    }

    @Override
    public int getPollVoteCount() {
        return voteTable.getCount();
    }

    @Override
    public Stream<Vote> getVotes(long pollId, int from, int to) {
        return voteTable.getVotes(pollId, from, to).stream();
    }

    @Override
    public Vote getVote(long pollId, long voterId) {
        return voteTable.getVote(pollId, voterId);
    }

    @Override
    public Vote addVote(Transaction transaction, MessagingVoteCasting attachment) {
        return voteTable.addVote(transaction, attachment, blockchain.getLastBlock().getHeight());
    }

    @Override
    public void addPoll(Transaction transaction, MessagingPollCreation attachment) {
        pollTable.addPoll(transaction, attachment, blockChainInfoService.getLastBlockTimestamp(), blockChainInfoService.getHeight());
    }

    @Override
    public boolean isFinished(final int finishHeight) {
        return finishHeight <= blockChainInfoService.getHeight();
    }
}
