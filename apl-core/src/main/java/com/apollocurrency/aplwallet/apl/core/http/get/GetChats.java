/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Chat;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetChats extends AbstractAPIRequestHandler {

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
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        long account = ParameterParser.getAccountId(request, true);
        int firstIndex = ParameterParser.getFirstIndex(request);
        int lastIndex = ParameterParser.getLastIndex(request);
        JSONObject response = new JSONObject();
        JSONArray chatJsonArray = new JSONArray();
        try (DbIterator<Chat.ChatInfo> iter = Chat.getChatAccounts(account, firstIndex, lastIndex)) {
            while (iter.hasNext()) {
                Chat.ChatInfo chat = iter.next();
                JSONObject chatJson = new JSONObject();
                JSONData.putAccount(chatJson, "account", chat.getAccount());
                chatJson.put("lastMessageTime", chat.getLastMessageTime());
                chatJsonArray.add(chatJson);
            }
        }
        response.put("chats", chatJsonArray);
        return response;
    }
}
