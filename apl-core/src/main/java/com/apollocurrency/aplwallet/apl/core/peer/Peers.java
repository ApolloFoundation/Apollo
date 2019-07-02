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
package com.apollocurrency.aplwallet.apl.core.peer;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import com.apollocurrency.aplwallet.api.p2p.PeerInfo;
import com.apollocurrency.aplwallet.apl.core.account.AccountEventType;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.observer.events.AccountEvent;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountServiceImpl;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.http.API;
import com.apollocurrency.aplwallet.apl.core.http.APIEnum;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.Filter;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.Listeners;
import com.apollocurrency.aplwallet.apl.util.QueuedThreadPool;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.ThreadFactoryImpl;
import com.apollocurrency.aplwallet.apl.util.ThreadPool;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Peers {

    private static final Logger LOG = LoggerFactory.getLogger(Peers.class);

    public enum Event {
        BLACKLIST, UNBLACKLIST, DEACTIVATE, REMOVE,
        DOWNLOADED_VOLUME, UPLOADED_VOLUME, WEIGHT,
        ADDED_ACTIVE_PEER, CHANGED_ACTIVE_PEER,
        NEW_PEER, ADD_INBOUND, REMOVE_INBOUND, CHANGED_SERVICES
    }

    private static final Version MAX_VERSION = Constants.VERSION;

    static final int LOGGING_MASK_EXCEPTIONS = 1;
    static final int LOGGING_MASK_NON200_RESPONSES = 2;
    static final int LOGGING_MASK_200_RESPONSES = 4;
    static volatile int communicationLoggingMask;

    static List<String> wellKnownPeers;
    static Set<String> knownBlacklistedPeers;

    static int connectTimeout;
    static int readTimeout;
    static int blacklistingPeriod;
    public static boolean getMorePeers;
    public static int MAX_REQUEST_SIZE;
    public static int MAX_RESPONSE_SIZE;
    public static int MAX_MESSAGE_SIZE;

    public static final int MIN_COMPRESS_SIZE = 256;
    static boolean useWebSockets;
    static int webSocketIdleTimeout;
    static boolean useProxy;
    static boolean isGzipEnabled;
    public static boolean useTLS;

    private static String myHallmark;

    private static int maxNumberOfInboundConnections;
    private static int maxNumberOfOutboundConnections;
    public static int maxNumberOfConnectedPublicPeers;
    private static int maxNumberOfKnownPeers;
    static int minNumberOfKnownPeers;
    static boolean enableHallmarkProtection;
    private static int pushThreshold;
    private static int pullThreshold;
    private static int sendToPeersLimit;
    static boolean usePeersDb;
    static boolean savePeers;
    public static boolean ignorePeerAnnouncedAddress;
    static boolean cjdnsOnly;
    static final int MAX_APPLICATION_LENGTH = 20;

    static final int MAX_ANNOUNCED_ADDRESS_LENGTH = 200;
    public static boolean hideErrorDetails;

    private static final int sendTransactionsBatchSize = 10;

    private static JSONObject myPeerInfo;
    public static PeerInfo myPI;
    private static List<Peer.Service> myServices;
    private static volatile BlockchainState currentBlockchainState;
    private static volatile JSONStreamAware myPeerInfoRequest;
    private static volatile JSONStreamAware myPeerInfoResponse;

    static boolean shutdown = false;
    static boolean suspend = false;

    private static final Listeners<Peer, Event> listeners = new Listeners<>();

    // used by threads so shoudl be ConcurrentMap
    static final ConcurrentMap<String, PeerImpl> peers = new ConcurrentHashMap<>();

    public static final ExecutorService peersExecutorService = new QueuedThreadPool(2, 15, "PeersService");

    private static final ExecutorService sendingService = Executors.newFixedThreadPool(10, new ThreadFactoryImpl("PeersSendingService"));

    // TODO: YL remove static instance later
    private static PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
    static BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
    private static Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();
    private static BlockchainProcessor blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
    private static volatile EpochTime timeService = CDI.current().select(EpochTime.class).get();
    private static AccountService accountService = CDI.current().select(AccountServiceImpl.class).get();
    private static PeerHttpServer peerHttpServer = CDI.current().select(PeerHttpServer.class).get();
    public static int myPort;

    private Peers() {
    } // never

    public static void init() {
        MAX_REQUEST_SIZE = propertiesHolder.getIntProperty("apl.maxPeerRequestSize", 4096 * 1024);
        MAX_RESPONSE_SIZE = propertiesHolder.getIntProperty("apl.maxPeerResponseSize", 4096 * 1024);
        MAX_MESSAGE_SIZE = propertiesHolder.getIntProperty("apl.maxPeerMessageSize", 15 * 1024 * 1024);
        useProxy = System.getProperty("socksProxyHost") != null || System.getProperty("http.proxyHost") != null;
        hideErrorDetails = propertiesHolder.getBooleanProperty("apl.hideErrorDetails", true);
        useTLS = propertiesHolder.getBooleanProperty("apl.userPeersTLS", true);
        String myHost = null;
        if (peerHttpServer.getMyExtAddress() != null) {
                PeerAddress pa = peerHttpServer.getMyExtAddress();
                myHost = pa.getHost();
                myPort = pa.getPort();
        }
        myHallmark = Convert.emptyToNull(propertiesHolder.getStringProperty("apl.myHallmark", "").trim());
        if (myHallmark != null && Peers.myHallmark.length() > 0) {
            try {
                Hallmark hallmark = Hallmark.parseHallmark(myHallmark);
                if (!hallmark.isValid()) {
                    throw new RuntimeException("Hallmark is not valid");
                }
                if (peerHttpServer.getMyExtAddress() != null) {
                    if (!hallmark.getHost().equals(myHost)) {
                        throw new RuntimeException("Invalid hallmark host");
                    }
                    if (myPort != hallmark.getPort()) {
                        throw new RuntimeException("Invalid hallmark port");
                    }
                }
            } catch (RuntimeException e) {
                LOG.error("Your hallmark is invalid: " + myHallmark + " for your address: " + peerHttpServer.getMyExtAddress());
                throw new RuntimeException(e.toString(), e);
            }
        }

        final List<String> defaultPeers = blockchainConfig.getChain().getDefaultPeers();
        wellKnownPeers = blockchainConfig.getChain().getWellKnownPeers();

        List<String> knownBlacklistedPeersList = blockchainConfig.getChain().getBlacklistedPeers();
        if (knownBlacklistedPeersList.isEmpty()) {
            knownBlacklistedPeers = Collections.emptySet();
        } else {
            knownBlacklistedPeers = Collections.unmodifiableSet(new HashSet<>(knownBlacklistedPeersList));
        }

        maxNumberOfInboundConnections = propertiesHolder.getIntProperty("apl.maxNumberOfInboundConnections");
        maxNumberOfOutboundConnections = propertiesHolder.getIntProperty("apl.maxNumberOfOutboundConnections");
        maxNumberOfConnectedPublicPeers = Math.min(propertiesHolder.getIntProperty("apl.maxNumberOfConnectedPublicPeers"),
                maxNumberOfOutboundConnections);
        maxNumberOfKnownPeers = propertiesHolder.getIntProperty("apl.maxNumberOfKnownPeers");
        minNumberOfKnownPeers = propertiesHolder.getIntProperty("apl.minNumberOfKnownPeers");
        connectTimeout = propertiesHolder.getIntProperty("apl.connectTimeout");
        readTimeout = propertiesHolder.getIntProperty("apl.readTimeout");
        enableHallmarkProtection = propertiesHolder.getBooleanProperty("apl.enableHallmarkProtection") && !propertiesHolder.isLightClient();
        pushThreshold = propertiesHolder.getIntProperty("apl.pushThreshold");
        pullThreshold = propertiesHolder.getIntProperty("apl.pullThreshold");
        useWebSockets = propertiesHolder.getBooleanProperty("apl.useWebSockets");
        webSocketIdleTimeout = propertiesHolder.getIntProperty("apl.webSocketIdleTimeout");
        isGzipEnabled = propertiesHolder.getBooleanProperty("apl.enablePeerServerGZIPFilter");
        blacklistingPeriod = propertiesHolder.getIntProperty("apl.blacklistingPeriod") / 1000;
        communicationLoggingMask = propertiesHolder.getIntProperty("apl.communicationLoggingMask");
        sendToPeersLimit = propertiesHolder.getIntProperty("apl.sendToPeersLimit");
        usePeersDb = propertiesHolder.getBooleanProperty("apl.usePeersDb") && !propertiesHolder.isOffline();
        savePeers = usePeersDb && propertiesHolder.getBooleanProperty("apl.savePeers");
        getMorePeers = propertiesHolder.getBooleanProperty("apl.getMorePeers");
        cjdnsOnly = propertiesHolder.getBooleanProperty("apl.cjdnsOnly");
        ignorePeerAnnouncedAddress = propertiesHolder.getBooleanProperty("apl.ignorePeerAnnouncedAddress");

        if (useWebSockets && useProxy) {
            LOG.info("Using a proxy, will not create outbound websockets.");
        }

        fillMyPeerInfo();

        final List<Future<String>> unresolvedPeers = Collections.synchronizedList(new ArrayList<>());

        if (!propertiesHolder.isOffline()) {
            ThreadPool.runBeforeStart("PeerLoader", new PeerLoaderThread(defaultPeers, unresolvedPeers, timeService), false);
        }

        ThreadPool.runAfterStart("UnresolvedPeersAnalyzer", new UnresolvedPeersAnalyzer(unresolvedPeers));

        addListener(peer -> peersExecutorService.submit(() -> {
            if (peer.getAnnouncedAddress() != null && !peer.isBlacklisted()) {
                try {
                    PeerDb.updatePeer((PeerImpl) peer);
                } catch (RuntimeException e) {
                    LOG.error("Unable to update peer database", e);
                }
            }
        }), Peers.Event.CHANGED_SERVICES);

        // moved to Weld Event
        /*AccountService.addListener(account -> peers.values().forEach(peer -> {
            if (peer.getHallmark() != null && peer.getHallmark().getAccountId() == account.getId()) {
                listeners.notify(peer, Event.WEIGHT);
            }
        }), AccountEventType.BALANCE);*/

        if (!propertiesHolder.isOffline()) {
            ThreadPool.scheduleThread("PeerConnecting", new PeerConnectingThread(timeService), 20);
            ThreadPool.scheduleThread("PeerUnBlacklisting", new PeerUnBlacklistingThread(timeService), 60);
            if (getMorePeers) {
                ThreadPool.scheduleThread("GetMorePeers", new GetMorePeersThread(timeService), 20);
            }
        }
        peerHttpServer.start();
    }

    @Singleton
    static class AccountEventHandler {
        public void onAccountBalance(@Observes @AccountEvent(AccountEventType.BALANCE) Account account){
            peers.values().forEach(peer -> {
                if (peer.getHallmark() != null && peer.getHallmark().getAccountId() == account.getId()) {
                    listeners.notify(peer, Event.WEIGHT);
                }
            });
        }
    }

    private static void fillMyPeerInfo() {
        myPeerInfo = new JSONObject();
        PeerInfo pi = new PeerInfo();
        LOG.debug("Start filling 'MyPeerInfo'...");
        List<Peer.Service> servicesList = new ArrayList<>();
        PeerAddress myExtAddress = peerHttpServer.getMyExtAddress();

        if ( myExtAddress != null) {
            String host = myExtAddress.getHost();
            int port = myExtAddress.getPort();
            String  announcedAddress = myExtAddress.getAddrWithPort();
            LOG.debug("Peer external address  = {} : {} + {}", host, port, announcedAddress);
            if (announcedAddress.length() > MAX_ANNOUNCED_ADDRESS_LENGTH) {
                 throw new RuntimeException("Invalid announced address length: " + announcedAddress);
            }
            pi.setAnnouncedAddress(announcedAddress);
        }else{
            LOG.debug("Peer external address is NOT SET");
        }

        if (myHallmark != null && myHallmark.length() > 0) {
            pi.setHallmark(myHallmark);
            servicesList.add(Peer.Service.HALLMARK);
        }
        pi.setApplication(Constants.APPLICATION);
        pi.setVersion(Constants.VERSION.toString());
        pi.setPlatform(peerHttpServer.getMyPlatform());
        pi.setChainId(blockchainConfig.getChain().getChainId().toString());
        pi.setShareAddress(peerHttpServer.shareMyAddress);
        if (!blockchainConfig.isEnablePruning() && propertiesHolder.INCLUDE_EXPIRED_PRUNABLE()) {
            servicesList.add(Peer.Service.PRUNABLE);
        }
        if (API.openAPIPort > 0) {
            pi.setApiPort(API.openAPIPort);
            servicesList.add(Peer.Service.API);
        }
        if (API.openAPISSLPort > 0) {
            pi.setApiSSLPort(API.openAPISSLPort);
            servicesList.add(Peer.Service.API_SSL);
        }

        if (API.isOpenAPI) {
            EnumSet<APIEnum> disabledAPISet = EnumSet.noneOf(APIEnum.class);

            API.disabledAPIs.forEach(apiName -> {
                APIEnum api = APIEnum.fromName(apiName);
                if (api != null) {
                    disabledAPISet.add(api);
                }
            });
            API.disabledAPITags.forEach(apiTag -> {
                for (APIEnum api : APIEnum.values()) {
                    if (api.getHandler() != null && api.getHandler().getAPITags().contains(apiTag)) {
                        disabledAPISet.add(api);
                    }
                }
            });
            pi.setDisabledAPIs(APIEnum.enumSetToBase64String(disabledAPISet));

            pi.setApiServerIdleTimeout(API.apiServerIdleTimeout);

            if (API.apiServerCORS) {
                servicesList.add(Peer.Service.CORS);
            }
        }

        long services = 0;
        for (Peer.Service service : servicesList) {
            services |= service.getCode();
        }
        pi.setServices(Long.toUnsignedString(services));
        myServices = Collections.unmodifiableList(servicesList);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonOrgModule());
        myPeerInfo = mapper.convertValue(pi, JSONObject.class);
        LOG.debug("My peer info:\n" + myPeerInfo.toJSONString());
        myPI = pi;
        LOG.debug("Finished filling 'MyPeerInfo'");
    }

    public static void shutdown() {
        try {
            shutdown = true;
            peerHttpServer.shutdown();
            ThreadPool.shutdownExecutor("sendingService", sendingService, 2);
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
        try {
            ThreadPool.shutdownExecutor("peersService", peersExecutorService, 5);
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    public static void suspend() {
        suspend = peerHttpServer.suspend();
    }

    public static void resume() {
        if (suspend) {
            suspend = !peerHttpServer.resume();
        }
    }

    public static boolean addListener(Listener<Peer> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Peer> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static void notifyListeners(Peer peer, Event eventType) {
        listeners.notify(peer, eventType);
    }

    public static Collection<Peer> getAllPeers() {
        Collection<Peer> res =  Collections.unmodifiableCollection(peers.values());
        return res;
    }

    public static List<Peer> getActivePeers() {
        return getPeers(peer -> peer.getState() != PeerState.NON_CONNECTED);
    }

    public static List<Peer> getPeers(final PeerState state) {
        return getPeers(peer -> peer.getState() == state);
    }

    public static List<Peer> getPeers(Filter<Peer> filter) {
        return getPeers(filter, Integer.MAX_VALUE);
    }

    public static List<Peer> getPeers(Filter<Peer> filter, int limit) {
        List<Peer> result = new ArrayList<>();
        for (Peer peer : peers.values()) {
            if (filter.test(peer)) {
                result.add(peer);
                if (result.size() >= limit) {
                    break;
                }
            }
        }
        return result;
    }

    public static Peer getPeer(String host) {
        PeerAddress pa = new PeerAddress( host);
        return peers.get(pa.getAddrWithPort());
    }

    public static List<Peer> getInboundPeers() {
        return getPeers(Peer::isInbound);
    }

    public static boolean hasTooManyInboundPeers() {
        return getPeers(Peer::isInbound, maxNumberOfInboundConnections).size() >= maxNumberOfInboundConnections;
    }

    public static boolean hasTooManyOutboundConnections() {
        return getPeers(peer -> !peer.isBlacklisted() && peer.getState() == PeerState.CONNECTED && peer.getAnnouncedAddress() != null,
                maxNumberOfOutboundConnections).size() >= maxNumberOfOutboundConnections;
    }

    public static PeerImpl findOrCreatePeer(String announcedAddress, boolean create) {
        if (announcedAddress == null || announcedAddress.isEmpty()) {
            return null;
        }
        announcedAddress = announcedAddress.trim().toLowerCase();
        PeerAddress pAnnouncedAddress = new PeerAddress(announcedAddress);
        PeerImpl peer;
        if ((peer = peers.get(pAnnouncedAddress.getAddrWithPort())) != null) {
            LOG.trace("Return peer from peers list = {}", peer);
            return peer;
        }
        try {

            String host = pAnnouncedAddress.getAddrWithPort();
            if (host == null) {
                return null;
            }
            if ((peer = peers.get(host)) != null) {
                LOG.trace("Return 2 = {}", peer);
                return peer;
            }

            InetAddress inetAddress = InetAddress.getByName(pAnnouncedAddress.getHost());
            return findOrCreatePeer(inetAddress, announcedAddress, create);
        } catch (UnknownHostException e) {
            //LOG.debug("Invalid peer address: " + announcedAddress + ", " + e.toString());
            return null;
        }
    }

    public static PeerImpl findOrCreatePeer(String host) {
        try {
            InetAddress inetAddress = InetAddress.getByName(host);
            return findOrCreatePeer(inetAddress, null, true);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public static boolean isMyAddress(PeerAddress pa) {
        if(pa==null){
            return true;
        }
        //TODO: many ports: http, https, ssl
        if ((pa.isLocal() && myPort == pa.getPort())) {
            return true;
        }
        String ca = propertiesHolder.getStringProperty("apl.myAddress", "");
        if (!ca.isEmpty()) {
            PeerAddress myConfiguredAddr = new PeerAddress( ca);
            if (myConfiguredAddr.compareTo(pa) == 0) {
                return true;
            }
        }
        PeerAddress myExtAddr = peerHttpServer.getMyExtAddress();
        if (pa.compareTo(myExtAddr) == 0) {
            return true;
        }
        return false;
    }

    public static PeerImpl findOrCreatePeer(final InetAddress inetAddress, final String announcedAddress, final boolean create) {

        String host = inetAddress.getHostAddress();
        if (cjdnsOnly && !host.substring(0, 2).equals("fc")) {
            return null;
        }
        //re-add the [] to ipv6 addresses lost in getHostAddress() above
        if (host.split(":").length > 2) {
            host = "[" + host + "]";
        }

        PeerAddress pa = new PeerAddress(host);

        if (isMyAddress(pa)) {
            return null;
        }

        PeerImpl peer;
        if ((peer = peers.get(pa.getAddrWithPort())) != null) {
            LOG.trace("Returning existing peer from map {}", peer);
            return peer;
        }
        if (!create) {
            return null;
        }

        if (announcedAddress != null && announcedAddress.length() > MAX_ANNOUNCED_ADDRESS_LENGTH) {
            return null;
        }
        peer = new PeerImpl(host, announcedAddress, blockchainConfig, blockchain, timeService, propertiesHolder, accountService);
        return peer;
    }

    public static void setAnnouncedAddress(PeerImpl peer, String newAnnouncedAddress) {
        if (StringUtils.isBlank(newAnnouncedAddress)) {
            LOG.debug("newAnnouncedAddress is empty for host: {}, ignoring", peer.getHostWithPort());
        }
        PeerAddress newPa = new PeerAddress(newAnnouncedAddress);
        Peer oldPeer = peers.get(peer.getHostWithPort());
        if (oldPeer != null) {
            PeerAddress oldPa = new PeerAddress(oldPeer.getAnnouncedAddress());
            if (newPa.compareTo(oldPa) != 0) {
                LOG.debug("Removing old announced address " + oldPa + " for peer " + oldPeer.getHost() + ":" + oldPeer.getPort());
//                selfAnnouncedAddresses.remove(newPa.getAddrWithPort());
                try {
                    peer.setAnnouncedAddress(newAnnouncedAddress);
                    oldPeer = peers.remove(oldPeer);
                    if (oldPeer != null) {
                       notifyListeners(oldPeer, Event.REMOVE);
                    }
                } catch (MalformedURLException | UnknownHostException ex) {
                    LOG.warn("Wrong announces address: " + newAnnouncedAddress, ex);
                }
            }
        }
    }

    public static boolean addPeer(Peer peer, String newAnnouncedAddress) {
        setAnnouncedAddress((PeerImpl) peer, newAnnouncedAddress.toLowerCase());
        return addPeer(peer);
    }

    public static boolean addPeer(Peer peer) {
        if (peer != null && peer.getHost() != null && !peer.getHost().isEmpty()) {
            // put new or replace previous
            if (!peers.containsKey(peer.getHostWithPort())) {
                peers.put(peer.getHostWithPort(), (PeerImpl) peer);
            } else {
                peers.replace(peer.getHostWithPort(), (PeerImpl) peer);
            }
            listeners.notify(peer, Event.NEW_PEER);
            return true;
        }
        return false;
    }

    public static PeerImpl removePeer(Peer peer) {
        PeerDb.Entry entry = new PeerDb.Entry(peer.getHostWithPort(), 0, 0);
        PeerDb.deletePeer(entry);
        return peers.remove(peer.getHostWithPort());
    }

    public static void connectPeer(Peer peer) {
        peer.unBlacklist();
        peer.handshake(blockchainConfig.getChain().getChainId());
    }

    public static void sendToSomePeers(Block block) {
        JSONObject request = block.getJSONObject();
        request.put("requestType", "processBlock");
        sendToSomePeers(request);
    }

    public static void sendToSomePeers(List<? extends Transaction> transactions) {
        int nextBatchStart = 0;
        while (nextBatchStart < transactions.size()) {
            JSONObject request = new JSONObject();
            JSONArray transactionsData = new JSONArray();
            for (int i = nextBatchStart; i < nextBatchStart + sendTransactionsBatchSize && i < transactions.size(); i++) {
                transactionsData.add(transactions.get(i).getJSONObject());
            }
            request.put("requestType", "processTransactions");
            request.put("transactions", transactionsData);
            sendToSomePeers(request);
            nextBatchStart += sendTransactionsBatchSize;
        }
    }

    private static void sendToSomePeers(final JSONObject request) {
        if (shutdown || suspend) {
            String errorMessage = String.format("Cannot send request to peers. Peer server was %s", suspend ? "suspended" : "shutdown");
            LOG.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
        sendingService.submit(() -> {
            request.put("chainId", blockchainConfig.getChain().getChainId());
            final JSONStreamAware jsonRequest = JSON.prepareRequest(request);

            int successful = 0;
            List<Future<JSONObject>> expectedResponses = new ArrayList<>();
            for (final Peer peer : peers.values()) {

                if (enableHallmarkProtection && peer.getWeight() < pushThreshold) {
                    continue;
                }

                if (!peer.isBlacklisted() && peer.getState() == PeerState.CONNECTED && peer.getAnnouncedAddress() != null
                        && peer.getBlockchainState() != BlockchainState.LIGHT_CLIENT) {
                    Future<JSONObject> futureResponse = peersExecutorService.submit(() -> peer.send(jsonRequest,
                            blockchainConfig.getChain().getChainId()));
                    expectedResponses.add(futureResponse);
                }
                if (expectedResponses.size() >= sendToPeersLimit - successful) {
                    for (Future<JSONObject> future : expectedResponses) {
                        try {
                            JSONObject response = future.get();
                            if (response != null && response.get("error") == null) {
                                successful += 1;
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } catch (ExecutionException e) {
                            LOG.debug("Error in sendToSomePeers", e);
                        }

                    }
                    expectedResponses.clear();
                }
                if (successful >= sendToPeersLimit) {
                    return;
                }
            }
        });
    }

    public static Peer getAnyPeer(final PeerState state, final boolean applyPullThreshold) {
        return getWeightedPeer(getPublicPeers(state, applyPullThreshold));
    }

    public static List<Peer> getPublicPeers(final PeerState state, final boolean applyPullThreshold) {
        UUID chainId = blockchainConfig.getChain().getChainId();
        return getPeers(peer -> !peer.isBlacklisted() && peer.getState() == state && chainId.equals(peer.getChainId()) && peer.getAnnouncedAddress() != null
                && (!applyPullThreshold || !enableHallmarkProtection || peer.getWeight() >= pullThreshold));
    }

    public static Peer getWeightedPeer(List<Peer> selectedPeers) {
        if (selectedPeers.isEmpty()) {
            return null;
        }
        if (!enableHallmarkProtection || ThreadLocalRandom.current().nextInt(3) == 0) {
            return selectedPeers.get(ThreadLocalRandom.current().nextInt(selectedPeers.size()));
        }
        long totalWeight = 0;
        for (Peer peer : selectedPeers) {
            long weight = peer.getWeight();
            if (weight == 0) {
                weight = 1;
            }
            totalWeight += weight;
        }
        long hit = ThreadLocalRandom.current().nextLong(totalWeight);
        for (Peer peer : selectedPeers) {
            long weight = peer.getWeight();
            if (weight == 0) {
                weight = 1;
            }
            if ((hit -= weight) < 0) {
                return peer;
            }
        }
        return null;
    }

    public static boolean hasTooFewKnownPeers() {
        return peers.size() < minNumberOfKnownPeers;
    }

    public static boolean hasTooManyKnownPeers() {
        return peers.size() > maxNumberOfKnownPeers;
    }

    static boolean hasEnoughConnectedPublicPeers(int limit) {
        return getPeers(peer -> !peer.isBlacklisted() && peer.getState() == PeerState.CONNECTED && peer.getAnnouncedAddress() != null
                && (!enableHallmarkProtection || peer.getWeight() > 0), limit).size() >= limit;
    }

    /**
     * Set the communication logging mask
     *
     * @param events Communication event list or null to reset communications
     * logging
     * @return TRUE if the communication logging mask was updated
     */
    public static boolean setCommunicationLoggingMask(String[] events) {
        boolean updated = true;
        int mask = 0;
        if (events != null) {
            for (String event : events) {
                switch (event) {
                    case "EXCEPTION":
                        mask |= LOGGING_MASK_EXCEPTIONS;
                        break;
                    case "HTTP-ERROR":
                        mask |= LOGGING_MASK_NON200_RESPONSES;
                        break;
                    case "HTTP-OK":
                        mask |= LOGGING_MASK_200_RESPONSES;
                        break;
                    default:
                        updated = false;
                }
                if (!updated) {
                    break;
                }
            }
        }
        if (updated) {
            communicationLoggingMask = mask;
        }
        return updated;
    }

    /**
     * Return local peer services
     *
     * @return List of local peer services
     */
    public static List<Peer.Service> getServices() {
        return myServices;
    }

    private static void checkBlockchainState() {
        BlockchainState state = propertiesHolder.isLightClient()
                ? BlockchainState.LIGHT_CLIENT
                : (blockchainProcessor.isDownloading() || blockchain.getLastBlockTimestamp() < timeService.getEpochTime() - 600)
                ? BlockchainState.DOWNLOADING
                : (blockchain.getLastBlock().getBaseTarget() / blockchainConfig.getCurrentConfig().getInitialBaseTarget() > 10)
                ? BlockchainState.FORK
                : BlockchainState.UP_TO_DATE;
        if (state != currentBlockchainState) {
            JSONObject json = new JSONObject(myPeerInfo);
            json.put("blockchainState", state.ordinal());
            myPeerInfoResponse = JSON.prepare(json);
            json.put("requestType", "getInfo");
            myPeerInfoRequest = JSON.prepareRequest(json);
            currentBlockchainState = state;
            LOG.trace("currentBlockchainState = {}", currentBlockchainState);
        }
    }

    public static JSONStreamAware getMyPeerInfoRequest() {
        checkBlockchainState();
        return myPeerInfoRequest;
    }

    public static JSONStreamAware getMyPeerInfoResponse() {
        checkBlockchainState();
        return myPeerInfoResponse;
    }

    public static BlockchainState getMyBlockchainState() {
        checkBlockchainState();
        return currentBlockchainState;
    }

}
