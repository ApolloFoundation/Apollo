/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Chat;
import com.apollocurrency.aplwallet.apl.Transaction;
import com.apollocurrency.aplwallet.apl.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetChatHistory extends APIServlet.APIRequestHandler {
    private static class GetChatHistoryHolder {
        private static final GetChatHistory INSTANCE = new GetChatHistory();
    }

    public static GetChatHistory getInstance() {
        return GetChatHistoryHolder.INSTANCE;
    }
    private GetChatHistory() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.MESSAGES}, "account1", "account2", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        long account1 = ParameterParser.getAccountId(request,"account1", true);
        long account2 = ParameterParser.getAccountId(request,"account2", true);
        int firstIndex = ParameterParser.getFirstIndex(request);
        int lastIndex = ParameterParser.getLastIndex(request);
        JSONObject response = new JSONObject();
        JSONArray chatJsonArray = new JSONArray();
        try (DbIterator<? extends Transaction> iter = Chat.getChatHistory(account1, account2, firstIndex, lastIndex)) {
            while (iter.hasNext()) {
                chatJsonArray.add(JSONData.transaction(false, iter.next()));
            }
        }
        response.put("chatHistory", chatJsonArray);
        return response;
    }
}
