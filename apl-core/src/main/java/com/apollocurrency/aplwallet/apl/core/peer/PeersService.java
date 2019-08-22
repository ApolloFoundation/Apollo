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

import com.apollocurrency.aplwallet.api.p2p.PeerInfo;
import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.http.API;
import com.apollocurrency.aplwallet.apl.core.http.APIEnum;
import com.apollocurrency.aplwallet.apl.core.task.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.Filter;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.Listeners;
import com.apollocurrency.aplwallet.apl.util.QueuedThreadPool;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.task.NamedThreadFactory;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;
import com.apollocurrency.aplwallet.apl.util.task.TaskOrder;
import com.apollocurrency.aplwallet.apl.util.task.Tasks;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;
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

@Singleton
public class PeersService {

    private static final Logger LOG = LoggerFactory.getLogger(PeersService.class);


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

    List<String> wellKnownPeers;
    Set<String> knownBlacklistedPeers;

    static int connectTimeout;
    static int readTimeout;
    static int blacklistingPeriod;
    public static boolean getMorePeers;
    //TODO:  hardcode in Constants and use from there
    public static int MAX_REQUEST_SIZE= 10 * 1024 * 1024;
    public static int MAX_RESPONSE_SIZE= 40 * 1024 * 1024;
    public static int MAX_MESSAGE_SIZE= 40 * 1024 * 1024;

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
    private List<Peer.Service> myServices;
    private volatile BlockchainState currentBlockchainState;
    private volatile JSONStreamAware myPeerInfoRequest;
    private volatile JSONStreamAware myPeerInfoResponse;

    boolean shutdown = false;
    boolean suspend = false;

    private final Listeners<Peer, Event> listeners = new Listeners<>();

    private final static String BACKGROUND_SERVICE_NAME = "PeersService";
    /**
     * Map of ANNOUNCED address with port to peer. Contains only peers that are connectable
     * (has announced public address) 
     */

    private final ConcurrentMap<String, PeerImpl> connectablePeers = new ConcurrentHashMap<>();
    /**
     * Map of incoming peers only. In incoming peer announces some public address, it will
     * be added to connectablePeers
     */
    private final ConcurrentMap<String, PeerImpl> inboundPeers = new ConcurrentHashMap<>();
    public final ExecutorService peersExecutorService = new QueuedThreadPool(2, 15, "PeersExecutorService");

    private final ExecutorService sendingService = Executors.newFixedThreadPool(10, new NamedThreadFactory("PeersSendingService"));

    // TODO: YL remove static instance later
    private final PropertiesHolder propertiesHolder;
    final BlockchainConfig blockchainConfig;
    private final Blockchain blockchain;
    private BlockchainProcessor blockchainProcessor;
    private volatile TimeService timeService;

    private final PeerHttpServer peerHttpServer;
    private final TaskDispatchManager taskDispatchManager;

    public static int myPort;
    public final boolean isLightClient;

    @Inject
    public PeersService(PropertiesHolder propertiesHolder, BlockchainConfig blockchainConfig, Blockchain blockchain,
                        TimeService timeService, TaskDispatchManager taskDispatchManager, PeerHttpServer peerHttpServer) {
        this.propertiesHolder = propertiesHolder;
        this.blockchainConfig = blockchainConfig;
        this.blockchain = blockchain;
        this.timeService = timeService;
        this.taskDispatchManager = taskDispatchManager;
        this.peerHttpServer = peerHttpServer;

        isLightClient = propertiesHolder.isLightClient();
    }

    private BlockchainProcessor lookupBlockchainProcessor() {
        if (blockchainProcessor == null) blockchainProcessor = CDI.current().select(BlockchainProcessor.class).get();
        return blockchainProcessor;
    }

    public void init() {

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
        if (myHallmark != null && PeersService.myHallmark.length() > 0) {
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

        addListener(peer -> peersExecutorService.submit(() -> {
            if (peer.getAnnouncedAddress() != null && !peer.isBlacklisted()) {
                try {
                    PeerDb.updatePeer((PeerImpl) peer);
                } catch (RuntimeException e) {
                    LOG.error("Unable to update peer database", e);
                }
            }
        }), PeersService.Event.CHANGED_SERVICES);

        Account.addListener(account -> connectablePeers.values().forEach(peer -> {
            if (peer.getHallmark() != null && peer.getHallmark().getAccountId() == account.getId()) {
                listeners.notify(peer, Event.WEIGHT);
            }
        }), Account.Event.BALANCE);

        configureBackgroundTasks();

        peerHttpServer.start();
    }

    private void configureBackgroundTasks() {
        final List<String> defaultPeers = blockchainConfig.getChain().getDefaultPeers();
        final List<Future<String>> unresolvedPeers = Collections.synchronizedList(new ArrayList<>());
        TaskDispatcher dispatcher = taskDispatchManager.newBackgroundDispatcher(BACKGROUND_SERVICE_NAME);

        if (!propertiesHolder.isOffline()) {
            Task peerLoaderTask = Task.builder()
                    .name("PeerLoader")
                    .task(new PeerLoaderThread(defaultPeers, unresolvedPeers, timeService, this))
                    .build();

            dispatcher.schedule(peerLoaderTask, TaskOrder.INIT);
        }
        dispatcher.schedule(Task.builder()
                .name("UnresolvedPeersAnalyzer")
                .task(new UnresolvedPeersAnalyzer(unresolvedPeers))
                .build(), TaskOrder.AFTER);
        if (!propertiesHolder.isOffline()) {
            dispatcher.schedule(Task.builder()
                    .name("PeerConnecting")
                    .delay(20000)
                    .task(new PeerConnectingThread(timeService, this))
                    .build(),TaskOrder.TASK);

            dispatcher.schedule(Task.builder()
                    .name("PeerUnBlacklisting")
                    .delay(60000)
                    .task(new PeerUnBlacklistingThread(timeService, this))
                    .build(), TaskOrder.TASK);

            if (getMorePeers) {
                dispatcher.schedule(Task.builder()
                        .name("GetMorePeers")
                        .delay(20000)
                        .task(new GetMorePeersThread(timeService, this))
                        .build(), TaskOrder.TASK);
            }
        }
    }

    private void fillMyPeerInfo() {
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
        pi.setShareAddress(peerHttpServer.isShareMyAddress());
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

    public PeerInfo getMyPeerInfo() {
        return myPI;
    }

    public void shutdown() {
        try {
            shutdown = true;
            peerHttpServer.shutdown();
            TaskDispatcher dispatcher = taskDispatchManager.getDispatcher(BACKGROUND_SERVICE_NAME);
            dispatcher.shutdown();
            Tasks.shutdownExecutor("sendingService", sendingService, 2);
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
        try {
            Tasks.shutdownExecutor("peersService", peersExecutorService, 5);
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    public void suspend() {
        if(peerHttpServer!=null){
           suspend = peerHttpServer.suspend();
        }
        TaskDispatcher dispatcher = taskDispatchManager.getDispatcher(BACKGROUND_SERVICE_NAME);
        dispatcher.suspend();
        getActivePeers().forEach((p) -> {
            p.deactivate("Suspending peer operations");
        });
    }

    public void resume() {
        if (suspend && peerHttpServer!=null) {
            suspend = !peerHttpServer.resume();
        }
        TaskDispatcher dispatcher = taskDispatchManager.getDispatcher(BACKGROUND_SERVICE_NAME);
        dispatcher.resume();
    }

    public boolean addListener(Listener<Peer> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public boolean removeListener(Listener<Peer> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public void notifyListeners(Peer peer, Event eventType) {
        listeners.notify(peer, eventType);
    }

    public PeerAddress resolveAnnouncedAddress(String adrWithPort) {
        PeerAddress pa = null;
        if(adrWithPort!=null && adrWithPort.length()<=MAX_ANNOUNCED_ADDRESS_LENGTH){
            pa = new PeerAddress(adrWithPort);
            if(!pa.isValid()){
                pa=null;
            }
        }
        return pa;
    }

    public Collection<Peer> getAllConnectablePeers() {
        Collection<Peer> res =  Collections.unmodifiableCollection(connectablePeers.values());
        return res;
    }

    public Collection<Peer> getAllPeers() {
        List<Peer> peers = new ArrayList(connectablePeers.values());
        peers.addAll(inboundPeers.values());
        Collection<Peer> res =  Collections.unmodifiableCollection(peers);
        return res;
    }

    public List<Peer> getActivePeers() {
        return getPeers(peer -> peer.getState() == PeerState.CONNECTED);
    }

    public List<Peer> getPeers(final PeerState state) {
        return getPeers(peer -> peer.getState() == state);
    }

    public List<Peer> getPeers(Filter<Peer> filter) {
        return getPeers(filter, Integer.MAX_VALUE);
    }

    public List<Peer> getPeers(Filter<Peer> filter, int limit) {
        List<Peer> result = new ArrayList<>();
        for (Peer peer : connectablePeers.values()) {
            if (filter.test(peer)) {
                result.add(peer);
                if (result.size() >= limit) {
                    break;
                }
            }
        }
        for(Peer peer:  inboundPeers.values()){
            if (filter.test(peer)) {
                result.add(peer);
                if (result.size() >= limit) {
                    break;
                }
            }            
        }
        return result;
    }

    public Peer getPeer(String hostWithPort) {
        PeerAddress pa = new PeerAddress( hostWithPort);
        return connectablePeers.get(pa.getAddrWithPort());
    }

    public List<Peer> getInboundPeers() {
        return getPeers(Peer::isInbound);
    }

    public List<Peer> getOutboundPeers() {
        return getPeers(Peer::isOutbound);
    }

    public boolean hasTooManyInboundPeers() {
        return getPeers(Peer::isInbound, maxNumberOfInboundConnections).size() >= maxNumberOfInboundConnections;
    }

    public boolean hasTooManyOutboundConnections() {
        return getPeers(peer -> !peer.isBlacklisted() && peer.getState() == PeerState.CONNECTED && peer.getAnnouncedAddress() != null,
                maxNumberOfOutboundConnections).size() >= maxNumberOfOutboundConnections;
    }


    public boolean isMyAddress(PeerAddress pa) {
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

    public PeerImpl findOrCreatePeer(PeerAddress actualAddr, final String announcedAddress, final boolean create) {
        if(actualAddr==null){
            actualAddr=resolveAnnouncedAddress(announcedAddress);
        }
        if(actualAddr==null){
            return null;
        }
        String hostName = actualAddr.getHostName();
        if(hostName!=null && cjdnsOnly && !hostName.startsWith("fc")){
            return null;
        }
        //do not create peer for ourselves
        if (isMyAddress(actualAddr)) {
            return null;
        }

        PeerImpl peer;
        //search by connection addres first
        if ((peer = inboundPeers.get(actualAddr.getAddrWithPort())) != null) {
            LOG.trace("Returning existing peer from inbound map {}", peer);
            return peer;
        }
        if ((peer = connectablePeers.get(actualAddr.getAddrWithPort())) != null) {
            LOG.trace("Returning existing peer from connectable map {}", peer);
            return peer;
        }
        
        if (!create) {
            return null;
        }
        //check not-null announced address and do not create peer
        //if it is not resolvable
        PeerAddress apa = resolveAnnouncedAddress(announcedAddress);
        peer = new PeerImpl(actualAddr, apa, blockchainConfig, blockchain, timeService, peerHttpServer.getPeerServlet(), this);
        if(apa!=null){
            connectablePeers.put(apa.getAddrWithPort(),peer);
        }else{
            inboundPeers.put(actualAddr.getAddrWithPort(), peer);
        }
        return peer;
    }

    public void setAnnouncedAddress(PeerImpl peer, String newAnnouncedAddress) {
        if (StringUtils.isBlank(newAnnouncedAddress)) {
            LOG.debug("newAnnouncedAddress is empty for host: {}, ignoring", peer.getHostWithPort());
        }
        PeerAddress newPa = resolveAnnouncedAddress(newAnnouncedAddress);
        if(newPa==null){
            return;
        }
        String oldAnnouncedAddr = peer.getAnnouncedAddress();
        Peer oldPeer = null;
        if(oldAnnouncedAddr!=null){
            oldPeer = connectablePeers.get(peer.getAnnouncedAddress());
        }
        if (oldPeer != null) {
            PeerAddress oldPa = new PeerAddress(oldPeer.getAnnouncedAddress());
            if (newPa.compareTo(oldPa) != 0) {
                LOG.debug("Removing old announced address " + oldPa + " for peer " + oldPeer.getHost() + ":" + oldPeer.getPort());

                peer.setAnnouncedAddress(newAnnouncedAddress);
                oldPeer = removePeer(oldPeer);
                if (oldPeer != null) {
                    notifyListeners(oldPeer, Event.REMOVE);
                }
            }
        }
    }

    public boolean addPeer(Peer peer, String newAnnouncedAddress) {
        setAnnouncedAddress((PeerImpl) peer, newAnnouncedAddress.toLowerCase());
        return addPeer(peer);
    }

    public boolean addPeer(Peer peer) {
        
        if (peer != null && peer.getAnnouncedAddress() != null) {
            // put new or replace previous
            connectablePeers.put(peer.getAnnouncedAddress(), (PeerImpl) peer);
            listeners.notify(peer, Event.NEW_PEER);
            return true;
        }
        return false;
    }
    //we do not need inboulnd peers that is not
    //connected, but we should be carefull because peer could be connecting
    //right now
    void cleanupPeers(Peer peer) {
        long now = System.currentTimeMillis();
        Set<Peer> toDelete=new HashSet<>();
        if(peer!=null){
           toDelete.add(peer);
        }
        inboundPeers.values().stream()
                .filter((p) -> (
                        p.getState()!=PeerState.CONNECTED 
                     && now - p.getLastActivityTime() > webSocketIdleTimeout)
                )
                .forEachOrdered((p) -> toDelete.add(p));
        
        toDelete.forEach((p) -> {
            p.deactivate("Cleanup of inbounds");
            inboundPeers.remove(p.getHostWithPort());
        });
    }

    public PeerImpl removePeer(Peer peer) {
        PeerImpl p=null;
        if (peer.getAnnouncedAddress() != null) {
            PeerDb.Entry entry = new PeerDb.Entry(peer.getAnnouncedAddress(), 0, 0);
            PeerDb.deletePeer(entry);
            if (connectablePeers.containsKey(peer.getAnnouncedAddress())) {
                p = connectablePeers.remove(peer.getAnnouncedAddress());
            }
        }
        cleanupPeers(peer);
        return p;
    }

    public void connectPeer(Peer peer) {
        peer.unBlacklist();
        PeerAddress pa= resolveAnnouncedAddress(peer.getAnnouncedAddress());
        if(pa!=null && !isMyAddress(pa)){
           peer.handshake(blockchainConfig.getChain().getChainId());
        }
    }

    public void sendToSomePeers(Block block) {
        JSONObject request = block.getJSONObject();
        request.put("requestType", "processBlock");
        sendToSomePeers(request);
    }

    public void sendToSomePeers(List<? extends Transaction> transactions) {
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

    private void sendToSomePeers(final JSONObject request) {
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
            Set<Peer> peers = new HashSet<>(getPeers(PeerState.CONNECTED));
            peers.addAll(connectablePeers.values());
            LOG.trace("Prepare sending data to CONNECTED peer(s) = [{}]", peers.size());
            for (final Peer peer : peers) {

                if (enableHallmarkProtection && peer.getWeight() < pushThreshold) {
                    continue;
                }

                if ( !peer.isBlacklisted() 
                     && peer.getState() == PeerState.CONNECTED // skip not connected peers
                     && peer.getBlockchainState() != BlockchainState.LIGHT_CLIENT
                   ) {
                    LOG.trace("Prepare send to peer = {}", peer);
                    Future<JSONObject> futureResponse = peersExecutorService.submit(() -> 
                        peer.send(jsonRequest, blockchainConfig.getChain().getChainId())
                    );
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

    public Peer getAnyPeer(final PeerState state, final boolean applyPullThreshold) {
        return getWeightedPeer(getPublicPeers(state, applyPullThreshold));
    }

    public List<Peer> getPublicPeers(final PeerState state, final boolean applyPullThreshold) {
        UUID chainId = blockchainConfig.getChain().getChainId();
        return getPeers(peer -> !peer.isBlacklisted() && peer.getState() == state && chainId.equals(peer.getChainId()) && peer.getAnnouncedAddress() != null
                && (!applyPullThreshold || !enableHallmarkProtection || peer.getWeight() >= pullThreshold));
    }

    public Peer getWeightedPeer(List<Peer> selectedPeers) {
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

    public boolean hasTooFewKnownPeers() {
        return connectablePeers.size() < minNumberOfKnownPeers;
    }

    public boolean hasTooManyKnownPeers() {
        return connectablePeers.size() > maxNumberOfKnownPeers;
    }

    boolean hasEnoughConnectedPublicPeers(int limit) {
        return getPeers(peer -> !peer.isBlacklisted() && peer.getState() == PeerState.CONNECTED && peer.getAnnouncedAddress() != null
                && (!enableHallmarkProtection || peer.getWeight() > 0), limit).size() >= limit;
    }
  /**
     * Return local peer services
     *
     * @return List of local peer services
     */
  public List<Peer.Service> getServices() {
        return myServices;
    }

    private void checkBlockchainState() {
        BlockchainState state = propertiesHolder.isLightClient()
                ? BlockchainState.LIGHT_CLIENT
                : (lookupBlockchainProcessor().isDownloading() || blockchain.getLastBlockTimestamp() < timeService.getEpochTime() - 600)
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

    public JSONStreamAware getMyPeerInfoRequest() {
        checkBlockchainState();
        return myPeerInfoRequest;
    }

    public JSONStreamAware getMyPeerInfoResponse() {
        checkBlockchainState();
        return myPeerInfoResponse;
    }

    public BlockchainState getMyBlockchainState() {
        checkBlockchainState();
        return currentBlockchainState;
    }

}
