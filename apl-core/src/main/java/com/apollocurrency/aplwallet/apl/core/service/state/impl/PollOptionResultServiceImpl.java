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

import com.apollocurrency.aplwallet.apl.core.entity.state.Vote;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.dao.state.poll.PollResultTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.poll.PollTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.poll.VoteTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.poll.Poll;
import com.apollocurrency.aplwallet.apl.core.entity.state.poll.PollOptionResult;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.PollOptionResultService;
import com.apollocurrency.aplwallet.apl.util.Constants;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class PollOptionResultServiceImpl implements PollOptionResultService {
    private final BlockChainInfoService blockChainInfoService;
    private final PollTable pollTable;
    private final PollResultTable pollResultTable;
    private final VoteTable voteTable;

    @Inject
    public PollOptionResultServiceImpl(
        final BlockChainInfoService blockChainInfoService,
        final PollTable pollTable,
        final PollResultTable pollResultTable,
        final VoteTable voteTable
        ) {
        this.blockChainInfoService = blockChainInfoService;
        this.pollTable = pollTable;
        this.pollResultTable = pollResultTable;
        this.voteTable = voteTable;
    }

    @Override
    public List<PollOptionResult> getResultsByVoteWeightingAndPoll(final VoteWeighting voteWeighting, final Poll poll) {
        Objects.requireNonNull(voteWeighting, "voteWeighting is not supposed to be null.");

        if (voteWeighting.equals(poll.getVoteWeighting())) {
            return getResultsByPoll(poll);
        } else {
            return countResults(voteWeighting, poll);
        }
    }

    @Override
    public List<PollOptionResult> getResultsByPoll(final Poll poll) {
        if (isFinished(poll.getFinishHeight())) {
            return pollResultTable.get(pollTable.getDbKey(poll)).stream().filter(r -> !r.isUndefined()).collect(Collectors.toList());
        } else {
            return countResults(poll.getVoteWeighting(), poll);
        }
    }

    private List<PollOptionResult> countResults(VoteWeighting voteWeighting, Poll poll) {
        int countHeight = Math.min(poll.getFinishHeight(), blockChainInfoService.getHeight());
        if (countHeight < blockChainInfoService.getMinRollbackHeight()) {
            return null;
        }
        return countResults(voteWeighting, countHeight, poll.getId(), poll.getAccountId(), poll.getOptions().length);
    }

    public List<PollOptionResult> countResults(
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
            result[i] = new PollOptionResult(id, blockChainInfoService.getHeight());
        }
        VoteWeighting.VotingModel votingModel = voteWeighting.getVotingModel();
        List<Vote> voteList = voteTable.getVotes(id, 0, -1);

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
                        result[i] = new PollOptionResult(id, partialResult[i], weight, blockChainInfoService.getHeight());
                    } else {
                        result[i].add(partialResult[i], weight);
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

    private boolean isFinished(final int finishHeight) {
        return finishHeight <= blockChainInfoService.getHeight();
    }
}