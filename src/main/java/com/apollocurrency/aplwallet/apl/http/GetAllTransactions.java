/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Transaction;
import com.apollocurrency.aplwallet.apl.TransactionType;
import com.apollocurrency.aplwallet.apl.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.PRIVATE_TRANSACTIONS_ACCESS_DENIED;


public class GetAllTransactions extends APIServlet.APIRequestHandler{

    private static class GetAllTransactionsHolder {
        private static final GetAllTransactions INSTANCE = new GetAllTransactions();
    }

    public static GetAllTransactions getInstance() {
        return GetAllTransactionsHolder.INSTANCE;
    }

    private GetAllTransactions() {
        super(new APITag[] {APITag.TRANSACTIONS}, "type", "subtype", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        byte type = ParameterParser.getByteOrNegative(req, "type", false);
        byte subtype = ParameterParser.getByteOrNegative(req, "subtype", false);
        if (TransactionType.findTransactionType(type, subtype) == TransactionType.Payment.PRIVATE) {
            return PRIVATE_TRANSACTIONS_ACCESS_DENIED;
        }
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONArray transactions = new JSONArray();
        try (DbIterator<? extends Transaction> iterator = Apl.getBlockchain().getTransactions(type, subtype, firstIndex, lastIndex)) {
            while (iterator.hasNext()) {
                Transaction transaction = iterator.next();
                transactions.add(JSONData.transaction(transaction, false, false));
            }
        }
        JSONObject response = new JSONObject();
        response.put("transactions", transactions);
        return response;

    }
}
