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

import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.entity.state.poll.Poll;
import com.apollocurrency.aplwallet.apl.core.entity.state.poll.PollOptionResult;

import java.util.List;

/**
 * @author Konstantin Silaev on 6/12/2020
 */
public interface PollOptionResultService {
    List<PollOptionResult> getResultsByVoteWeightingAndPoll(VoteWeighting voteWeighting, Poll poll);

    List<PollOptionResult> getResultsByPoll(Poll poll);

    List<PollOptionResult> countResults(
        final VoteWeighting voteWeighting,
        final int height,
        final long id,
        final long accountId,
        int optionsLength
    );
}
