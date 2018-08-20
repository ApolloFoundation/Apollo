/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Chat;
import com.apollocurrency.aplwallet.apl.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetChats extends APIServlet.APIRequestHandler {

    private static class GetChatsHolder {
        private static final GetChats INSTANCE = new GetChats();
    }

    public static GetChats getInstance() {
        return GetChatsHolder.INSTANCE;
    }

    private GetChats() {
        super(new APITag[] {APITag.MESSAGES, APITag.ACCOUNTS}, "account", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        long account = ParameterParser.getAccountId(request, true);
        int firstIndex = ParameterParser.getFirstIndex(request);
        int lastIndex = ParameterParser.getLastIndex(request);
        JSONObject response = new JSONObject();
        JSONArray chatJsonArray = new JSONArray();
        try (DbIterator<Chat.ChatInfo> iter = Chat.getChatAccounts(account, firstIndex, lastIndex)) {
            while (iter.hasNext()) {
                Chat.ChatInfo chat = iter.next();
                JSONObject chatJson = new JSONObject();
                chatJson.put("account", chat.getAccount());
                chatJson.put("lastMessageTime", chat.getLastMessageTime());
                chatJsonArray.add(chatJson);
            }
        }
        response.put("chats", chatJsonArray);
        return response;
    }
}
