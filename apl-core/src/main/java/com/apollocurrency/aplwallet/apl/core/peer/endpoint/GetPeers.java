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

package com.apollocurrency.aplwallet.apl.core.peer.endpoint;

import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerImpl;
import com.apollocurrency.aplwallet.apl.core.peer.PeerState;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetPeers extends PeerRequestHandler {

    public GetPeers() {
    }

    @Override
    public JSONStreamAware processRequest(JSONObject request, Peer peer) {
        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        JSONArray services = new JSONArray();
        lookupPeersService().getAllPeers().forEach(otherPeer -> {
            if (!otherPeer.isBlacklisted() && otherPeer.getAnnouncedAddress() != null
                && otherPeer.getState() == PeerState.CONNECTED && otherPeer.shareAddress()) {
                jsonArray.add(otherPeer.getAnnouncedAddress());
                services.add(Long.toUnsignedString(((PeerImpl) otherPeer).getServices()));
            }
        });
        response.put("peers", jsonArray);
        response.put("services", services);         // Separate array for backwards compatibility
        return response;
    }

    @Override
    public boolean rejectWhileDownloading() {
        return false;
    }

}
