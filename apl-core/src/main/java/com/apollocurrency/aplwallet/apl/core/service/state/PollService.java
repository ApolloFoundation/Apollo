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


package com.apollocurrency.aplwallet.apl.core.service.state;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.Vote;
import com.apollocurrency.aplwallet.apl.core.entity.state.poll.Poll;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingPollCreation;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingVoteCasting;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;

import java.util.stream.Stream;

/**
 * @author silaev-firstbridge on 6/11/2020
 */
public interface PollService {
    void checkPolls(int currentHeight);

    Poll getPoll(long id);

    Stream<Poll> getPollsFinishingAtOrBefore(int height, int from, int to);

    Stream<Poll> getAllPolls(int from, int to);

    Stream<Poll> getActivePolls(int from, int to);

    Stream<Poll> getPollsByAccount(long accountId, boolean includeFinished, boolean finishedOnly, int from, int to);

    Stream<Poll> getVotedPollsByAccount(long accountId, int from, int to) throws AplException.NotValidException;

    Stream<Poll> searchPolls(String query, boolean includeFinished, int from, int to);

    int getCount();

    int getPollVoteCount();

    void addPoll(Transaction transaction, MessagingPollCreation attachment);

    Stream<Vote> getVotes(long pollId, int from, int to);

    Vote getVote(long pollId, long voterId);

    Vote addVote(Transaction transaction, MessagingVoteCasting attachment);

    boolean isFinished(int finishHeight);
}
