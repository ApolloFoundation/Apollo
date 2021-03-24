/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

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
        TransactionTypes.TransactionTypeSpec privatePayment = TransactionTypes.TransactionTypeSpec.PRIVATE_PAYMENT;
        if (privatePayment.getType() == type && privatePayment.getSubtype() == subtype) {
            return PRIVATE_TRANSACTIONS_ACCESS_DENIED;
        }
        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);

        JSONArray transactions = new JSONArray();
        List<Transaction> txs = lookupBlockchain().getTransactions(type, subtype, firstIndex, lastIndex);

        txs.forEach(e-> {
            transactions.add(JSONData.transaction(e, false, false));
        });
        JSONObject response = new JSONObject();
        response.put("transactions", transactions);
        return response;

    }
}
