/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.entity.appdata.ChatInfo;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionService;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Vetoed
public class GetChats extends AbstractAPIRequestHandler {
    private TransactionService txService = CDI.current().select(TransactionService.class).get();

    public GetChats() {
        super(new APITag[]{APITag.MESSAGES, APITag.ACCOUNTS}, "account", "firstIndex", "lastIndex");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        long account = HttpParameterParserUtil.getAccountId(request, true);
        int firstIndex = HttpParameterParserUtil.getFirstIndex(request);
        int lastIndex = HttpParameterParserUtil.getLastIndex(request);
        JSONObject response = new JSONObject();
        JSONArray chatJsonArray = new JSONArray();
        List<ChatInfo> chatAccounts = txService.getChatAccounts(account, firstIndex, lastIndex);
        for (ChatInfo chat : chatAccounts) {
            JSONObject chatJson = new JSONObject();
            JSONData.putAccount(chatJson, "account", chat.getAccount());
            chatJson.put("lastMessageTime", chat.getLastMessageTime());
            chatJsonArray.add(chatJson);
        }
        response.put("chats", chatJsonArray);
        return response;
    }
}
