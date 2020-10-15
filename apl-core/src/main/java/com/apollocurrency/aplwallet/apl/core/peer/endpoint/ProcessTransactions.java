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

import com.apollocurrency.aplwallet.api.p2p.request.ProcessTransactionsRequest;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.parser.ProcessTransactionsRequestParser;
import com.apollocurrency.aplwallet.apl.core.rest.converter.TransactionDTOConverter;
import com.apollocurrency.aplwallet.apl.util.JSON;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public final class ProcessTransactions extends PeerRequestHandler {
    private final ProcessTransactionsRequestParser responseParser = new ProcessTransactionsRequestParser();
    private final TransactionDTOConverter dtoConverter;

    @Inject
    public ProcessTransactions(TransactionDTOConverter dtoConverter) {
        this.dtoConverter = dtoConverter;
    }

    @Override
    public JSONStreamAware processRequest(JSONObject request, Peer peer) {

        try {
            long startTime = System.currentTimeMillis();
            log.trace("---start json conversion {}", startTime);
            ProcessTransactionsRequest transactionsRequest = responseParser.parse(request);
            log.trace("---end json conversion in {} ms, tx count={}", System.currentTimeMillis() - startTime, transactionsRequest.transactions.size());
            List<Transaction> transactions = transactionsRequest.transactions
                .stream()
                .map(dtoConverter::convert)
                .collect(Collectors.toList());

            log.trace("Will process {} peer transactions from {}", transactions.size(), peer.getAnnouncedAddress());
            lookupTransactionProcessor().processPeerTransactions(transactions);
            return JSON.emptyJSON;
        } catch (AplException.ValidationException | RuntimeException e) {
            //LOG.debug("Failed to parse peer transactions: " + request.toJSONString());
            peer.blacklist(e);
            return PeerResponses.error(e);
        }

    }

    @Override
    public boolean rejectWhileDownloading() {
        return true;
    }

}
