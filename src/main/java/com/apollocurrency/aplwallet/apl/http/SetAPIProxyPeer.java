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

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.peer.Peer;
import com.apollocurrency.aplwallet.apl.peer.Peers;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.API_PROXY_NO_OPEN_API_PEERS;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.PEER_NOT_CONNECTED;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.PEER_NOT_OPEN_API;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.UNKNOWN_PEER;

public class SetAPIProxyPeer extends APIServlet.APIRequestHandler {

    private static class SetAPIProxyPeerHolder {
        private static final SetAPIProxyPeer INSTANCE = new SetAPIProxyPeer();
    }

    public static SetAPIProxyPeer getInstance() {
        return SetAPIProxyPeerHolder.INSTANCE;
    }

    private SetAPIProxyPeer() {
        super(new APITag[] {APITag.NETWORK}, "peer");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        String peerAddress = Convert.emptyToNull(request.getParameter("peer"));
        if (peerAddress == null) {
            Peer peer = APIProxy.getInstance().setForcedPeer(null);
            if (peer == null) {
                return API_PROXY_NO_OPEN_API_PEERS;
            }
            return JSONData.peer(peer);
        }
        Peer peer = Peers.findOrCreatePeer(peerAddress, false);
        if (peer == null) {
            return UNKNOWN_PEER;
        }
        if (peer.getState() != Peer.State.CONNECTED ) {
            return PEER_NOT_CONNECTED;
        }
        if (!peer.isOpenAPI()) {
            return PEER_NOT_OPEN_API;
        }
        APIProxy.getInstance().setForcedPeer(peer);
        return JSONData.peer(peer);
    }

    @Override
    protected boolean requirePost() {
        return true;
    }

    @Override
    protected boolean requirePassword() {
        return true;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }


}
