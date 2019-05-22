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

package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.api.p2p.PeerInfo;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import javax.enterprise.inject.spi.CDI;

import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import com.apollocurrency.aplwallet.apl.util.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import javax.enterprise.inject.Vetoed;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Vetoed
final class GetInfo extends PeerRequestHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GetInfo.class);
    private static volatile EpochTime timeService = CDI.current().select(EpochTime.class).get();
    private  ObjectMapper mapper = new ObjectMapper();
 
    private static final JSONStreamAware INVALID_ANNOUNCED_ADDRESS;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.INVALID_ANNOUNCED_ADDRESS);
        INVALID_ANNOUNCED_ADDRESS = JSON.prepare(response);
    }

    public GetInfo() {
       mapper.registerModule(new JsonOrgModule()); 
    }

    @Override
    protected boolean isChainIdProtected() {
        return false;
    }

    @Override
    JSONStreamAware processRequest(JSONObject req, Peer peer) {
        PeerImpl peerImpl = (PeerImpl)peer;
        PeerInfo pi = mapper.convertValue(req, PeerInfo.class);
        peerImpl.setLastUpdated(timeService.getEpochTime());
        long origServices = peerImpl.getServices();
        String servicesString = pi.services;
        String announcedAddress = null;
        peerImpl.setServices(servicesString != null ? Long.parseUnsignedLong(servicesString) : 0);
        peerImpl.analyzeHallmark(pi.hallmark);
        if (!Peers.ignorePeerAnnouncedAddress) {
           announcedAddress = Convert.emptyToNull(pi.announcedAddress);
            if (announcedAddress != null) {
                announcedAddress = announcedAddress.toLowerCase();
                if (announcedAddress != null) {
                    if (!peerImpl.verifyAnnouncedAddress(announcedAddress)) {
                        LOG.debug("GetInfo: ignoring invalid announced address for " + peerImpl.getHost());
                        if (!peerImpl.verifyAnnouncedAddress(peerImpl.getAnnouncedAddress())) {
                            LOG.debug("GetInfo: old announced address for " + peerImpl.getHost() + " no longer valid");
                            Peers.setAnnouncedAddress(peerImpl, null);
                        }
                        peerImpl.setState(Peer.State.NON_CONNECTED);
                        return INVALID_ANNOUNCED_ADDRESS;
                    }
                    if (!announcedAddress.equals(peerImpl.getAnnouncedAddress())) {
                        LOG.debug("GetInfo: peer " + peer.getHost() + " changed announced address from " + peer.getAnnouncedAddress() + " to " + announcedAddress);
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
        if (pi.application == null) {
            pi.application = "?";
        }

        if(!peerImpl.setApplication(pi.application.trim())){
            LOG.debug("Invalid application. IP: {}, application: {}, removng", peerImpl.getHost(),pi.application);
            peerImpl.remove();
        }

        Version version = null;
        try {
            version = new Version(pi.version);
        }
        catch (Exception e) {
            LOG.error("Cannot parse version.", e);
            version = new Version(1, 0, 0);
        }
        LOG.debug("PEER-GetINFO: IP: {}, application: {} version {}", peerImpl.getHost(), pi.application, version);
        peerImpl.setVersion(version);

        if (pi.platform == null) {
            pi.platform = "?";
        }
        peerImpl.setPlatform(pi.platform.trim());

        peerImpl.setShareAddress(pi.shareAddress);

        peerImpl.setApiPort(pi.apiPort);
        peerImpl.setApiSSLPort(pi.apiSSLPort);
        peerImpl.setDisabledAPIs(pi.disabledAPIs);
        peerImpl.setApiServerIdleTimeout(pi.apiServerIdleTimeout);
        peerImpl.setBlockchainState(pi.blockchainState);

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
