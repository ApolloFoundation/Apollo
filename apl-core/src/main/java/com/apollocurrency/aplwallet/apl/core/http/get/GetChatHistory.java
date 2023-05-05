/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
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
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Vetoed
public class GetChatHistory extends AbstractAPIRequestHandler {
    TransactionService txService = CDI.current().select(TransactionService.class).get();

    public GetChatHistory() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.MESSAGES}, "account1", "account2", "firstIndex", "lastIndex");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        long account1 = HttpParameterParserUtil.getAccountId(request, "account1", true);
        long account2 = HttpParameterParserUtil.getAccountId(request, "account2", true);
        int firstIndex = HttpParameterParserUtil.getFirstIndex(request);
        int lastIndex = HttpParameterParserUtil.getLastIndex(request);
        JSONObject response = new JSONObject();
        JSONArray chatJsonArray = new JSONArray();
        List<? extends Transaction> chatHistory = txService.getTransactionsChatHistory(account1, account2, firstIndex, lastIndex);
        chatHistory.forEach(e-> chatJsonArray.add(JSONData.transaction(false, e)));
        response.put("chatHistory", chatJsonArray);
        return response;
    }
}
