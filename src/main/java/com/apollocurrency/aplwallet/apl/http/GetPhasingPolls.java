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

import com.apollocurrency.aplwallet.apl.PhasingPoll;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetPhasingPolls extends APIServlet.APIRequestHandler {

    private static class GetPhasingPollsHolder {
        private static final GetPhasingPolls INSTANCE = new GetPhasingPolls();
    }

    public static GetPhasingPolls getInstance() {
        return GetPhasingPollsHolder.INSTANCE;
    }

    private GetPhasingPolls() {
        super(new APITag[] {APITag.PHASING}, "transaction", "transaction", "transaction", "countVotes"); // limit to 3 for testing
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        long[] transactionIds = ParameterParser.getUnsignedLongs(req, "transaction");
        boolean countVotes = "true".equalsIgnoreCase(req.getParameter("countVotes"));
        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        response.put("polls", jsonArray);
        for (long transactionId : transactionIds) {
            PhasingPoll poll = PhasingPoll.getPoll(transactionId);
            if (poll != null) {
                jsonArray.add(JSONData.phasingPoll(poll, countVotes));
            } else {
                PhasingPoll.PhasingPollResult pollResult = PhasingPoll.getResult(transactionId);
                if (pollResult != null) {
                    jsonArray.add(JSONData.phasingPollResult(pollResult));
                }
            }
        }
        return response;
    }

}
