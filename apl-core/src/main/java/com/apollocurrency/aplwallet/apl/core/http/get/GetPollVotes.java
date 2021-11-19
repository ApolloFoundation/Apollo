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

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.entity.state.poll.Poll;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;

@Vetoed
public class GetPollVotes extends AbstractAPIRequestHandler {

    public GetPollVotes() {
        super(new APITag[]{APITag.VS}, "poll", "firstIndex", "lastIndex", "includeWeights");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);
        boolean includeWeights = "true".equalsIgnoreCase(req.getParameter("includeWeights"));
        Poll poll = HttpParameterParserUtil.getPoll(req);
        int countHeight;

        JSONData.VoteWeighter weighter = null;
        if (includeWeights && (countHeight = Math.min(poll.getFinishHeight(), lookupBlockchain().getHeight()))
            >= lookupBlockchainProcessor().getMinRollbackHeight()) {
            VoteWeighting voteWeighting = poll.getVoteWeighting();
            VoteWeighting.VotingModel votingModel = voteWeighting.getVotingModel();
            weighter = voterId -> votingModel.calcWeight(voteWeighting, voterId, countHeight);
        }
        JSONArray votesJson = new JSONArray();
        JSONData.VoteWeighter finalWeighter = weighter;

        pollService.getVotes(poll.getId(), firstIndex, lastIndex)
            .forEach(vote -> votesJson.add(JSONData.vote(vote, finalWeighter)));

        JSONObject response = new JSONObject();
        response.put("votes", votesJson);
        return response;
    }
}
