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

import com.apollocurrency.aplwallet.apl.Poll;
import com.apollocurrency.aplwallet.apl.db.DbIterator;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class SearchPolls extends APIServlet.APIRequestHandler {

    private static class SearchPollsHolder {
        private static final SearchPolls INSTANCE = new SearchPolls();
    }

    public static SearchPolls getInstance() {
        return SearchPollsHolder.INSTANCE;
    }

    private SearchPolls() {
        super(new APITag[] {APITag.VS, APITag.SEARCH}, "query", "firstIndex", "lastIndex", "includeFinished");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        String query = Convert.nullToEmpty(req.getParameter("query"));
        if (query.isEmpty()) {
            return JSONResponses.missing("query");
        }
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean includeFinished = "true".equalsIgnoreCase(req.getParameter("includeFinished"));

        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        try (DbIterator<Poll> polls = Poll.searchPolls(query, includeFinished, firstIndex, lastIndex)) {
            while (polls.hasNext()) {
                jsonArray.add(JSONData.poll(polls.next()));
            }
        }
        response.put("polls", jsonArray);
        return response;
    }

}
