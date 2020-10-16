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

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.p2p.PeerInfo;
import com.apollocurrency.aplwallet.api.p2p.request.BaseP2PRequest;
import com.apollocurrency.aplwallet.api.p2p.request.ProcessBlockRequest;
import com.apollocurrency.aplwallet.api.p2p.request.ProcessTransactionsRequest;
import com.apollocurrency.aplwallet.apl.core.app.runnable.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.core.app.runnable.limiter.TimeLimiterService;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.http.API;
import com.apollocurrency.aplwallet.apl.core.http.APIEnum;
import com.apollocurrency.aplwallet.apl.core.rest.converter.BlockConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.TransactionConverter;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
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
import com.apollocurrency.aplwallet.apl.util.task.Tasks;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class PeersService {

    public static final int MIN_COMPRESS_SIZE = 256;
    static final int MAX_APPLICATION_LENGTH = 20;
    static final int MAX_ANNOUNCED_ADDRESS_LENGTH = 200;
    private static final Logger LOG = LoggerFactory.getLogger(PeersService.class);
    private static final Version MAX_VERSION = Constants.VERSION;
    private static final int sendTransactionsBatchSize = 500;
    private final static String BACKGROUND_SERVICE_NAME = "PeersService";
    public static int DEFAULT_CONNECT_TIMEOUT = 2000; //2s default websocket connect timeout
    public static int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    public static boolean getMorePeers;
    //TODO:  hardcode in Constants and use from there
    public static int MAX_REQUEST_SIZE = 10 * 1024 * 1024;
    public static int MAX_RESPONSE_SIZE = 40 * 1024 * 1024;
    public static int MAX_MESSAGE_SIZE = 40 * 1024 * 1024;
    public static boolean useTLS;
    public static int maxNumberOfConnectedPublicPeers;
    public static boolean ignorePeerAnnouncedAddress;
    public static boolean hideErrorDetails;
    public static PeerInfo myPI;
    public static int myPort;
    static int readTimeout;
    static int blacklistingPeriod;
    static boolean useWebSockets;
    static int webSocketIdleTimeout;
    static boolean useProxy;
    static boolean isGzipEnabled;
    static int minNumberOfKnownPeers;
    static boolean enableHallmarkProtection;
    static boolean usePeersDb;
    static boolean savePeers;
    static boolean cjdnsOnly;
    private static String myHallmark;
    private static int maxNumberOfInboundConnections;
    private static int maxNumberOfOutboundConnections;
    private static int maxNumberOfKnownPeers;
    private static int pushThreshold;
    private static int pullThreshold;
    private static int sendToPeersLimit;
    private static JSONObject myPeerInfo;
    public final ExecutorService peersExecutorService = new QueuedThreadPool(2, 15, "PeersExecutorService");
    public final boolean isLightClient;
    @Getter
    final BlockchainConfig blockchainConfig;
    private final Listeners<Peer, Event> listeners = new Listeners<>();
    /**
     * Map of ANNOUNCED address with port to peer. Contains only peers that are connectable
     * (has announced public address)
     */

    private final ConcurrentMap<String, Peer> connectablePeers = new ConcurrentHashMap<>();
    /**
     * Map of incoming peers only. In incoming peer announces some public address, it will
     * be added to connectablePeers
     */
    private final ConcurrentMap<String, Peer> inboundPeers = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor sendingService;
    private final TimeLimiterService timeLimiterService;
    private final PropertiesHolder propertiesHolder;
    private final Blockchain blockchain;
    private final PeerHttpServer peerHttpServer;
    private final TaskDispatchManager taskDispatchManager;
    private final AccountService accountService;
    List<String> wellKnownPeers;
    Set<String> knownBlacklistedPeers;
    boolean shutdown = false;
    boolean suspend = false;
    private List<Peer.Service> myServices = new ArrayList<>();
    private BlockchainState currentBlockchainState;
    private JSONStreamAware myPeerInfoRequest;
    private JSONStreamAware myPeerInfoResponse;
    private BlockchainProcessor blockchainProcessor;
    private volatile TimeService timeService;
    private final TransactionConverter transactionConverter;
    private final BlockConverter blockConverter;
//    private final ExecutorService txSendingDispatcher;

    @Inject
    public PeersService(PropertiesHolder propertiesHolder,
                        BlockchainConfig blockchainConfig,
                        Blockchain blockchain,
                        TimeService timeService,
                        TaskDispatchManager taskDispatchManager,
                        PeerHttpServer peerHttpServer,
                        TimeLimiterService timeLimiterService,
                        AccountService accountService,
                        TransactionConverter transactionConverter,
                        BlockConverter blockConverter) {
        this.propertiesHolder = propertiesHolder;
        this.blockchainConfig = blockchainConfig;
        this.blockchain = blockchain;
        this.timeService = timeService;
        this.taskDispatchManager = taskDispatchManager;
        this.peerHttpServer = peerHttpServer;
        this.timeLimiterService = timeLimiterService;
        this.accountService = accountService;
        this.transactionConverter = transactionConverter;
        this.blockConverter = blockConverter;
        int asyncTxSendingPoolSize = propertiesHolder.getIntProperty("apl.maxAsyncPeerSendingPoolSize", 30);
//        this.txSendingDispatcher = new ThreadPoolExecutor(5, asyncTxSendingPoolSize, 10_000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(asyncTxSendingPoolSize), new NamedThreadFactory("P2PTxSendingPool", true));

        this.sendingService = new TimeTraceDecoratedThreadPoolExecutor(10, asyncTxSendingPoolSize, 10_000, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(1000), new NamedThreadFactory("PeersSendingService"));
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
                long maxBalanceAPL = blockchainConfig.getCurrentConfig().getMaxBalanceAPL();
                Hallmark hallmark = Hallmark.parseHallmark(myHallmark, maxBalanceAPL);
                if (!hallmark.isValid()) {
                    throw new PeerRuntimeException("Hallmark is not valid");
                }
                if (peerHttpServer.getMyExtAddress() != null) {
                    if (!hallmark.getHost().equals(myHost)) {
                        throw new PeerRuntimeException("Invalid hallmark host");
                    }
                    if (myPort != hallmark.getPort()) {
                        throw new PeerRuntimeException("Invalid hallmark port");
                    }
                }
            } catch (RuntimeException e) {
                LOG.error("Your hallmark is invalid: " + myHallmark + " for your address: " + peerHttpServer.getMyExtAddress());
                throw new PeerRuntimeException(e.toString(), e);
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
        if (maxNumberOfInboundConnections <= 0) {
            throw new IllegalArgumentException("Wrong apl.maxNumberOfInboundConnections property value " + maxNumberOfInboundConnections);
        }

        maxNumberOfOutboundConnections = propertiesHolder.getIntProperty("apl.maxNumberOfOutboundConnections");
        if (maxNumberOfOutboundConnections <= 0) {
            throw new IllegalArgumentException("Wrong apl.maxNumberOfOutboundConnections property value " + maxNumberOfOutboundConnections);
        }

        maxNumberOfConnectedPublicPeers = Math.min(propertiesHolder.getIntProperty("apl.maxNumberOfConnectedPublicPeers"),
            maxNumberOfOutboundConnections);
        maxNumberOfKnownPeers = propertiesHolder.getIntProperty("apl.maxNumberOfKnownPeers");
        minNumberOfKnownPeers = propertiesHolder.getIntProperty("apl.minNumberOfKnownPeers");
        if (minNumberOfKnownPeers > maxNumberOfInboundConnections) {
            throw new IllegalArgumentException("apl.maxNumberOfKnownPeers=" + maxNumberOfKnownPeers + " is less than apl.minNumberOfKnownPeers=" + minNumberOfKnownPeers);
        }

        connectTimeout = propertiesHolder.getIntProperty("apl.connectTimeout", DEFAULT_CONNECT_TIMEOUT);
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

        // moved to Weld Event
        /* Account.addListener(account -> connectablePeers.values().forEach(peer -> {
            if (peer.getHallmark() != null && peer.getHallmark().getAccountId() == account.getId()) {
                listeners.notify(peer, Event.WEIGHT);
            }
        }), AccountEventType.BALANCE);*/

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

            dispatcher.invokeInit(peerLoaderTask);
        }
        dispatcher.invokeAfter(Task.builder()
            .name("UnresolvedPeersAnalyzer")
            .task(new UnresolvedPeersAnalyzer(unresolvedPeers))
            .build());
        if (!propertiesHolder.isOffline()) {
            dispatcher.schedule(Task.builder()
                .name("PeerConnecting")
                .delay(20000)
                .task(new PeerConnectingThread(timeService, this))
                .build());

            dispatcher.schedule(Task.builder()
                .name("PeerUnBlacklisting")
                .delay(60000)
                .task(new PeerUnBlacklistingThread(timeService, this))
                .build());

            if (getMorePeers) {
                dispatcher.schedule(Task.builder()
                    .name("GetMorePeers")
                    .delay(20000)
                    .task(new GetMorePeersThread(timeService, this))
                    .build());
            }
        }
    }

    private void fillMyPeerInfo() {
        myPeerInfo = new JSONObject();
        PeerInfo pi = new PeerInfo();
        LOG.debug("Start filling 'MyPeerInfo'...");
        List<Peer.Service> servicesList = new ArrayList<>();
        PeerAddress myExtAddress = peerHttpServer.getMyExtAddress();

        if (myExtAddress != null) {
            String host = myExtAddress.getHost();
            int port = myExtAddress.getPort();
            String announcedAddress = myExtAddress.getAddrWithPort();
            LOG.debug("Peer external address  = {} : {} + {}", host, port, announcedAddress);
            if (announcedAddress.length() > MAX_ANNOUNCED_ADDRESS_LENGTH) {
                throw new PeerRuntimeException("Invalid announced address length: " + announcedAddress);
            }
            pi.setAnnouncedAddress(announcedAddress);
        } else {
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
            if (dispatcher != null) {
                dispatcher.shutdown();
            }
            Tasks.shutdownExecutor("sendingService", sendingService, 2);
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
        try {
            Tasks.shutdownExecutor("peersService", peersExecutorService, 5);
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
        try {
            timeLimiterService.shutdown();
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    public void suspend() {
        LOG.debug("peerHttpServer suspend...");
        if (peerHttpServer != null) {
            suspend = peerHttpServer.suspend();
        }
        TaskDispatcher dispatcher = taskDispatchManager.getDispatcher(BACKGROUND_SERVICE_NAME);
        dispatcher.suspend();
        getActivePeers().forEach((p) -> {
            p.deactivate("Suspending peer operations");
        });
    }

    public void resume() {
        LOG.debug("peerHttpServer resume...");
        if (suspend && peerHttpServer != null) {
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
        if (adrWithPort != null && adrWithPort.length() <= MAX_ANNOUNCED_ADDRESS_LENGTH) {
            pa = new PeerAddress(adrWithPort);
            if (!pa.isValid()) {
                pa = null;
            }
        }
        return pa;
    }

    public Collection<Peer> getAllConnectablePeers() {
        Collection<Peer> res = Collections.unmodifiableCollection(connectablePeers.values());
        return res;
    }

    public Collection<Peer> getAllPeers() {
        List<Peer> peers = new ArrayList(connectablePeers.values());
        peers.addAll(inboundPeers.values());
        Collection<Peer> res = Collections.unmodifiableCollection(peers);
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
        for (Peer peer : inboundPeers.values()) {
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
        PeerAddress pa = new PeerAddress(hostWithPort);
        return connectablePeers.get(pa.getAddrWithPort());
    }

    public List<Peer> getInboundPeers() {
        return getPeers(Peer::isInbound);
    }

    public List<Peer> getOutboundPeers() {
        return getPeers(Peer::isOutbound);
    }

    public Set<Peer> getAllConnectedPeers() {
        Collection<? extends Peer> knownPeers = getActivePeers();
        return new HashSet<>(knownPeers);
    }

    public boolean hasTooManyInboundPeers() {
        return getPeers(Peer::isInbound, maxNumberOfInboundConnections).size() >= maxNumberOfInboundConnections;
    }

    public boolean hasTooManyOutboundConnections() {
        return getPeers(peer -> !peer.isBlacklisted() && peer.getState() == PeerState.CONNECTED && peer.getAnnouncedAddress() != null,
            maxNumberOfOutboundConnections).size() >= maxNumberOfOutboundConnections;
    }

    public boolean isMyAddress(PeerAddress pa) {
        if (pa == null) {
            return true;
        }
        //TODO: many ports: http, https, ssl
        if ((pa.isLocal() && myPort == pa.getPort())) {
            return true;
        }
        String ca = propertiesHolder.getStringProperty("apl.myAddress", "");
        if (!ca.isEmpty()) {
            PeerAddress myConfiguredAddr = new PeerAddress(ca);
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

    public Peer findOrCreatePeer(PeerAddress actualAddr, final String announcedAddress, final boolean create) {
        if (actualAddr == null) {
            actualAddr = resolveAnnouncedAddress(announcedAddress);
        }
        if (actualAddr == null) {
            return null;
        }
        String hostName = actualAddr.getHostName();
        if (hostName != null && cjdnsOnly && !hostName.startsWith("fc")) {
            return null;
        }
        //do not create peer for ourselves
        if (isMyAddress(actualAddr)) {
            return null;
        }

        Peer peer;
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
        peer = new PeerImpl(actualAddr, apa, blockchainConfig, blockchain, timeService, peerHttpServer.getPeerServlet(),
            this, timeLimiterService.acquireLimiter("P2PTransport"), accountService);
        listeners.notify(peer, Event.NEW_PEER);
        if (apa != null) {
            connectablePeers.put(apa.getAddrWithPort(), peer);
        } else {
            inboundPeers.put(actualAddr.getAddrWithPort(), peer);
        }
        return peer;
    }

    public void setAnnouncedAddress(PeerImpl peer, String newAnnouncedAddress) {
        if (StringUtils.isBlank(newAnnouncedAddress)) {
            LOG.debug("newAnnouncedAddress is empty for host: {}, ignoring", peer.getHostWithPort());
        }
        PeerAddress newPa = resolveAnnouncedAddress(newAnnouncedAddress);
        if (newPa == null) {
            return;
        }
        String oldAnnouncedAddr = peer.getAnnouncedAddress();
        Peer oldPeer = null;
        if (oldAnnouncedAddr != null) {
            oldPeer = connectablePeers.get(peer.getAnnouncedAddress());
        }
        if (oldPeer != null) {
            PeerAddress oldPa = new PeerAddress(oldPeer.getAnnouncedAddress());
            if (newPa.compareTo(oldPa) != 0) {
                LOG.debug("Removing old announced address {} for peer {}:{}", oldPa, oldPeer.getHost(), oldPeer.getPort());

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

    //we do not need inbound peers that is not connected,
    // but we should be carefully because peer could be connecting right now
    void cleanupPeers(Peer peer) {
        long now = System.currentTimeMillis();
        Set<Peer> toDelete = new HashSet<>();
        if (peer != null) {
            toDelete.add(peer);
        }
        inboundPeers.values().stream()
            .filter(p -> (p.getState() != PeerState.CONNECTED
                    && now - p.getLastActivityTime() > webSocketIdleTimeout
                )
            )
            .forEachOrdered(toDelete::add);

        toDelete.forEach(p -> {
            p.deactivate("Cleanup of inbounds");
            inboundPeers.remove(p.getHostWithPort());
        });
    }

    public Peer removePeer(Peer peer) {
        Peer p = null;
        if (peer.getAnnouncedAddress() != null) {
            PeerDb.Entry entry = new PeerDb.Entry(peer.getAnnouncedAddress(), 0, 0);
            PeerDb.deletePeer(entry);
            if (connectablePeers.containsKey(peer.getAnnouncedAddress())) {
                p = connectablePeers.remove(peer.getAnnouncedAddress());
            }
            LOG.debug("Removed peer {}", peer);
        }
        cleanupPeers(peer);
        return p;
    }

    public boolean connectPeer(Peer peer) {
        Objects.requireNonNull(peer, "peer is NULL");
        boolean res = false;
        if (peer.getState() == PeerState.CONNECTED) {
            return true;
        }
        peer.unBlacklist();
        PeerAddress pa = resolveAnnouncedAddress(peer.getAnnouncedAddress());
        if (pa != null && !isMyAddress(pa)) {
            res = ((PeerImpl) peer).handshake();
        }
        if (res) {
            connectablePeers.putIfAbsent(peer.getHostWithPort(), (PeerImpl) peer);
        }
        return res;
    }

    public void sendToSomePeers(Block block) {
        ProcessBlockRequest request = new ProcessBlockRequest(blockConverter.convert(block), blockchainConfig.getChain().getChainId());
        LOG.debug("Send to some peers the block: {} at height: {}", block.getId(), block.getHeight());
        sendToSomePeersAsync(request);
    }

    public void sendToSomePeers(List<? extends Transaction> transactions) {
        int nextBatchStart = 0;
        while (nextBatchStart < transactions.size()) {
            List<TransactionDTO> transactionsData = new ArrayList<>();
            for (int i = nextBatchStart; i < nextBatchStart + sendTransactionsBatchSize && i < transactions.size(); i++) {
                transactionsData.add(transactionConverter.convert(transactions.get(i)));
            }
            BaseP2PRequest request = new ProcessTransactionsRequest(transactionsData, blockchainConfig.getChain().getChainId());
            try {
                sendToSomePeersAsync(request);
            } catch (RejectedExecutionException e) {
                log.debug("Unable to send async batch, skip it");
            }
            nextBatchStart += sendTransactionsBatchSize;
        }
    }

    private void checkP2PUp() {
        if (shutdown || suspend) {
            String errorMessage = String.format("Cannot send request to peers. Peer server was %s", suspend ? "suspended" : "shutdown");
            LOG.error(errorMessage);
            throw new PeerRuntimeException(errorMessage);
        }
    }
    public void sendToSomePeersAsync(BaseP2PRequest request) {
        sendingService.submit(() -> {
            long time = System.nanoTime();
            checkP2PUp();
            Set<Peer> peers = new HashSet<>(getPeers(PeerState.CONNECTED));
            int counterOfPeersToSend = Math.min(peers.size(), sendToPeersLimit);
            for (final Peer peer : peers) {
                if (counterOfPeersToSend == 0) {
                    break;
                }
                if (!peer.isBlacklisted()
                    && peer.getState() == PeerState.CONNECTED // skip not connected peers
                    && peer.getBlockchainState() != BlockchainState.LIGHT_CLIENT
                ) {
                    counterOfPeersToSend--;
                    try {
                        peer.sendAsync(request);
                    } catch (RejectedExecutionException e) {
                        try {
                            log.debug("Failed to send to peer {} asynchronously, will send synchronously", peer.getHost());
                            peer.send(request);
                        } catch (PeerNotConnectedException peerNotConnectedException) {
                            log.debug("Peer not connected, failed to send request {}", peerNotConnectedException.getMessage());
                        }
                    }
                }
            }
            log.trace("Time to send to peers async {}", (System.nanoTime() - time));
        });
    }



    private void sendToSomePeers(BaseP2PRequest request) {
        checkP2PUp();
        log.debug("Sending Service STATS: current running {}, waiting {}", sendingService.getActiveCount(), sendingService.getQueue().size());
        sendingService.submit(() -> {
            int successful = 0;
            List<Future<JSONObject>> expectedResponses = new ArrayList<>();
            Set<Peer> peers = new HashSet<>(getPeers(PeerState.CONNECTED));
            int counterOfPeersToSend = Math.min(peers.size(), sendToPeersLimit);

            LOG.debug("Prepare sending data to CONNECTED peer(s) = [{}]", peers.size());
            for (final Peer peer : peers) {

                if (enableHallmarkProtection && peer.getWeight() < pushThreshold) {
                    continue;
                }

                if (!peer.isBlacklisted()
                    && peer.getState() == PeerState.CONNECTED // skip not connected peers
                    && peer.getBlockchainState() != BlockchainState.LIGHT_CLIENT
                ) {
                    LOG.trace("Prepare send to peer = {}", peer);
                    Future<JSONObject> futureResponse = peersExecutorService.submit(() -> peer.send(request));
                    expectedResponses.add(futureResponse);
                }
                if (expectedResponses.size() >= counterOfPeersToSend - successful) {
                    for (Future<JSONObject> future : expectedResponses) {
                        try {
                            JSONObject response = future.get();
                            if (response != null && response.get("error") == null) {
                                successful += 1;
                            } else {
                                LOG.debug("Send to peer error");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } catch (ExecutionException e) {
                            LOG.debug("Error in sendToSomePeers", e);
                        }

                    }
                    expectedResponses.clear();
                }
                if (successful >= counterOfPeersToSend) {
                    LOG.debug("SendToSomePeers() success.");
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

    public enum Event {
        BLACKLIST, UNBLACKLIST, DEACTIVATE, REMOVE,
        DOWNLOADED_VOLUME, UPLOADED_VOLUME, WEIGHT,
        ADDED_ACTIVE_PEER, CHANGED_ACTIVE_PEER,
        NEW_PEER, ADD_INBOUND, REMOVE_INBOUND, CHANGED_SERVICES
    }

}
