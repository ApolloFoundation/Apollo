/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.peer.Peer;
import com.apollocurrency.aplwallet.apl.peer.Peers;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.MISSING_PEER;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.UNKNOWN_PEER;

public class BlacklistAPIProxyPeer extends APIServlet.APIRequestHandler {

    private static class BlacklistAPIProxyPeerHolder {
        private static final BlacklistAPIProxyPeer INSTANCE = new BlacklistAPIProxyPeer();
    }

    public static BlacklistAPIProxyPeer getInstance() {
        return BlacklistAPIProxyPeerHolder.INSTANCE;
    }

    private BlacklistAPIProxyPeer() {
        super(new APITag[] {APITag.NETWORK}, "peer");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        String peerAddress = Convert.emptyToNull(request.getParameter("peer"));
        if (peerAddress == null) {
            return MISSING_PEER;
        }
        Peer peer = Peers.findOrCreatePeer(peerAddress, true);
        JSONObject response = new JSONObject();
        if (peer == null) {
            return UNKNOWN_PEER;
        } else {
            response.put("done", APIProxy.getInstance().blacklistHost(peer.getHost()));
        }

        return response;
    }

    @Override
    protected final boolean requirePost() {
        return true;
    }

    @Override
    protected boolean requirePassword() {
        return true;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }
}
