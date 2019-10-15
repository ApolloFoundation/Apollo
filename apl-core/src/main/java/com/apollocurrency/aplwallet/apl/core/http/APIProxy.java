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

package com.apollocurrency.aplwallet.apl.core.http;

import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.task.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskOrder;
import org.slf4j.Logger;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static org.slf4j.LoggerFactory.getLogger;

@Vetoed
public class APIProxy {
    private static final Logger LOG = getLogger(APIProxy.class);
    private static final String BACKGROUND_SERVICE_NAME = "APIProxyService";

    public static final Set<String> NOT_FORWARDED_REQUESTS;
    
    @Vetoed
    private static class APIProxyHolder {
        private static final APIProxy INSTANCE = new APIProxy();
    }
    
    // TODO: YL remove static instance later
    private static final PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
    private static final BlockchainProcessor blockchainProcessor = CDI.current().select(BlockchainProcessor.class).get();
    private static TimeService timeService = CDI.current().select(TimeService.class).get();
    private static TaskDispatchManager taskDispatchManager = CDI.current().select(TaskDispatchManager.class).get();
    private static PeersService peers = CDI.current().select(PeersService.class).get(); 

    public static APIProxy getInstance() {
        return APIProxyHolder.INSTANCE;
    }

    static final boolean enableAPIProxy = propertiesHolder.isLightClient() ||
            (propertiesHolder.getBooleanProperty("apl.enableAPIProxy") && ! API.isOpenAPI);
    private static final int blacklistingPeriod = propertiesHolder.getIntProperty("apl.apiProxyBlacklistingPeriod") / 1000;
    static final String forcedServerURL = propertiesHolder.getStringProperty("apl.forceAPIProxyServerURL", "");

    private volatile String forcedPeerHost;
    private volatile List<String> peersHosts = Collections.emptyList();
    private volatile String mainPeerAnnouncedAddress;

    private final Map<String, Integer> blacklistedPeers = new ConcurrentHashMap<>();

    static {
        Set<String> requests = new HashSet<>();
        requests.add("getBlockchainStatus");
        requests.add("getState");

        final EnumSet<APITag> notForwardedTags = EnumSet.of(APITag.DEBUG, APITag.NETWORK);

        for (APIEnum api : APIEnum.values()) {
            AbstractAPIRequestHandler handler = api.getHandler();
            if (handler.requireBlockchain() && !Collections.disjoint(handler.getAPITags(), notForwardedTags)) {
                requests.add(api.getName());
            }
        }

        NOT_FORWARDED_REQUESTS = Collections.unmodifiableSet(requests);
    }

    private static final Runnable peersUpdateThread = () -> {
        int curTime = timeService.getEpochTime();
        getInstance().blacklistedPeers.entrySet().removeIf((entry) -> {
            if (entry.getValue() < curTime) {
                LOG.debug("Unblacklisting API peer " + entry.getKey());
                return true;
            }
            return false;
        });
        List<String> currentPeersHosts = getInstance().peersHosts;
        if (currentPeersHosts != null) {
            for (String host : currentPeersHosts) {
                Peer peer = peers.getPeer(host);
                if (peer != null) {
                    peers.connectPeer(peer);
                }
            }
        }
    };

    static {
        if (!propertiesHolder.isOffline() && enableAPIProxy) {
            taskDispatchManager.newBackgroundDispatcher(BACKGROUND_SERVICE_NAME)
                    .schedule(Task.builder()
                            .name("APIProxyPeersUpdate")
                            .delay(60000)
                            .task(peersUpdateThread)
                            .build(), TaskOrder.TASK);
        }
    }

    private APIProxy() {

    }

    public static void init() {}

    Peer getServingPeer(String requestType) {
        if (forcedPeerHost != null) {
            return peers.getPeer(forcedPeerHost);
        }

        APIEnum requestAPI = APIEnum.fromName(requestType);
        if (!peersHosts.isEmpty()) {
            for (String host : peersHosts) {
                Peer peer = peers.getPeer(host);
                if (peer != null && peer.isApiConnectable() && !peer.getDisabledAPIs().contains(requestAPI)) {
                    return peer;
                }
            }
        }

        List<Peer> connectablePeers = peers.getPeers(p -> p.isApiConnectable() && !blacklistedPeers.containsKey(p.getHost()));
        if (connectablePeers.isEmpty()) {
            return null;
        }
        // subset of connectable peers that have at least one new API enabled, which was disabled for the
        // The first peer (element 0 of peersHosts) is chosen at random. Next peers are chosen randomly from a
        // previously chosen peers. In worst case the size of peersHosts will be the number of APIs
        Peer peer = getRandomAPIPeer(connectablePeers);
        if (peer == null) {
            return null;
        }

        Peer resultPeer = null;
        List<String> currentPeersHosts = new ArrayList<>();
        EnumSet<APIEnum> disabledAPIs = EnumSet.noneOf(APIEnum.class);
        currentPeersHosts.add(peer.getHost());
        mainPeerAnnouncedAddress = peer.getAnnouncedAddress();
        if (!peer.getDisabledAPIs().contains(requestAPI)) {
            resultPeer = peer;
        }
        while (!disabledAPIs.isEmpty() && !connectablePeers.isEmpty()) {
            // remove all peers that do not introduce new enabled APIs
            connectablePeers.removeIf(p -> p.getDisabledAPIs().containsAll(disabledAPIs));
            peer = getRandomAPIPeer(connectablePeers);
            if (peer != null) {
                currentPeersHosts.add(peer.getHost());
                if (!peer.getDisabledAPIs().contains(requestAPI)) {
                    resultPeer = peer;
                }
                disabledAPIs.retainAll(peer.getDisabledAPIs());
            }
        }
        peersHosts = Collections.unmodifiableList(currentPeersHosts);
        LOG.info("Selected API peer " + resultPeer + " peer hosts selected " + currentPeersHosts);
        return resultPeer;
    }

    public Peer setForcedPeer(Peer peer) {
        if (peer != null) {
            forcedPeerHost = peer.getHost();
            mainPeerAnnouncedAddress = peer.getAnnouncedAddress();
            return peer;
        } else {
            forcedPeerHost = null;
            mainPeerAnnouncedAddress = null;
            return getServingPeer(null);
        }
    }

    public String getMainPeerAnnouncedAddress() {
        // The first client request GetBlockchainState is handled by the server
        // Not by the proxy. In order to report a peer to the client we have
        // To select some initial peer.
        if (mainPeerAnnouncedAddress == null) {
            Peer peer = getServingPeer(null);
            if (peer != null) {
                mainPeerAnnouncedAddress = peer.getAnnouncedAddress();
            }
        }
        return mainPeerAnnouncedAddress;
    }

    public static boolean isActivated() {
        return propertiesHolder.isLightClient() || (enableAPIProxy && blockchainProcessor.isDownloading());
    }

    public boolean blacklistHost(String host) {
        if (blacklistedPeers.size() > 1000) {
            LOG.info("Too many blacklisted peers");
            return false;
        }
        blacklistedPeers.put(host, timeService.getEpochTime() + blacklistingPeriod);
        if (peersHosts.contains(host)) {
            peersHosts = Collections.emptyList();
            getServingPeer(null);
        }
        return true;
    }

    private Peer getRandomAPIPeer(List<Peer> peers) {
        if (peers.isEmpty()) {
            return null;
        }
        int index = ThreadLocalRandom.current().nextInt(peers.size());
        return peers.remove(index);
    }
}
