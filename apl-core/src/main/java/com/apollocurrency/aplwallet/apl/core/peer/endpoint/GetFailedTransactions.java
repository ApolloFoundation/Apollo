/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.endpoint;

import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

/**
 * @author Andrii Boiarskyi
 * @see
 * @since 1.48.4
 */
public class GetFailedTransactions extends PeerRequestHandler{
    @Override
    public JSONStreamAware processRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();
        request.get("fromBlock")

        return null;
    }

    @Override
    public boolean rejectWhileDownloading() {
        return false;
    }
}
