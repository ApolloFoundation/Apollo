/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Poll;
import com.apollocurrency.aplwallet.apl.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetVotedAccountPolls extends APIServlet.APIRequestHandler {
    private static class GetVotedAccountPollsHolder {
        private static final GetVotedAccountPolls INSTANCE = new GetVotedAccountPolls();
    }

    public static GetVotedAccountPolls getInstance() {
        return GetVotedAccountPollsHolder.INSTANCE;
    }
    private GetVotedAccountPolls() {
        super(new APITag[] {APITag.VS, APITag.ACCOUNTS }, "account", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        long account = ParameterParser.getAccountId(request, true);
        int firstIndex = ParameterParser.getFirstIndex(request);
        int lastIndex = ParameterParser.getLastIndex(request);
        JSONObject response = new JSONObject();
        JSONArray pollJsonArray = new JSONArray();
        try (DbIterator<Poll> pollDbIterator = Poll.getVotedPollsByAccount(account, firstIndex, lastIndex)) {
            while (pollDbIterator.hasNext()) {
                pollJsonArray.add(JSONData.poll(pollDbIterator.next()));
            }
        }
        response.put("polls", pollJsonArray);
        return response;
    }
}
