/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package com.apollocurrency.aplwallet.apl.peer;

import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.Transaction;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.List;
import java.util.SortedSet;

final class GetUnconfirmedTransactions extends PeerServlet.PeerRequestHandler {

    private static class GetUnconfirmedTransactionsHolder {
        private static final GetUnconfirmedTransactions INSTANCE = new GetUnconfirmedTransactions();
    }

    public static GetUnconfirmedTransactions getInstance() {
        return GetUnconfirmedTransactionsHolder.INSTANCE;
    }

    private GetUnconfirmedTransactions() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        List<String> exclude = (List<String>)request.get("exclude");
        if (exclude == null) {
            return JSON.emptyJSON;
        }

        SortedSet<? extends Transaction> transactionSet = Apl.getTransactionProcessor().getCachedUnconfirmedTransactions(exclude);
        JSONArray transactionsData = new JSONArray();
        for (Transaction transaction : transactionSet) {
            if (transactionsData.size() >= 100) {
                break;
            }
            transactionsData.add(transaction.getJSONObject());
        }
        JSONObject response = new JSONObject();
        response.put("unconfirmedTransactions", transactionsData);

        return response;
    }

    @Override
    boolean rejectWhileDownloading() {
        return true;
    }

}
