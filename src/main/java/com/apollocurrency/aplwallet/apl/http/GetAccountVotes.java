/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Poll;
import com.apollocurrency.aplwallet.apl.Transaction;
import com.apollocurrency.aplwallet.apl.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetAccountVotes extends APIServlet.APIRequestHandler {
    private static class GetAccountVotesHolder {
        private static final GetAccountVotes INSTANCE = new GetAccountVotes();
    }

    public static GetAccountVotes getInstance() {
        return GetAccountVotesHolder.INSTANCE;
    }
    private GetAccountVotes() {
        super(new APITag[] {APITag.VS, }, "account", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        long account = ParameterParser.getAccountId(request, true);
        int firstIndex = ParameterParser.getFirstIndex(request);
        int lastIndex = ParameterParser.getLastIndex(request);
        JSONObject response = new JSONObject();
        JSONArray pollJsonArray = new JSONArray();
        try (DbIterator<? extends Transaction> pollDbIterator = Poll.getPollsByAccount(account, firstIndex, lastIndex)) {
            while (pollDbIterator.hasNext()) {
                pollJsonArray.add(JSONData.transaction(false, pollDbIterator.next()));
            }
        }
        response.put("polls", pollJsonArray);
        return response;
    }
}
