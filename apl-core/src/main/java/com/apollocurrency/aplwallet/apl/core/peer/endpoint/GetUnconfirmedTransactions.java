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

package com.apollocurrency.aplwallet.apl.core.peer.endpoint;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionSerializer;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.spi.CDI;
import java.util.List;
import java.util.SortedSet;

public final class GetUnconfirmedTransactions extends PeerRequestHandler {
    private TransactionSerializer transactionSerializer = CDI.current().select(TransactionSerializer.class).get();

    public GetUnconfirmedTransactions() {
    }


    @Override
    public JSONStreamAware processRequest(JSONObject request, Peer peer) {

        List<String> exclude = (List<String>) request.get("exclude");
        if (exclude == null) {
            return JSON.emptyJSON;
        }

        SortedSet<? extends Transaction> transactionSet = lookupTransactionProcessor().getCachedUnconfirmedTransactions(exclude);
        JSONArray transactionsData = new JSONArray();
        for (Transaction transaction : transactionSet) {
            if (transactionsData.size() >= 100) {
                break;
            }
            transactionsData.add(transactionSerializer.toJson(transaction));
        }
        JSONObject response = new JSONObject();
        response.put("unconfirmedTransactions", transactionsData);

        return response;
    }

    @Override
    public boolean rejectWhileDownloading() {
        return true;
    }

}
