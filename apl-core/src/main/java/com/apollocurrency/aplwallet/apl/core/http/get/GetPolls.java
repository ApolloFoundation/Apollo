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
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import javax.enterprise.inject.Vetoed;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

@Vetoed
public class GetPolls extends AbstractAPIRequestHandler {

    public GetPolls() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.VS}, "account", "firstIndex", "lastIndex", "timestamp", "includeFinished", "finishedOnly");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
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
                    polls = Poll.getPollsFinishingAtOrBefore(lookupBlockchain().getHeight(), firstIndex, lastIndex);
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
