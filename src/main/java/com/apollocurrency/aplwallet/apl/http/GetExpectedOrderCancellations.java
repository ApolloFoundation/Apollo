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

import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Transaction;
import com.apollocurrency.aplwallet.apl.TransactionType;
import com.apollocurrency.aplwallet.apl.util.Filter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public final class GetExpectedOrderCancellations extends APIServlet.APIRequestHandler {

    private static class GetExpectedOrderCancellationsHolder {
        private static final GetExpectedOrderCancellations INSTANCE = new GetExpectedOrderCancellations();
    }

    public static GetExpectedOrderCancellations getInstance() {
        return GetExpectedOrderCancellationsHolder.INSTANCE;
    }

    private GetExpectedOrderCancellations() {
        super(new APITag[] {APITag.AE});
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        Filter<Transaction> filter = transaction -> transaction.getType() == TransactionType.ColoredCoins.ASK_ORDER_CANCELLATION
                || transaction.getType() == TransactionType.ColoredCoins.BID_ORDER_CANCELLATION;

        List<? extends Transaction> transactions = Apl.getBlockchain().getExpectedTransactions(filter);
        JSONArray cancellations = new JSONArray();
        transactions.forEach(transaction -> cancellations.add(JSONData.expectedOrderCancellation(transaction)));
        JSONObject response = new JSONObject();
        response.put("orderCancellations", cancellations);
        return response;
    }
}
