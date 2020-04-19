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

import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public final class ProcessTransactions extends PeerRequestHandler {
    private static final Logger LOG = getLogger(ProcessTransactions.class);

    public ProcessTransactions() {
    }


    @Override
    public JSONStreamAware processRequest(JSONObject request, Peer peer) {

        try {
            lookupTransactionProcessor().processPeerTransactions(request);
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
