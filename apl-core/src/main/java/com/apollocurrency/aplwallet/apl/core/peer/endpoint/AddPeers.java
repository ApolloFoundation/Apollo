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
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.util.JSON;
import javax.enterprise.inject.spi.CDI;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class AddPeers extends PeerRequestHandler {
    private static PeersService peers = CDI.current().select(PeersService.class).get(); 
    
    public AddPeers() {}

    @Override
    public JSONStreamAware processRequest(JSONObject request, Peer peer) {
        final JSONArray peersArray = (JSONArray)request.get("peers");
        if (peersArray != null && peers.getMorePeers && !peers.hasTooManyKnownPeers()) {
            final JSONArray services = (JSONArray)request.get("services");
            final boolean setServices = (services != null && services.size() == peersArray.size());
            peers.peersExecutorService.submit(() -> {
                for (int i=0; i<peersArray.size(); i++) {
                    String announcedAddress = (String)peersArray.get(i);
                    PeerImpl newPeer = peers.findOrCreatePeer(null, announcedAddress, true);
                    if (newPeer != null) {
                        if (peers.addPeer(newPeer) && setServices) {
                            newPeer.setServices(Long.parseUnsignedLong((String)services.get(i)));
                        }
                        if (peers.hasTooManyKnownPeers()) {
                            break;
                        }
                    }
                }
            });
        }
        return JSON.emptyJSON;
    }

    @Override
    public boolean rejectWhileDownloading() {
        return false;
    }

}
