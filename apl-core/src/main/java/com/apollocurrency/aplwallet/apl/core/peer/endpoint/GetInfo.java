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

import com.apollocurrency.aplwallet.api.p2p.PeerInfo;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerImpl;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.Version;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringWriter;

public final class GetInfo extends PeerRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(GetInfo.class);
    private final TimeService timeService;

    private static final JSONStreamAware INVALID_ANNOUNCED_ADDRESS;
    private static final JSONStreamAware INVALID_APPLICATION;
    private static final JSONStreamAware INVALID_CHAINID;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.INVALID_ANNOUNCED_ADDRESS);
        INVALID_ANNOUNCED_ADDRESS = JSON.prepare(response);
    
        response = new JSONObject();
        response.put("error", Errors.INVALID_CHAINID);
        INVALID_CHAINID = JSON.prepare(response);
        
        response = new JSONObject();
        response.put("error", Errors.INVALID_APPLICATION);
        INVALID_APPLICATION = JSON.prepare(response);
    }
    
    @Inject
    public GetInfo(TimeService timeService) {
       this.timeService = timeService;
    }

    @Override
    public JSONStreamAware processRequest(JSONObject req, Peer peer) {
        PeerImpl peerImpl = (PeerImpl)peer;
        PeerInfo pi = mapper.convertValue(req, PeerInfo.class);
        log.trace("GetInfo - PeerInfo from request = {}", pi);
        peerImpl.setLastUpdated(timeService.getEpochTime());
        long origServices = peerImpl.getServices();
        String servicesString = pi.getServices();
        String announcedAddress;
        peerImpl.setServices(servicesString != null ? Long.parseUnsignedLong(servicesString) : 0);
        peerImpl.analyzeHallmark(pi.getHallmark());
        if (!PeersService.ignorePeerAnnouncedAddress) {
           announcedAddress = Convert.emptyToNull(pi.getAnnouncedAddress());
            if (announcedAddress != null) {
                announcedAddress = announcedAddress.toLowerCase();
                if (announcedAddress != null) {
                    if (!peerImpl.verifyAnnouncedAddress(announcedAddress)) {
                        log.trace("GetInfo: ignoring invalid announced address for " + peerImpl.getHost());
                        if (!peerImpl.verifyAnnouncedAddress(peerImpl.getAnnouncedAddress())) {
                            log.trace("GetInfo: old announced address for " + peerImpl.getHost() + " no longer valid");
                            lookupPeersService().setAnnouncedAddress(peerImpl, null);
                        }
                        peer.deactivate("Invalid announced address: "+announcedAddress);
                        return INVALID_ANNOUNCED_ADDRESS;
                    }
                    if (!announcedAddress.equals(peerImpl.getAnnouncedAddress())) {
                        log.trace("GetInfo: peer " + peer.getHost() + " changed announced address from " + peer.getAnnouncedAddress() + " to " + announcedAddress);
                        int oldPort = peerImpl.getPort();
                        lookupPeersService().setAnnouncedAddress(peerImpl, announcedAddress);
                    }
                } else {
                    lookupPeersService().setAnnouncedAddress(peerImpl, null);
                }
            }
        }

        if(!peerImpl.setApplication(pi.getApplication().trim())){
            log.trace("Invalid application. IP: {}, application value: '{}', removing", peerImpl.getHost(), pi.getApplication());
//            log.debug("Peer = {} Received Invalid App in PI = \n{}", peerImpl, pi);
            peerImpl.remove();
            return INVALID_APPLICATION;
        }

        if (!PeersService.myPI.getChainId().equalsIgnoreCase(pi.getChainId())) {
            peerImpl.remove();
            return INVALID_CHAINID;
        }
        
        Version version = null;
        try {
            version = new Version(pi.getVersion());
        }
        catch (Exception e) {
            log.error("Cannot parse version = '{}'", pi.getVersion(), e);
            version = new Version(1, 0, 0);
        }
        log.trace("PEER-GetINFO: IP: {}, application: {} version {}", peerImpl.getHost(), pi.getApplication(), version);
        peerImpl.setVersion(version);

        if (pi.getPlatform() == null) {
            log.warn("Setting Platform = '?' instead of Platform Value...");
            pi.setPlatform("?");
        }
        
        peerImpl.setPlatform(pi.getPlatform().trim());

        peerImpl.setShareAddress(pi.getShareAddress());

        peerImpl.setApiPort(pi.getApiPort());
        peerImpl.setApiSSLPort(pi.getApiSSLPort());
        peerImpl.setDisabledAPIs(pi.getDisabledAPIs());
        peerImpl.setApiServerIdleTimeout(pi.getApiServerIdleTimeout());
        peerImpl.setBlockchainState(pi.getBlockchainState());

        if (peerImpl.getServices() != origServices) {
            lookupPeersService().notifyListeners(peerImpl, PeersService.Event.CHANGED_SERVICES);
        }
        JSONStreamAware myPeerInfoResponse = lookupPeersService().getMyPeerInfoResponse();

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
        lookupPeersService().addPeer(peer);
        return myPeerInfoResponse;

    }

    @Override
   public  boolean rejectWhileDownloading() {
        return false;
    }

}
