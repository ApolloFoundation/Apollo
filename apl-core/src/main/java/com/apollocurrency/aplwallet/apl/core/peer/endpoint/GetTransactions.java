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
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionSerializer;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;

/**
 * Get the transactions
 */
@Singleton
public class GetTransactions extends PeerRequestHandler {
    private PropertiesHolder propertiesHolder;// = CDI.current().select(PropertiesHolder.class).get();
    private TransactionSerializer transactionSerializer;// = CDI.current().select(TransactionSerializer.class).get();

    public GetTransactions() {
    }

    @Inject
    public GetTransactions(PropertiesHolder propertiesHolder,
                           TransactionSerializer transactionSerializer) {
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder);
        this.transactionSerializer = Objects.requireNonNull(transactionSerializer);
    }

    @Override
    public JSONStreamAware processRequest(JSONObject request, Peer peer) {
        if (!propertiesHolder.INCLUDE_EXPIRED_PRUNABLE()) {
            return PeerResponses.UNSUPPORTED_REQUEST_TYPE;
        }
        JSONObject response = new JSONObject();
        JSONArray transactionArray = new JSONArray();
        JSONArray transactionIds = (JSONArray) request.get("transactionIds");
        Blockchain blockchain = lookupBlockchain();
        //
        // Return the transactions to the caller
        //
        if (transactionIds != null) {
            transactionIds.forEach(transactionId -> {
                long id = Long.parseUnsignedLong((String) transactionId);
                Transaction transaction = blockchain.getTransaction(id);
                if (transaction != null) {
                    JSONObject transactionJSON = transactionSerializer.toJson(transaction);
                    transactionArray.add(transactionJSON);
                }
            });
        }
        response.put("transactions", transactionArray);
        return response;
    }

    @Override
    public boolean rejectWhileDownloading() {
        return false;
    }
}
