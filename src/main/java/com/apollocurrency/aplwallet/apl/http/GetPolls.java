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
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;


import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Poll;
import com.apollocurrency.aplwallet.apl.db.DbIterator;
import com.apollocurrency.aplwallet.apl.db.DbUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetPolls extends APIServlet.APIRequestHandler {

    private static class GetPollsHolder {
        private static final GetPolls INSTANCE = new GetPolls();
    }

    public static GetPolls getInstance() {
        return GetPollsHolder.INSTANCE;
    }

    private GetPolls() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.VS}, "account", "firstIndex", "lastIndex", "timestamp", "includeFinished", "finishedOnly");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        long accountId = ParameterParser.getAccountId(req, "account", false);
        boolean includeFinished = "true".equalsIgnoreCase(req.getParameter("includeFinished"));
        boolean finishedOnly = "true".equalsIgnoreCase(req.getParameter("finishedOnly"));
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        final int timestamp = ParameterParser.getTimestamp(req);

        JSONArray pollsJson = new JSONArray();
        DbIterator<Poll> polls = null;
        try {
            if (accountId == 0) {
                if (finishedOnly) {
                    polls = Poll.getPollsFinishingAtOrBefore(Apl.getBlockchain().getHeight(), firstIndex, lastIndex);
                } else if (includeFinished) {
                    polls = Poll.getAllPolls(firstIndex, lastIndex);
                } else {
                    polls = Poll.getActivePolls(firstIndex, lastIndex);
                }
            } else {
                polls = Poll.getPollsByAccount(accountId, includeFinished, finishedOnly, firstIndex, lastIndex);
            }
            while (polls.hasNext()) {
                Poll poll = polls.next();
                if (poll.getTimestamp() < timestamp) {
                    break;
                }
                pollsJson.add(JSONData.poll(poll));
            }
        } finally {
            DbUtils.close(polls);
        }

        JSONObject response = new JSONObject();
        response.put("polls", pollsJson);
        return response;
    }
}
