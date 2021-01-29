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
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionJsonSerializer;
import com.apollocurrency.aplwallet.apl.util.JSON;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;
import java.util.List;
import java.util.Set;
@Slf4j
@Singleton
public final class GetUnconfirmedTransactions extends PeerRequestHandler {
    private final TransactionJsonSerializer transactionJsonSerializer = CDI.current().select(TransactionJsonSerializer.class).get();

    public GetUnconfirmedTransactions() {
    }


    @Override
    public JSONStreamAware processRequest(JSONObject request, Peer peer) {

        List<String> exclude = (List<String>) request.get("exclude");
        if (exclude == null) {
            return JSON.emptyJSON;
        }

        Set<UnconfirmedTransaction> transactionSet = lookupMemPool().getCachedUnconfirmedTransactions(exclude);
        log.trace("Return {} txs to peer {}", transactionSet.size(), peer.getHost());
        JSONArray transactionsData = new JSONArray();
        for (Transaction transaction : transactionSet) {
            if (transactionsData.size() >= 200) {
                break;
            }
            transactionsData.add(transactionJsonSerializer.toJson(transaction));
        }
        JSONObject response = new JSONObject();
        response.put("unconfirmedTransactions", transactionsData);

        return response;
    }

    @Override
    public boolean rejectWhileDownloading() {
        return false;
    }

}
