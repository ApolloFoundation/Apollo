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
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import javax.enterprise.inject.spi.CDI;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

/**
 * Get the transactions
 */
public class GetTransactions extends PeerServlet.PeerRequestHandler {
    private static PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get(); 
    private static class GetTransactionsHolder {
        private static final GetTransactions INSTANCE = new GetTransactions();
    }

    public static GetTransactions getInstance() {
        return GetTransactionsHolder.INSTANCE;
    }

    private GetTransactions() {}

    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {
        if (!propertiesHolder.INCLUDE_EXPIRED_PRUNABLE()) {
            return PeerServlet.UNSUPPORTED_REQUEST_TYPE;
        }
        JSONObject response = new JSONObject();
        JSONArray transactionArray = new JSONArray();
        JSONArray transactionIds = (JSONArray)request.get("transactionIds");
        Blockchain blockchain = lookupBlockchain();
        //
        // Return the transactions to the caller
        //
        if (transactionIds != null) {
            transactionIds.forEach(transactionId -> {
                long id = Long.parseUnsignedLong((String)transactionId);
                Transaction transaction = blockchain.getTransaction(id);
                if (transaction != null) {
                    transaction.getAppendages(true);
                    JSONObject transactionJSON = transaction.getJSONObject();
                    transactionArray.add(transactionJSON);
                }
            });
        }
        response.put("transactions", transactionArray);
        return response;
    }

    @Override
    boolean rejectWhileDownloading() {
        return true;
    }
}
