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
import com.apollocurrency.aplwallet.apl.Version;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetInfo extends PeerServlet.PeerRequestHandler {

    private static class GetInfoHolder {
        private static final GetInfo INSTANCE = new GetInfo();
    }

    public static GetInfo getInstance() {
        return GetInfoHolder.INSTANCE;
    }

    private static final JSONStreamAware INVALID_ANNOUNCED_ADDRESS;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.INVALID_ANNOUNCED_ADDRESS);
        INVALID_ANNOUNCED_ADDRESS = JSON.prepare(response);
    }

    private GetInfo() {}

    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {
        PeerImpl peerImpl = (PeerImpl)peer;
        peerImpl.setLastUpdated(Apl.getEpochTime());
        long origServices = peerImpl.getServices();
        String servicesString = (String)request.get("services");
        peerImpl.setServices(servicesString != null ? Long.parseUnsignedLong(servicesString) : 0);
        peerImpl.analyzeHallmark((String)request.get("hallmark"));
        if (!Peers.ignorePeerAnnouncedAddress) {
            String announcedAddress = Convert.emptyToNull((String) request.get("announcedAddress"));
            if (announcedAddress != null) {
                announcedAddress = Peers.addressWithPort(announcedAddress.toLowerCase());
                if (announcedAddress != null) {
                    if (!peerImpl.verifyAnnouncedAddress(announcedAddress)) {
                        Logger.logDebugMessage("GetInfo: ignoring invalid announced address for " + peerImpl.getHost());
                        if (!peerImpl.verifyAnnouncedAddress(peerImpl.getAnnouncedAddress())) {
                            Logger.logDebugMessage("GetInfo: old announced address for " + peerImpl.getHost() + " no longer valid");
                            Peers.setAnnouncedAddress(peerImpl, null);
                        }
                        peerImpl.setState(Peer.State.NON_CONNECTED);
                        return INVALID_ANNOUNCED_ADDRESS;
                    }
                    if (!announcedAddress.equals(peerImpl.getAnnouncedAddress())) {
                        Logger.logDebugMessage("GetInfo: peer " + peer.getHost() + " changed announced address from " + peer.getAnnouncedAddress() + " to " + announcedAddress);
                        int oldPort = peerImpl.getPort();
                        Peers.setAnnouncedAddress(peerImpl, announcedAddress);
                        if (peerImpl.getPort() != oldPort) {
                            // force checking connectivity to new announced port
                            peerImpl.setState(Peer.State.NON_CONNECTED);
                        }
                    }
                } else {
                    Peers.setAnnouncedAddress(peerImpl, null);
                }
            }
        }
        String application = (String)request.get("application");
        if (application == null) {
            application = "?";
        }
        peerImpl.setApplication(application.trim());

        Version version = null;
        try {
            version = Version.from((String)request.get("version"));
        }
        catch (Exception e) {
            Logger.logErrorMessage("Cannot parse version.", e);
            version = new Version(1, 0, 0);
        }
        peerImpl.setVersion(version);

        String platform = (String)request.get("platform");
        if (platform == null) {
            platform = "?";
        }
        peerImpl.setPlatform(platform.trim());

        peerImpl.setShareAddress(Boolean.TRUE.equals(request.get("shareAddress")));

        peerImpl.setApiPort(request.get("apiPort"));
        peerImpl.setApiSSLPort(request.get("apiSSLPort"));
        peerImpl.setDisabledAPIs(request.get("disabledAPIs"));
        peerImpl.setApiServerIdleTimeout(request.get("apiServerIdleTimeout"));
        peerImpl.setBlockchainState(request.get("blockchainState"));

        if (peerImpl.getServices() != origServices) {
            Peers.notifyListeners(peerImpl, Peers.Event.CHANGED_SERVICES);
        }

        return Peers.getMyPeerInfoResponse();

    }

    @Override
    boolean rejectWhileDownloading() {
        return false;
    }

}
