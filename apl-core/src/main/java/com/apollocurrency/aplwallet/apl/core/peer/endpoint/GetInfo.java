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

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import java.io.IOException;
import java.io.StringWriter;

import com.apollocurrency.aplwallet.api.p2p.PeerInfo;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerImpl;
import com.apollocurrency.aplwallet.apl.core.peer.Peers;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Vetoed
public final class GetInfo extends PeerRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(GetInfo.class);
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
    public JSONStreamAware processRequest(JSONObject req, Peer peer) {
        PeerImpl peerImpl = (PeerImpl)peer;
        PeerInfo pi = mapper.convertValue(req, PeerInfo.class);
        log.debug("GetInfo - PeerInfo = {}", pi);
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
                        log.debug("GetInfo: ignoring invalid announced address for " + peerImpl.getHost());
                        if (!peerImpl.verifyAnnouncedAddress(peerImpl.getAnnouncedAddress())) {
                            log.debug("GetInfo: old announced address for " + peerImpl.getHost() + " no longer valid");
                            Peers.setAnnouncedAddress(peerImpl, null);
                        }
                        peer.deactivate();
                        return INVALID_ANNOUNCED_ADDRESS;
                    }
                    if (!announcedAddress.equals(peerImpl.getAnnouncedAddress())) {
                        log.debug("GetInfo: peer " + peer.getHost() + " changed announced address from " + peer.getAnnouncedAddress() + " to " + announcedAddress);
                        int oldPort = peerImpl.getPort();
                        Peers.setAnnouncedAddress(peerImpl, announcedAddress);
                        if (peerImpl.getPort() != oldPort) {
                            // force checking connectivity to new announced port
                            peerImpl.deactivate();
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
//            log.debug("Invalid application. IP: {}, application: {}, removing", peerImpl.getHost(), pi.application);
            log.debug("Peer = {} Received Invalid App in PI = \n{}", peerImpl, pi);
            peerImpl.remove();
        }

        Version version = null;
        try {
            version = new Version(pi.version);
        }
        catch (Exception e) {
            log.error("Cannot parse version.", e);
            version = new Version(1, 0, 0);
        }
        log.trace("PEER-GetINFO: IP: {}, application: {} version {}", peerImpl.getHost(), pi.application, version);
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
        JSONStreamAware myPeerInfoResponse = Peers.getMyPeerInfoResponse();

        if (log.isTraceEnabled()) {
            try {
                StringWriter writer = new StringWriter(1000);
                JSON.writeJSONString(myPeerInfoResponse, writer);
                String response = writer.toString();
                log.trace("myPeerInfoResponse = {}", response);
            } catch (IOException e) {
                log.error("ERROR, DUMP myPeerInfoResponse", e);
            }
        }
        return myPeerInfoResponse;

    }

    @Override
   public  boolean rejectWhileDownloading() {
        return false;
    }

}
