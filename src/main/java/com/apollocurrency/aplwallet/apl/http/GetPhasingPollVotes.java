/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.PhasingPoll;
import com.apollocurrency.aplwallet.apl.PhasingVote;
import com.apollocurrency.aplwallet.apl.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetPhasingPollVotes extends APIServlet.APIRequestHandler  {
    private static class GetPhasingPollVotesHolder {
        private static final GetPhasingPollVotes INSTANCE = new GetPhasingPollVotes();
    }

    public static GetPhasingPollVotes getInstance() {
        return GetPhasingPollVotesHolder.INSTANCE;
    }

    private GetPhasingPollVotes() {
        super(new APITag[] {APITag.PHASING}, "transaction", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        long transactionId = ParameterParser.getUnsignedLong(req, "transaction", true);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        PhasingPoll phasingPoll = PhasingPoll.getPoll(transactionId);
        if (phasingPoll != null) {
            JSONObject response = new JSONObject();
            JSONArray votesJSON = new JSONArray();
            try (DbIterator<PhasingVote> votes = PhasingVote.getVotes(transactionId, firstIndex, lastIndex)) {
                for (PhasingVote vote : votes) {
                    votesJSON.add(JSONData.phasingPollVote(vote));
                }
            }
            response.put("votes", votesJSON);
            return response;
        }
        return JSONResponses.UNKNOWN_TRANSACTION;
    }
}
