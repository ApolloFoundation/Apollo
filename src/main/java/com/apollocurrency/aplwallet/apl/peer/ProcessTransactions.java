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

package com.apollocurrency.aplwallet.apl.peer;

import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class ProcessTransactions extends PeerServlet.PeerRequestHandler {

    private static class ProcessTransactionsHolder {
        private static final ProcessTransactions INSTANCE = new ProcessTransactions();
    }

    public static ProcessTransactions getInstance() {
        return ProcessTransactionsHolder.INSTANCE;
    }

    private ProcessTransactions() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        try {
            Apl.getTransactionProcessor().processPeerTransactions(request);
            return JSON.emptyJSON;
        } catch (RuntimeException | AplException.ValidationException e) {
            //Logger.logDebugMessage("Failed to parse peer transactions: " + request.toJSONString());
            peer.blacklist(e);
            return PeerServlet.error(e);
        }

    }

    @Override
    boolean rejectWhileDownloading() {
        return true;
    }

}
