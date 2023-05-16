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

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.p2p.request.GetTransactionsRequest;
import com.apollocurrency.aplwallet.api.p2p.response.GetTransactionsResponse;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.parser.GetTransactionsRequestParser;
import com.apollocurrency.aplwallet.apl.core.rest.converter.TransactionConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.TransactionConverterCreator;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.util.JSON;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Set;

/**
 * Get the transactions
 */
@Slf4j
@Singleton
public class GetTransactions extends PeerRequestHandler {
    private final TransactionConverter converter;
    private final GetTransactionsRequestParser requestParser;
    private final Blockchain blockchain;

    @Inject
    public GetTransactions(Blockchain blockchain, TransactionConverterCreator converterCreator, GetTransactionsRequestParser requestParser) {
        this.converter = converterCreator.create(false);
        this.requestParser = requestParser;
        this.blockchain = blockchain;
    }

    @Override
    public JSONStreamAware processRequest(JSONObject request, Peer peer) {
        GetTransactionsRequest jsonRequest = requestParser.parse(request);
        Set<Long> transactionIds = jsonRequest.getTransactionIds();
        //
        // Return the transactions to the caller
        //
        if (log.isTraceEnabled()) {
            log.trace("blockchain.getTransaction idList={}", transactionIds);
        }
        List<Transaction> transactions = blockchain.getTransactionsByIds(transactionIds);
        List<TransactionDTO> transactionDTOS = converter.convert(transactions);
        GetTransactionsResponse response = new GetTransactionsResponse(transactionDTOS);
        return JSON.objectToJson(response);
    }

    @Override
    public boolean rejectWhileDownloading() {
        return false;
    }
}
