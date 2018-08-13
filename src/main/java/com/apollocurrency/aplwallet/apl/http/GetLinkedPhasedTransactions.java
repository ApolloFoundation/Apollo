/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.PhasingPoll;
import com.apollocurrency.aplwallet.apl.Transaction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class GetLinkedPhasedTransactions extends APIServlet.APIRequestHandler {
    private static class GetLinkedPhasedTransactionsHolder {
        private static final GetLinkedPhasedTransactions INSTANCE = new GetLinkedPhasedTransactions();
    }

    public static GetLinkedPhasedTransactions getInstance() {
        return GetLinkedPhasedTransactionsHolder.INSTANCE;
    }

    private GetLinkedPhasedTransactions() {
        super(new APITag[]{APITag.PHASING}, "linkedFullHash");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        byte[] linkedFullHash = ParameterParser.getBytes(req, "linkedFullHash", true);

        JSONArray json = new JSONArray();
        List<? extends Transaction> transactions = PhasingPoll.getLinkedPhasedTransactions(linkedFullHash);
        transactions.forEach(transaction -> json.add(JSONData.transaction(false, transaction)));
        JSONObject response = new JSONObject();
        response.put("transactions", json);

        return response;
    }
}
