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

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.http.API;
import com.apollocurrency.aplwallet.apl.core.http.APIEnum;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.Filter;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.Listeners;
import com.apollocurrency.aplwallet.apl.util.QueuedThreadPool;
import com.apollocurrency.aplwallet.apl.util.ThreadFactoryImpl;
import com.apollocurrency.aplwallet.apl.util.ThreadPool;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import javax.enterprise.inject.spi.CDI;

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

    private static List<String> wellKnownPeers;
    static Set<String> knownBlacklistedPeers;

    // TODO: YL remove static instance later
    private static PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();    
    private static BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
    private static Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();
    private static BlockchainProcessor blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
    private static volatile EpochTime timeService = CDI.current().select(EpochTime.class).get();
    private static DatabaseManager databaseManager;
    private static PeerDb peerDb;

    static int connectTimeout;
    static int readTimeout;
    static int blacklistingPeriod;
    static boolean getMorePeers;
    static final int MAX_REQUEST_SIZE = propertiesHolder.getIntProperty("apl.maxPeerRequestSize", 1024 * 1024);
    static final int MAX_RESPONSE_SIZE = propertiesHolder.getIntProperty("apl.maxPeerResponseSize", 1024 * 1024);
    static final int MAX_MESSAGE_SIZE = propertiesHolder.getIntProperty("apl.maxPeerMessageSize", 10 * 1024 * 1024);
    public static final int MIN_COMPRESS_SIZE = 256;
    static boolean useWebSockets;
    static int webSocketIdleTimeout;
    static final boolean useProxy = System.getProperty("socksProxyHost") != null || System.getProperty("http.proxyHost") != null;
    static boolean isGzipEnabled;


    private static String myHallmark;
   
    
    private static int maxNumberOfInboundConnections;
    private static int maxNumberOfOutboundConnections;
    public static int maxNumberOfConnectedPublicPeers;
    private static int maxNumberOfKnownPeers;
    private static int minNumberOfKnownPeers;
    private static boolean enableHallmarkProtection;
    private static int pushThreshold;
    private static int pullThreshold;
    private static int sendToPeersLimit;
    private static boolean usePeersDb;
    private static boolean savePeers;
    static boolean ignorePeerAnnouncedAddress;
    static boolean cjdnsOnly;
    static final int MAX_APPLICATION_LENGTH = 20;

    static final int MAX_ANNOUNCED_ADDRESS_LENGTH = 100;
    static final boolean hideErrorDetails = propertiesHolder.getBooleanProperty("apl.hideErrorDetails");

    private static JSONObject myPeerInfo;
    private static List<Peer.Service> myServices;
    private static volatile Peer.BlockchainState currentBlockchainState;
    private static volatile JSONStreamAware myPeerInfoRequest;
    private static volatile JSONStreamAware myPeerInfoResponse;
    static boolean shutdown=false;
    static boolean suspend;

    private static final Listeners<Peer,Event> listeners = new Listeners<>();
    // used by threads
    static final ConcurrentMap<String, PeerImpl> peers = new ConcurrentHashMap<>();
    
    private static final ConcurrentMap<String, String> selfAnnouncedAddresses = new ConcurrentHashMap<>();

    static final Collection<PeerImpl> allPeers = Collections.unmodifiableCollection(peers.values());

    static final ExecutorService peersService = new QueuedThreadPool(2, 15, "PeersService");
    
    private static final ExecutorService sendingService = Executors.newFixedThreadPool(10, new ThreadFactoryImpl("PeersSendingService"));

    private  static PeerHttpServer peerHttpServer = CDI.current().select(PeerHttpServer.class).get();  
    
    private Peers() {} // never
 
    private static TransactionalDataSource lookupDataSource() {
        if (databaseManager == null) {
            databaseManager = CDI.current().select(DatabaseManager.class).get();
        }
        return databaseManager.getDataSource();
    }

    public static void init() {
        String myHost = null;
        int myPort = -1;
        if (peerHttpServer.myAddress != null) {
            try {
                URI uri = new URI("http://" + peerHttpServer.myAddress);
                myHost = uri.getHost();
                myPort = (uri.getPort() == -1 ? Peers.getDefaultPeerPort() : uri.getPort());
                InetAddress[] myAddrs = InetAddress.getAllByName(myHost);
                boolean addrValid = false;
                Enumeration<NetworkInterface> intfs = NetworkInterface.getNetworkInterfaces();
                chkAddr: while (intfs.hasMoreElements()) {
                    NetworkInterface intf = intfs.nextElement();
                    List<InterfaceAddress> intfAddrs = intf.getInterfaceAddresses();
                    for (InterfaceAddress intfAddr: intfAddrs) {
                        InetAddress extAddr = intfAddr.getAddress();
                        for (InetAddress myAddr : myAddrs) {
                            if (extAddr.equals(myAddr)) {
                                addrValid = true;
                                break chkAddr;
                            }
                        }
                    }
                }
                if (!addrValid) {
                    InetAddress extAddr = peerHttpServer.upnp.getExternalAddress();
                    if (extAddr != null) {
                        for (InetAddress myAddr : myAddrs) {
                            if (extAddr.equals(myAddr)) {
                                addrValid = true;
                                break;
                            }
                        }
                    }
                }
                if (!addrValid) {
                    LOG.warn("Your announced address does not match your external address");
                }
            } catch (SocketException e) {
                LOG.error("Unable to enumerate the network interfaces :" + e.toString());
            } catch (URISyntaxException | UnknownHostException e) {
                LOG.warn("Your announced address is not valid: " + e.toString());
            }
        }

        myHallmark = Convert.emptyToNull(propertiesHolder.getStringProperty("apl.myHallmark", "").trim());
        if (Peers.myHallmark != null && Peers.myHallmark.length() > 0) {
            try {
                Hallmark hallmark = Hallmark.parseHallmark(Peers.myHallmark);
                if (!hallmark.isValid()) {
                    throw new RuntimeException("Hallmark is not valid");
                }
                if (peerHttpServer.myAddress != null) {
                    if (!hallmark.getHost().equals(myHost)) {
                        throw new RuntimeException("Invalid hallmark host");
                    }
                    if (myPort != hallmark.getPort()) {
                        throw new RuntimeException("Invalid hallmark port");
                    }
                }
            } catch (RuntimeException e) {
                LOG.error("Your hallmark is invalid: " + Peers.myHallmark + " for your address: " + peerHttpServer.myAddress);
                throw new RuntimeException(e.toString(), e);
            }
        }
        List<Peer.Service> servicesList = new ArrayList<>();
        JSONObject json = new JSONObject();
        if (peerHttpServer.myAddress != null) {
            try {
                URI uri = new URI("http://" + peerHttpServer.myAddress);
                String host = uri.getHost();
                int port = uri.getPort();
                String announcedAddress;
                if (port >= 0) {
                    announcedAddress = peerHttpServer.myAddress;
                } else {
                    announcedAddress = host + (peerHttpServer.myPeerServerPort != Constants.DEFAULT_PEER_PORT ? ":" + peerHttpServer.myPeerServerPort : "");
                }
                if (announcedAddress.length() > MAX_ANNOUNCED_ADDRESS_LENGTH) {
                    throw new RuntimeException("Invalid announced address length: " + announcedAddress);
                }
                json.put("announcedAddress", announcedAddress);
            } catch (URISyntaxException e) {
                LOG.info("Your announce address is invalid: " + peerHttpServer.myAddress);
                throw new RuntimeException(e.toString(), e);
            }
        }
        if (Peers.myHallmark != null && Peers.myHallmark.length() > 0) {
            json.put("hallmark", Peers.myHallmark);
            servicesList.add(Peer.Service.HALLMARK);
        }
        json.put("application", Constants.APPLICATION);
        json.put("version",Constants.VERSION.toString());
        json.put("platform", peerHttpServer.myPlatform);
        json.put("chainId", blockchainConfig.getChain().getChainId());
        json.put("shareAddress", peerHttpServer.shareMyAddress);
        if (!blockchainConfig.isEnablePruning() && propertiesHolder.INCLUDE_EXPIRED_PRUNABLE()) {
            servicesList.add(Peer.Service.PRUNABLE);
        }
        if (API.openAPIPort > 0) {
            json.put("apiPort", API.openAPIPort);
            servicesList.add(Peer.Service.API);
        }
        if (API.openAPISSLPort > 0) {
            json.put("apiSSLPort", API.openAPISSLPort);
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
            json.put("disabledAPIs", APIEnum.enumSetToBase64String(disabledAPISet));

            json.put("apiServerIdleTimeout", API.apiServerIdleTimeout);

            if (API.apiServerCORS) {
                servicesList.add(Peer.Service.CORS);
            }
        }

        long services = 0;
        for (Peer.Service service : servicesList) {
            services |= service.getCode();
        }
        json.put("services", Long.toUnsignedString(services));
        myServices = Collections.unmodifiableList(servicesList);
        LOG.debug("My peer info:\n" + json.toJSONString());
        myPeerInfo = json;

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
        usePeersDb = propertiesHolder.getBooleanProperty("apl.usePeersDb") && ! propertiesHolder.isOffline();
        savePeers = usePeersDb && propertiesHolder.getBooleanProperty("apl.savePeers");
        getMorePeers = propertiesHolder.getBooleanProperty("apl.getMorePeers");
        cjdnsOnly = propertiesHolder.getBooleanProperty("apl.cjdnsOnly");
        ignorePeerAnnouncedAddress = propertiesHolder.getBooleanProperty("apl.ignorePeerAnnouncedAddress");
        
        if (useWebSockets && useProxy) {
            LOG.info("Using a proxy, will not create outbound websockets.");
        }

        final List<Future<String>> unresolvedPeers = Collections.synchronizedList(new ArrayList<>());

        if (!propertiesHolder.isOffline()) {
            ThreadPool.runBeforeStart("PeerLoader", new PeerLoader(defaultPeers, unresolvedPeers), false);
        }

        ThreadPool.runAfterStart("UnresolvedPeersAnalyzer", new UnresolvedPeersAnalyzer(unresolvedPeers));
  // -- above was static section      

        // get main db data source
        TransactionalDataSource dataSource = lookupDataSource();

        Peers.addListener(peer -> peersService.submit(() -> {
            if (peer.getAnnouncedAddress() != null && !peer.isBlacklisted()) {
                try {
                    dataSource.begin();
                    PeerDb.updatePeer((PeerImpl)peer);
                    dataSource.commit();
                } catch (RuntimeException e) {
                    LOG.error("Unable to update peer database", e);
                    dataSource.rollback();
                }
            }
        }), Peers.Event.CHANGED_SERVICES);

        Account.addListener(account -> peers.values().forEach(peer -> {
            if (peer.getHallmark() != null && peer.getHallmark().getAccountId() == account.getId()) {
                Peers.listeners.notify(peer, Event.WEIGHT);
            }
        }), Account.Event.BALANCE);

        if (! propertiesHolder.isOffline()) {
            ThreadPool.scheduleThread("PeerConnecting", new PeerConnectingThread(), 20);
            ThreadPool.scheduleThread("PeerUnBlacklisting", new PeerUnBlacklistingThread(timeService), 60);
            if (Peers.getMorePeers) {
                ThreadPool.scheduleThread("GetMorePeers", new GetMorePeersThread(), 20);
            }
        }
    }

    public static void shutdown() {
        shutdown = true;
        peerHttpServer.shutdown();
        ThreadPool.shutdownExecutor("sendingService", sendingService, 2);
        ThreadPool.shutdownExecutor("peersService", peersService, 5);
    }

    public static void suspend() {
        suspend =  peerHttpServer.suspend();
    }

    public static void resume() {
        if(suspend) {
         suspend = !peerHttpServer.resume();         
        }
    }

    public static boolean addListener(Listener<Peer> listener, Event eventType) {
        return Peers.listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Peer> listener, Event eventType) {
        return Peers.listeners.removeListener(listener, eventType);
    }

    static void notifyListeners(Peer peer, Event eventType) {
        Peers.listeners.notify(peer, eventType);
    }

    public static int getDefaultPeerPort() {
        return propertiesHolder.getIntProperty("apl.networkPeerServerPort", Constants.DEFAULT_PEER_PORT);
    }

    public static Collection<? extends Peer> getAllPeers() {
        return allPeers;
    }

    public static List<Peer> getActivePeers() {
        return getPeers(peer -> peer.getState() != Peer.State.NON_CONNECTED);
    }

    public static List<Peer> getPeers(final Peer.State state) {
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
        return peers.get(host);
    }

    public static List<Peer> getInboundPeers() {
        return getPeers(Peer::isInbound);
    }

    public static boolean hasTooManyInboundPeers() {
        return getPeers(Peer::isInbound, maxNumberOfInboundConnections).size() >= maxNumberOfInboundConnections;
    }

    public static boolean hasTooManyOutboundConnections() {
        return getPeers(peer -> !peer.isBlacklisted() && peer.getState() == Peer.State.CONNECTED && peer.getAnnouncedAddress() != null,
                maxNumberOfOutboundConnections).size() >= maxNumberOfOutboundConnections;
    }

    public static PeerImpl findOrCreatePeer(String announcedAddress, boolean create) {
        if (announcedAddress == null || announcedAddress.isEmpty()) {
            return null;
        }
        announcedAddress = announcedAddress.trim().toLowerCase();
        PeerImpl peer;
        if ((peer = peers.get(announcedAddress)) != null) {
            LOG.trace("Return 0 = {}", peer);
            return peer;
        }
        String host = selfAnnouncedAddresses.get(announcedAddress);
        if (host != null && (peer = peers.get(host)) != null) {
            LOG.trace("Return 1 = {}", peer);
            return peer;
        }
        try {
            URI uri = new URI("http://" + announcedAddress);
            host = uri.getHost();
            if (host == null) {
                return null;
            }
            if ((peer = peers.get(host)) != null) {
                LOG.trace("Return 2 = {}", peer);
                return peer;
            }
            String host2 = selfAnnouncedAddresses.get(host);
            if (host2 != null && (peer = peers.get(host2)) != null) {
                LOG.trace("Return 3 = {}", peer);
                return peer;
            }
            InetAddress inetAddress = InetAddress.getByName(host);
            return findOrCreatePeer(inetAddress, addressWithPort(announcedAddress), create);
        } catch (URISyntaxException | UnknownHostException e) {
            //LOG.debug("Invalid peer address: " + announcedAddress + ", " + e.toString());
            return null;
        }
    }

    static PeerImpl findOrCreatePeer(String host) {
        try {
            InetAddress inetAddress = InetAddress.getByName(host);
            return findOrCreatePeer(inetAddress, null, true);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    static PeerImpl findOrCreatePeer(final InetAddress inetAddress, final String announcedAddress, final boolean create) {

        if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress() || inetAddress.isLinkLocalAddress()) {
            return null;
        }

        String host = inetAddress.getHostAddress();
        if (Peers.cjdnsOnly && !host.substring(0,2).equals("fc")) {
            return null;
        }
        //re-add the [] to ipv6 addresses lost in getHostAddress() above
        if (host.split(":").length > 2) {
            host = "[" + host + "]";
        }

        PeerImpl peer;
        if ((peer = peers.get(host)) != null) {
            return peer;
        }
        if (!create) {
            return null;
        }

        if (peerHttpServer.myAddress != null && peerHttpServer.myAddress.equalsIgnoreCase(announcedAddress)) {
            return null;
        }
        if (announcedAddress != null && announcedAddress.length() > MAX_ANNOUNCED_ADDRESS_LENGTH) {
            return null;
        }
        peer = new PeerImpl(host, announcedAddress);
        return peer;
    }

    static void setAnnouncedAddress(PeerImpl peer, String newAnnouncedAddress) {
        Peer oldPeer = peers.get(peer.getHost());
        if (oldPeer != null) {
            String oldAnnouncedAddress = oldPeer.getAnnouncedAddress();
            if (oldAnnouncedAddress != null && !oldAnnouncedAddress.isEmpty()
                    && newAnnouncedAddress != null && !newAnnouncedAddress.isEmpty()
                    && !oldAnnouncedAddress.equals(newAnnouncedAddress)) {
                LOG.debug("Removing old announced address " + oldAnnouncedAddress + " for peer " + oldPeer.getHost());
                selfAnnouncedAddresses.remove(oldAnnouncedAddress);
            }
        }
        if (newAnnouncedAddress != null && !newAnnouncedAddress.isEmpty()) {
            String oldHost = selfAnnouncedAddresses.put(newAnnouncedAddress, peer.getHost());
            if (oldHost != null && !peer.getHost().equals(oldHost)) {
                LOG.debug("Announced address " + newAnnouncedAddress + " now maps to peer " + peer.getHost()
                        + ", removing old peer " + oldHost);
                oldPeer = peers.remove(oldHost);
                if (oldPeer != null) {
                    Peers.notifyListeners(oldPeer, Event.REMOVE);
                }
            }
        }
        peer.setAnnouncedAddress(newAnnouncedAddress);
    }

    public static boolean addPeer(Peer peer, String newAnnouncedAddress) {
        setAnnouncedAddress((PeerImpl)peer, newAnnouncedAddress.toLowerCase());
        return addPeer(peer);
    }

    public static boolean addPeer(Peer peer) {
        if (peer != null && peer.getHost() != null && !peer.getHost().isEmpty()) {
            // put new or replace previous
            if (!peers.containsKey(peer.getHost())) {
                peers.put(peer.getHost(), (PeerImpl) peer);
            } else {
                peers.replace(peer.getHost(), (PeerImpl) peer);
            }
            listeners.notify(peer, Event.NEW_PEER);
            return true;
        }
        return false;
    }

    public static PeerImpl removePeer(Peer peer) {
        if (peer.getAnnouncedAddress() != null) {
            selfAnnouncedAddresses.remove(peer.getAnnouncedAddress());
        }
        return peers.remove(peer.getHost());
    }

    public static void connectPeer(Peer peer) {
        peer.unBlacklist();
        ((PeerImpl)peer).connect(blockchainConfig.getChain().getChainId());
    }

    public static void sendToSomePeers(Block block) {
        JSONObject request = block.getJSONObject();
        request.put("requestType", "processBlock");
        sendToSomePeers(request);
    }

    private static final int sendTransactionsBatchSize = 10;

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

                if (Peers.enableHallmarkProtection && peer.getWeight() < Peers.pushThreshold) {
                    continue;
                }

                if (!peer.isBlacklisted() && peer.getState() == Peer.State.CONNECTED && peer.getAnnouncedAddress() != null
                        && peer.getBlockchainState() != Peer.BlockchainState.LIGHT_CLIENT) {
                    Future<JSONObject> futureResponse = peersService.submit(() -> peer.send(jsonRequest,
                            blockchainConfig.getChain().getChainId()));
                    expectedResponses.add(futureResponse);
                }
                if (expectedResponses.size() >= Peers.sendToPeersLimit - successful) {
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
                if (successful >= Peers.sendToPeersLimit) {
                    return;
                }
            }
        });
    }

    public static Peer getAnyPeer(final Peer.State state, final boolean applyPullThreshold) {
        return getWeightedPeer(getPublicPeers(state, applyPullThreshold));
    }

    public static List<Peer> getPublicPeers(final Peer.State state, final boolean applyPullThreshold) {
        UUID chainId = blockchainConfig.getChain().getChainId();
        return getPeers(peer -> !peer.isBlacklisted() && peer.getState() == state && chainId.equals(peer.getChainId()) && peer.getAnnouncedAddress() != null
                && (!applyPullThreshold || !Peers.enableHallmarkProtection || peer.getWeight() >= Peers.pullThreshold));
    }

    public static Peer getWeightedPeer(List<Peer> selectedPeers) {
        if (selectedPeers.isEmpty()) {
            return null;
        }
        if (! Peers.enableHallmarkProtection || ThreadLocalRandom.current().nextInt(3) == 0) {
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

    static String addressWithPort(String address) {
        if (address == null) {
            return null;
        }
        try {
            URI uri = new URI("http://" + address);
            String host = uri.getHost();
            int port = uri.getPort();
            return port > 0 && port != Peers.getDefaultPeerPort() ? host + ":" + port : host;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public static boolean isOldVersion(Version version, Version minVersion) {
        if (version == null) {
            return true;
        }
        return version.lessThan(minVersion);
    }


    public static boolean isNewVersion(Version version) {
        if (version == null) {
            return true;
        }
        return version.greaterThan(MAX_VERSION);
    }

    public static boolean hasTooFewKnownPeers() {
        return peers.size() < Peers.minNumberOfKnownPeers;
    }

    public static boolean hasTooManyKnownPeers() {
        return peers.size() > Peers.maxNumberOfKnownPeers;
    }

    private static boolean hasEnoughConnectedPublicPeers(int limit) {
        return getPeers(peer -> !peer.isBlacklisted() && peer.getState() == Peer.State.CONNECTED && peer.getAnnouncedAddress() != null
                && (! Peers.enableHallmarkProtection || peer.getWeight() > 0), limit).size() >= limit;
    }

    /**
     * Set the communication logging mask
     *
     * @param   events              Communication event list or null to reset communications logging
     * @return                      TRUE if the communication logging mask was updated
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
                if (!updated)
                    break;
            }
        }
        if (updated)
            communicationLoggingMask = mask;
        return updated;
    }

    /**
     * Return local peer services
     *
     * @return                      List of local peer services
     */
    public static List<Peer.Service> getServices() {
        return myServices;
    }

    private static void checkBlockchainState() {
        Peer.BlockchainState state = propertiesHolder.isLightClient()
                ? Peer.BlockchainState.LIGHT_CLIENT
                : (blockchainProcessor.isDownloading() || blockchain.getLastBlockTimestamp() < timeService.getEpochTime() - 600)
                ? Peer.BlockchainState.DOWNLOADING :
                        (blockchain.getLastBlock().getBaseTarget() / blockchainConfig.getCurrentConfig().getInitialBaseTarget() > 10) ?
                                Peer.BlockchainState.FORK :
                        Peer.BlockchainState.UP_TO_DATE;
        if (state != currentBlockchainState) {
            JSONObject json = new JSONObject(myPeerInfo);
            json.put("blockchainState", state.ordinal());
            myPeerInfoResponse = JSON.prepare(json);
            json.put("requestType", "getInfo");
            myPeerInfoRequest = JSON.prepareRequest(json);
            currentBlockchainState = state;
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

    public static Peer.BlockchainState getMyBlockchainState() {
        checkBlockchainState();
        return currentBlockchainState;
    }


    private static class PeerLoader implements Runnable {

        private final List<String> defaultPeers;
        private final List<Future<String>> unresolvedPeers;

        public PeerLoader(List<String> defaultPeers, List<Future<String>> unresolvedPeers) {
            this.defaultPeers = defaultPeers;
            this.unresolvedPeers = unresolvedPeers;
        }
        private final Set<PeerDb.Entry> entries = new HashSet<>();
        private PeerDb peerDb;

        @Override
        public void run() {
            LOG.trace("'Peer loader': thread starting...");
            if (peerDb == null) peerDb = CDI.current().select(PeerDb.class).get();
            final int now = timeService.getEpochTime();
            wellKnownPeers.forEach(address -> entries.add(new PeerDb.Entry(address, 0, now)));
            if (usePeersDb) {
                LOG.debug("'Peer loader': Loading 'well known' peers from the database...");
                defaultPeers.forEach(address -> entries.add(new PeerDb.Entry(address, 0, now)));
                if (savePeers) {
                    List<PeerDb.Entry> dbPeers = peerDb.loadPeers();
                    dbPeers.forEach(entry -> {
                        if (!entries.add(entry)) {
                            // Database entries override entries from chains.json
                            entries.remove(entry);
                            entries.add(entry);
                            LOG.trace("'Peer loader': Peer Loaded from db = {}", entry);
                        }
                    });
                }
            }
            if (entries.size() > 0) {
                LOG.debug("'Peer loader': findOrCreatePeer() 'known peers'...");
            }
            entries.forEach(entry -> {
                Future<String> unresolvedAddress = peersService.submit(() -> {
                    PeerImpl peer = Peers.findOrCreatePeer(entry.getAddress(), true);
                    if (peer != null) {
                        peer.setLastUpdated(entry.getLastUpdated());
                        peer.setServices(entry.getServices());
                        Peers.addPeer(peer);
                        LOG.trace("'Peer loader': Put 'well known' Peer from db into 'Peers Map' = {}", entry);
                        return null;
                    }
                    return entry.getAddress();
                });
                unresolvedPeers.add(unresolvedAddress);
            });
            LOG.trace("'Peer loader': thread finished. Peers [{}] =\n{}", Peers.getAllPeers().size());
            Peers.getAllPeers().stream().forEach(peerHost -> LOG.trace("'Peer loader': dump = {}", peerHost));
        }
    }

    private static class PeerConnectingThread implements Runnable {

        public PeerConnectingThread() {
        }

        @Override
        public void run() {
            if (shutdown || suspend) {
                return;
            }
            try {
                try {
                    
                    final int now = timeService.getEpochTime();
                    if (!hasEnoughConnectedPublicPeers(Peers.maxNumberOfConnectedPublicPeers)) {
                        List<Future<?>> futures = new ArrayList<>();
                        List<Peer> hallmarkedPeers = getPeers(peer -> !peer.isBlacklisted()
                                && peer.getAnnouncedAddress() != null
                                && peer.getState() != Peer.State.CONNECTED
                                && now - peer.getLastConnectAttempt() > 600
                                && peer.providesService(Peer.Service.HALLMARK));
                        List<Peer> nonhallmarkedPeers = getPeers(peer -> !peer.isBlacklisted()
                                && peer.getAnnouncedAddress() != null
                                && peer.getState() != Peer.State.CONNECTED
                                && now - peer.getLastConnectAttempt() > 600
                                && !peer.providesService(Peer.Service.HALLMARK));
                        if (!hallmarkedPeers.isEmpty() || !nonhallmarkedPeers.isEmpty()) {
                            Set<PeerImpl> connectSet = new HashSet<>();
                            for (int i = 0; i < 10; i++) {
                                List<Peer> peerList;
                                if (hallmarkedPeers.isEmpty()) {
                                    peerList = nonhallmarkedPeers;
                                } else if (nonhallmarkedPeers.isEmpty()) {
                                    peerList = hallmarkedPeers;
                                } else {
                                    peerList = (ThreadLocalRandom.current().nextInt(2) == 0 ? hallmarkedPeers : nonhallmarkedPeers);
                                }
                                connectSet.add((PeerImpl)peerList.get(ThreadLocalRandom.current().nextInt(peerList.size())));
                            }
                            connectSet.forEach(peer -> futures.add(peersService.submit(() -> {
                                peer.connect(blockchainConfig.getChain().getChainId());
                                if (peer.getState() == Peer.State.CONNECTED &&
                                        enableHallmarkProtection && peer.getWeight() == 0 &&
                                        hasTooManyOutboundConnections()) {
                                    LOG.debug("Too many outbound connections, deactivating peer " + peer.getHost());
                                    peer.deactivate();
                                }
                                return null;
                            })));
                            for (Future<?> future : futures) {
                                future.get();
                            }
                        }
                    }
                    
                    peers.values().forEach(peer -> {
                        if (peer.getState() == Peer.State.CONNECTED
                                && now - peer.getLastUpdated() > 3600
                                && now - peer.getLastConnectAttempt() > 600) {
                            peersService.submit(()-> peer.connect(blockchainConfig.getChain().getChainId()));
                        }
                        if (peer.getLastInboundRequest() != 0 &&
                                now - peer.getLastInboundRequest() > Peers.webSocketIdleTimeout / 1000) {
                            peer.setLastInboundRequest(0);
                            notifyListeners(peer, Event.REMOVE_INBOUND);
                        }
                    });
                    
                    if (hasTooManyKnownPeers() && hasEnoughConnectedPublicPeers(Peers.maxNumberOfConnectedPublicPeers)) {
                        int initialSize = peers.size();
                        for (PeerImpl peer : peers.values()) {
                            if (now - peer.getLastUpdated() > 24 * 3600) {
                                peer.remove();
                            }
                            if (hasTooFewKnownPeers()) {
                                break;
                            }
                        }
                        if (hasTooManyKnownPeers()) {
                            PriorityQueue<PeerImpl> sortedPeers = new PriorityQueue<>(peers.values());
                            int skipped = 0;
                            while (skipped < Peers.minNumberOfKnownPeers) {
                                if (sortedPeers.poll() == null) {
                                    break;
                                }
                                skipped += 1;
                            }
                            while (!sortedPeers.isEmpty()) {
                                sortedPeers.poll().remove();
                            }
                        }
                        LOG.debug("Reduced peer pool size from " + initialSize + " to " + peers.size());
                    }
                    
                    for (String wellKnownPeer : wellKnownPeers) {
                        PeerImpl peer = findOrCreatePeer(wellKnownPeer, true);
                        if (peer != null && now - peer.getLastUpdated() > 3600 && now - peer.getLastConnectAttempt() > 600) {
                            peersService.submit(() -> {
                                addPeer(peer);
                                connectPeer(peer);
                            });
                        }
                    }
                    
                } catch (Exception e) {
                    LOG.debug("Error connecting to peer", e);
                }
            } catch (Throwable t) {
                LOG.error("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS", t);
                System.exit(1);
            }
            
        }
    }

    private static class GetMorePeersThread implements Runnable {

        public GetMorePeersThread() {
        }
        private final JSONStreamAware getPeersRequest;
        {
            JSONObject request = new JSONObject();
            request.put("requestType", "getPeers");
            request.put("chainId", blockchainConfig.getChain().getChainId());
            getPeersRequest = JSON.prepareRequest(request);
        }
        private volatile boolean updatedPeer;

        @Override
        public void run() {
            
            try {
                try {
                    if (hasTooManyKnownPeers()) {
                        return;
                    }
                    Peer peer = getAnyPeer(Peer.State.CONNECTED, true);
                    if (peer == null) {
                        return;
                    }
                    JSONObject response = peer.send(getPeersRequest, blockchainConfig.getChain().getChainId(), 10 * 1024 * 1024,
                            false);
                    if (response == null) {
                        return;
                    }
                    JSONArray peers = (JSONArray)response.get("peers");
                    Set<String> addedAddresses = new HashSet<>();
                    if (peers != null) {
                        JSONArray services = (JSONArray)response.get("services");
                        boolean setServices = (services != null && services.size() == peers.size());
                        int now = timeService.getEpochTime();
                        for (int i=0; i<peers.size(); i++) {
                            String announcedAddress = (String)peers.get(i);
                            PeerImpl newPeer = findOrCreatePeer(announcedAddress, true);
                            if (newPeer != null) {
                                if (now - newPeer.getLastUpdated() > 24 * 3600) {
                                    newPeer.setLastUpdated(now);
                                    updatedPeer = true;
                                }
                                if (Peers.addPeer(newPeer) && setServices) {
                                    newPeer.setServices(Long.parseUnsignedLong((String)services.get(i)));
                                }
                                addedAddresses.add(announcedAddress);
                                if (hasTooManyKnownPeers()) {
                                    break;
                                }
                            }
                        }
                        if (savePeers && updatedPeer) {
                            updateSavedPeers();
                            updatedPeer = false;
                        }
                    }
                    
                    JSONArray myPeers = new JSONArray();
                    JSONArray myServices = new JSONArray();
                    Peers.getAllPeers().forEach(myPeer -> {
                        if (!myPeer.isBlacklisted() && myPeer.getAnnouncedAddress() != null
                                && myPeer.getState() == Peer.State.CONNECTED && myPeer.shareAddress()
                                && !addedAddresses.contains(myPeer.getAnnouncedAddress())
                                && !myPeer.getAnnouncedAddress().equals(peer.getAnnouncedAddress())) {
                            myPeers.add(myPeer.getAnnouncedAddress());
                            myServices.add(Long.toUnsignedString(((PeerImpl) myPeer).getServices()));
                        }
                    });
                    if (myPeers.size() > 0) {
                        JSONObject request = new JSONObject();
                        request.put("requestType", "addPeers");
                        request.put("peers", myPeers);
                        request.put("services", myServices);            // Separate array for backwards compatibility
                        request.put("chainId", blockchainConfig.getChain().getChainId());
                        peer.send(JSON.prepareRequest(request), blockchainConfig.getChain().getChainId(), 0, false);
                    }
                    
                } catch (Exception e) {
                    LOG.debug("Error requesting peers from a peer", e);
                }
            } catch (Throwable t) {
                LOG.error("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS", t);
                System.exit(1);
            }
            
        }

        private void updateSavedPeers() {
            int now = timeService.getEpochTime();
            //
            // Load the current database entries and map announced address to database entry
            //
            List<PeerDb.Entry> oldPeers = PeerDb.loadPeers();
            Map<String, PeerDb.Entry> oldMap = new HashMap<>(oldPeers.size());
            oldPeers.forEach(entry -> oldMap.put(entry.getAddress(), entry));
            //
            // Create the current peer map (note that there can be duplicate peer entries with
            // the same announced address)
            //
            Map<String, PeerDb.Entry> currentPeers = new HashMap<>();
            UUID chainId = blockchainConfig.getChain().getChainId();
            Peers.peers.values().forEach(peer -> {
                if (peer.getAnnouncedAddress() != null && !peer.isBlacklisted() && chainId.equals(peer.getChainId()) && now - peer.getLastUpdated() < 7*24*3600) {
                    currentPeers.put(peer.getAnnouncedAddress(),
                            new PeerDb.Entry(peer.getAnnouncedAddress(), peer.getServices(), peer.getLastUpdated()));
                }
            });
            //
            // Build toDelete and toUpdate lists
            //
            List<PeerDb.Entry> toDelete = new ArrayList<>(oldPeers.size());
            oldPeers.forEach(entry -> {
                if (currentPeers.get(entry.getAddress()) == null)
                    toDelete.add(entry);
            });
            List<PeerDb.Entry> toUpdate = new ArrayList<>(currentPeers.size());
            currentPeers.values().forEach(entry -> {
                PeerDb.Entry oldEntry = oldMap.get(entry.getAddress());
                if (oldEntry == null || entry.getLastUpdated() - oldEntry.getLastUpdated() > 24*3600)
                    toUpdate.add(entry);
            });
            //
            // Nothing to do if all of the lists are empty
            //
            if (toDelete.isEmpty() && toUpdate.isEmpty())
                return;
            //
            // Update the peer database
            //
            TransactionalDataSource dataSource = lookupDataSource();
            try {
                dataSource.begin();
                PeerDb.deletePeers(toDelete);
                PeerDb.updatePeers(toUpdate);
                dataSource.commit();
            } catch (Exception e) {
                dataSource.rollback();
                throw e;
            }
        }
    }



}
