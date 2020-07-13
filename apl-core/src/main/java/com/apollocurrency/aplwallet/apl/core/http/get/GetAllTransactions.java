/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.transaction.Payment;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.PRIVATE_TRANSACTIONS_ACCESS_DENIED;

@Vetoed
public class GetAllTransactions extends AbstractAPIRequestHandler {

    public GetAllTransactions() {
        super(new APITag[]{APITag.TRANSACTIONS}, "type", "subtype", "firstIndex", "lastIndex");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        byte type = HttpParameterParserUtil.getByteOrNegative(req, "type", false);
        byte subtype = HttpParameterParserUtil.getByteOrNegative(req, "subtype", false);
        if (TransactionType.findTransactionType(type, subtype) == Payment.PRIVATE) {
            return PRIVATE_TRANSACTIONS_ACCESS_DENIED;
        }
        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);

        JSONArray transactions = new JSONArray();
        try (DbIterator<? extends Transaction> iterator = lookupBlockchain().getTransactions(type, subtype, firstIndex, lastIndex)) {
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
