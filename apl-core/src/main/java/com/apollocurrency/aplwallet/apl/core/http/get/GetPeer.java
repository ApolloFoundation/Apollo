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

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.Peers;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.MISSING_PEER;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.UNKNOWN_PEER;

public final class GetPeer extends AbstractAPIRequestHandler {

    private static class GetPeerHolder {
        private static final GetPeer INSTANCE = new GetPeer();
    }

    public static GetPeer getInstance() {
        return GetPeerHolder.INSTANCE;
    }

    private GetPeer() {
        super(new APITag[] {APITag.NETWORK}, "peer");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {

        String peerAddress = req.getParameter("peer");
        if (peerAddress == null) {
            return MISSING_PEER;
        }

        Peer peer = Peers.findOrCreatePeer(peerAddress, false);
        if (peer == null) {
            return UNKNOWN_PEER;
        }

        return JSONData.peer(peer);

    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

}
