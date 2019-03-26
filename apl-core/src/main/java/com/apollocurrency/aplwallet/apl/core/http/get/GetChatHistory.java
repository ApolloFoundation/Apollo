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
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import javax.enterprise.inject.Vetoed;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
@Vetoed
public class GetChatHistory extends AbstractAPIRequestHandler {
    public GetChatHistory() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.MESSAGES}, "account1", "account2", "firstIndex", "lastIndex");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
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
