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


import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Poll;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.POLL_RESULTS_NOT_AVAILABLE;
import javax.enterprise.inject.Vetoed;

@Vetoed
public class GetPollResult extends AbstractAPIRequestHandler {

    public GetPollResult() {
        super(new APITag[]{APITag.VS}, "poll", "votingModel", "holding", "minBalance", "minBalanceModel");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        Poll poll = ParameterParser.getPoll(req);
        List<Poll.OptionResult> pollResults;
        VoteWeighting voteWeighting;
        if (Convert.emptyToNull(req.getParameter("votingModel")) == null) {
            pollResults = poll.getResults();
            voteWeighting = poll.getVoteWeighting();
        } else {
            byte votingModel = ParameterParser.getByte(req, "votingModel", (byte)0, (byte)3, true);
            long holdingId = ParameterParser.getLong(req, "holding", Long.MIN_VALUE, Long.MAX_VALUE, false);
            long minBalance = ParameterParser.getLong(req, "minBalance", 0, Long.MAX_VALUE, false);
            byte minBalanceModel = ParameterParser.getByte(req, "minBalanceModel", (byte)0, (byte)3, false);
            voteWeighting = new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel);
            voteWeighting.validate();
            pollResults = poll.getResults(voteWeighting);
        }
        if (pollResults == null) {
            return POLL_RESULTS_NOT_AVAILABLE;
        }
        return JSONData.pollResults(poll, pollResults, voteWeighting);
    }
}
