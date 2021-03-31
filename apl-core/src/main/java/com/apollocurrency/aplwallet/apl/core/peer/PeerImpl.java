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
 * Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.api.p2p.PeerInfo;
import com.apollocurrency.aplwallet.api.p2p.request.BaseP2PRequest;
import com.apollocurrency.aplwallet.api.p2p.respons.BaseP2PResponse;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.http.API;
import com.apollocurrency.aplwallet.apl.core.http.APIEnum;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.Errors;
import com.apollocurrency.aplwallet.apl.core.peer.parser.JsonReqRespParser;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.security.id.IdentityService;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.task.NamedThreadFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.google.common.util.concurrent.TimeLimiter;
import io.firstbridge.identity.cert.ActorType;
import io.firstbridge.identity.cert.CertException;
import io.firstbridge.identity.cert.CertKeyPersistence;
import io.firstbridge.identity.cert.ExtCert;
import io.firstbridge.identity.handler.IdValidator;
import io.firstbridge.identity.utils.Hex;
import java.io.ByteArrayInputStream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.StringWriter;
import static java.lang.Math.abs;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public final class PeerImpl implements Peer {

    private static final Logger LOG = LoggerFactory.getLogger(PeerImpl.class);
    @Getter
    public static final String CAN_NOT_DESERIALIZE_REQUEST_MSG = "Can not deserialize request";

    private final String host;
    private final Object servicesMonitor = new Object();
    private final ReadWriteLock stateLock = new ReentrantReadWriteLock();
    private final AtomicReference<UUID> chainId = new AtomicReference<>();
    private final boolean isLightClient;
    private final BlockchainConfig blockchainConfig;
    private final Blockchain blockchain;
    private final PeersService peers;
    private final AccountService accountService;
    @Getter
    private final PeerInfo pi = new PeerInfo();
    //Jackson JSON
    private final ObjectMapper mapper = new ObjectMapper();
    @Getter
    private final Peer2PeerTransport p2pTransport;
    @Getter
    private volatile int port;
    @Getter
    private Hallmark hallmark;
    private EnumSet<APIEnum> disabledAPIs;
    @Getter
    private Version version;
    private volatile boolean isOldVersion;
    private volatile long adjustedWeight;
    private volatile int blacklistingTime;
    @Getter
    private volatile String blacklistingCause = "unknown";
    @Getter
    private PeerState state;
    @Getter @Setter
    private volatile int lastUpdated;
    @Getter
    private volatile int lastConnectAttempt;
    private volatile long hallmarkBalance = -1;
    private volatile int hallmarkBalanceHeight;
    private volatile long services;
    @Getter
    private BlockchainState blockchainState;
    private final TimeService timeService;
    @Getter
    private volatile int failedConnectAttempts = 0;
    private String peerId;
    @Getter
    private PeerTrustLevel trustLevel = PeerTrustLevel.NOT_TRUSTED;
    private volatile ThreadPoolExecutor asyncExecutor;
    private final IdentityService identityService;
    PeerImpl(PeerAddress addrByFact,
             PeerAddress announcedAddress,
             BlockchainConfig blockchainConfig,
             Blockchain blockchain,
             TimeService timeService,
             PeerServlet peerServlet,
             PeersService peers,
             TimeLimiter timeLimiter,
             AccountService accountService,
             IdentityService identityService
    ) {
        //TODO: remove Json.org entirely from P2P
        mapper.registerModule(new JsonOrgModule());
        this.host = addrByFact.getHost();
        this.port = addrByFact.getPort();

        if (announcedAddress == null) {
            log.trace("got empty announcedAddress from host {}", getHostWithPort());
            pi.setShareAddress(false);
        } else {
            pi.setShareAddress(true);
            pi.setAnnouncedAddress(announcedAddress.getAddrWithPort());
        }
        this.disabledAPIs = EnumSet.noneOf(APIEnum.class);
        pi.setApiServerIdleTimeout(API.apiServerIdleTimeout);
        this.blockchainState = BlockchainState.UP_TO_DATE;
        this.blockchainConfig = blockchainConfig;
        this.blockchain = blockchain;
        this.timeService = timeService;
        this.peers = peers;
        isLightClient = peers.isLightClient;
        this.p2pTransport = new Peer2PeerTransport(this, peerServlet, timeLimiter);
        state = PeerState.NON_CONNECTED; // set this peer its' initial state
        this.accountService = accountService;
        this.identityService = identityService;
    }

    private void initAsyncExecutor() {
        this.asyncExecutor = new TimeTraceDecoratedThreadPoolExecutor(1, Math.max(Runtime.getRuntime().availableProcessors() / 2, 1), 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1000), new NamedThreadFactory(getHostWithPort()+ "-AsyncExecutor"));
    }

    @Override
    public String getHostWithPort() {
        PeerAddress pa = new PeerAddress(port, host);
        return pa.getAddrWithPort();
    }


    private void setState(PeerState newState) {
        // if we are even not connected and some routine say to disconnect
        // we should close all because possible we already tried to connect and have
        // client thread running
        PeerState oldState = getState();
        Lock lock = stateLock.writeLock();
        lock.lock();
        try {
            if (newState != PeerState.CONNECTED) {
                p2pTransport.disconnect();
                if (asyncExecutor != null) {
                    asyncExecutor.shutdownNow();
                }
                // limiter.runWithTimeout(p2pTransport::disconnect, 1000, TimeUnit.MILLISECONDS);
            }  else {
                if (asyncExecutor == null || asyncExecutor.isShutdown() || asyncExecutor.isTerminated() || asyncExecutor.isTerminating()) {
                    initAsyncExecutor();
                }
            }
        } finally {
            //we have to change state anyway
            this.state = newState;
            lock.unlock();
        }
        if (newState == PeerState.CONNECTED && oldState != PeerState.CONNECTED) {
            peers.notifyListeners(this, PeersService.Event.ADDED_ACTIVE_PEER);
        } else if (newState == PeerState.NON_CONNECTED) {
            peers.notifyListeners(this, PeersService.Event.CHANGED_ACTIVE_PEER);
        }
        log.debug("Peer={} {} oldState={} newState={}", this.getAnnouncedAddress(),
            newState != PeerState.CONNECTED && oldState == PeerState.CONNECTED ? "was disconnected" : "",
            oldState, newState);
    }

    @Override
    public long getDownloadedVolume() {
        return p2pTransport.getDownloadedVolume();
    }


    @Override
    public long getUploadedVolume() {
        return p2pTransport.getUploadedVolume();
    }

    public void setVersion(Version version) {
        boolean versionChanged = version == null || !version.equals(this.version);
        this.version = version;
        isOldVersion = false;
        log.trace("setVersion to Application = {} for pi = {}", pi.getApplication(), pi);
        if (Constants.APPLICATION.equals(pi.getApplication())) {
            isOldVersion = Version.isOldVersion(version, Constants.MIN_VERSION);
            if (isOldVersion) {
                if (versionChanged && log.isDebugEnabled()) {
                    log.debug(String.format("Blacklisting %s version %s", host, version));
                }
                blacklistingCause = "Old version: " + version;
                setState(PeerState.NON_CONNECTED);
                peers.notifyListeners(this, PeersService.Event.BLACKLIST);
            }
        }
        log.trace("VERSION - Peer - {} set version - {}", host, version);
    }

    @Override
    public String getApplication() {
        return pi.getApplication();
    }

    public boolean setApplication(String application) {
        boolean res = true;
        if (application == null
            || application.length() > PeersService.MAX_APPLICATION_LENGTH
            || !application.equalsIgnoreCase(Constants.APPLICATION)
        ) {
            log.debug("Invalid application value='{}' from host:{}", application, host);
            res = false;
        } else {
            this.pi.setApplication(application.trim());
        }
        return res;
    }

    @Override
    public String getPlatform() {
        return pi.getPlatform();
    }

    public void setPlatform(String platform) {
        if (platform != null && platform.length() > PeerHttpServer.MAX_PLATFORM_LENGTH) {
            throw new IllegalArgumentException("Invalid platform length: " + platform.length());
        }
        pi.setPlatform(platform);
    }

    @Override
    public UUID getChainId() {
        return chainId.get();
    }

    public void setChainId(UUID chainId) {
        this.chainId.set(chainId);
    }

    @Override
    public String getSoftware() {
        return Convert.truncate(pi.getApplication(), "?", 10, false)
            + " (" + Convert.truncate(version.toString(), "?", 10, false) + ")"
            + " @ " + Convert.truncate(pi.getPlatform(), "?", 10, false);
    }

    @Override
    public int getApiPort() {
        return pi.getApiPort();
    }

    public void setApiPort(Integer apiPortValue) {
        if (apiPortValue != null) {
            pi.setApiPort(apiPortValue);
        }
    }

    @Override
    public int getApiSSLPort() {
        return pi.getApiSSLPort();
    }

    public void setApiSSLPort(Integer apiSSLPortValue) {
        if (apiSSLPortValue != null) {
            pi.setApiSSLPort(apiSSLPortValue);
        }
    }

    @Override
    public Set<APIEnum> getDisabledAPIs() {
        return Collections.unmodifiableSet(disabledAPIs);
    }

    public void setDisabledAPIs(String apiSetBase64) {
        disabledAPIs = APIEnum.base64StringToEnumSet(apiSetBase64);
    }

    @Override
    public int getApiServerIdleTimeout() {
        return pi.getApiServerIdleTimeout();
    }

    public void setApiServerIdleTimeout(Integer apiServerIdleTimeout) {
        pi.setApiServerIdleTimeout(apiServerIdleTimeout);
    }


    public void setBlockchainState(Integer blockchainStateInt) {
        if (blockchainStateInt >= 0 && blockchainStateInt < BlockchainState.values().length) {
            this.blockchainState = BlockchainState.values()[blockchainStateInt];
        }
    }

    @Override
    public boolean shareAddress() {
        return pi.getShareAddress();
    }

    public void setShareAddress(boolean shareAddress) {
        pi.setShareAddress(shareAddress);
    }

    @Override
    public String getAnnouncedAddress() {
        return pi.getAnnouncedAddress();
    }

    /**
     * Sets address of peer for outbound connections
     * Should not be used directly but from PeersService service only
     *
     * @param announcedAddress address with port  optionally
     */
    @Override
    public void setAnnouncedAddress(String announcedAddress) {
        if (announcedAddress != null && announcedAddress.length() > PeersService.MAX_ANNOUNCED_ADDRESS_LENGTH) {
            throw new IllegalArgumentException("Announced address too long: " + announcedAddress.length());
        }
        PeerAddress pa = new PeerAddress(announcedAddress);
        pi.setAnnouncedAddress(pa.getAddrWithPort());
        this.port = pa.getPort();
    }


    private void setHallmark(Hallmark hallmark) {
        this.hallmark = hallmark;
        addService(Service.HALLMARK, false);
    }

    @Override
    public int getWeight() {
        if (hallmark == null) {
            return 0;
        }
        if (hallmarkBalance == -1 || hallmarkBalanceHeight < blockchain.getHeight() - 60) {
            Account account = accountService.getAccount(hallmark.getAccountId());
            hallmarkBalance = account == null ? 0 : account.getBalanceATM();
            hallmarkBalanceHeight = blockchain.getHeight();
        }
        return (int) (adjustedWeight * (hallmarkBalance / blockchainConfig.getOneAPL()) / blockchainConfig.getCurrentConfig().getMaxBalanceAPL());
    }

    @Override
    public boolean isBlacklisted() {
        return blacklistingTime > 0 || isOldVersion || peers.knownBlacklistedPeers.contains(host)
            || (pi.getAnnouncedAddress() != null && peers.knownBlacklistedPeers.contains(pi.getAnnouncedAddress()));
    }

    @Override
    public void blacklist(Exception cause) {
        deactivate("Exception: " + cause.getClass().getName() + ": " + cause.getMessage());
        if (cause instanceof AplException.NotCurrentlyValidException || cause instanceof BlockchainProcessor.BlockOutOfOrderException
            || cause instanceof SQLException || cause.getCause() instanceof SQLException) {
            // don't blacklist peers just because a feature is not yet enabled, or because of database timeouts
            // prevents erroneous blacklisting during loading of blockchain from scratch
            return;
        }
        if (cause instanceof ParseException && Errors.END_OF_FILE.equals(cause.toString())) {
            return;
        }
        if (cause instanceof ClosedChannelException) { //debug mode of jetty
            return;
        }
        if (!isBlacklisted()) {
            log.trace("Connect error, peer=" + getHostWithPort(), cause);
            if (cause instanceof IOException || cause instanceof ParseException || cause instanceof IllegalArgumentException) {
                log.debug("Blacklisting " + host + " because of: " + cause.toString());
            } else {
                log.debug("Blacklisting " + host + " because of: " + cause.toString(), cause);
            }
        }
        blacklist(cause.toString() == null || PeersService.hideErrorDetails ? cause.getClass().getName() : cause.toString());
    }

    @Override
    public void blacklist(String cause) {
        blacklistingTime = timeService.getEpochTime();
        blacklistingCause = cause;
        deactivate("Blacklisting because of: " + cause);
        peers.notifyListeners(this, PeersService.Event.BLACKLIST);
        log.debug("Peer {} blacklisted. Cause: {}", getHostWithPort(), cause);
    }

    @Override
    public void unBlacklist() {
        if (blacklistingTime == 0) {
            return;
        }
        log.debug("UnBlacklisting {}", host);
        blacklistingTime = 0;
        blacklistingCause = null;
        peers.notifyListeners(this, PeersService.Event.UNBLACKLIST);
    }

    void updateBlacklistedStatus(int curTime) {
        if (blacklistingTime > 0 && blacklistingTime + PeersService.blacklistingPeriod <= curTime) {
            unBlacklist();
        }
        if (isOldVersion && lastUpdated < curTime - 3600) {
            isOldVersion = false;
        }
    }

    @Override
    public void deactivate(String reason) {
        setState(PeerState.NON_CONNECTED);
        log.trace("Deactivating peer {}. Reason: {}", getHostWithPort(), reason);
        peers.notifyListeners(this, PeersService.Event.DEACTIVATE);
    }

    @Override
    public void remove() {
        deactivate("Remove peer");
        peers.removePeer(this);
        peers.notifyListeners(this, PeersService.Event.REMOVE);
    }

    @Override
    public boolean isInbound() {
        return p2pTransport.isInbound();
    }

    @Override
    public boolean isOutbound() {
        return p2pTransport.isOutbound();
    }

    @Override
    public long getLastActivityTime() {
        return p2pTransport.getLastActivity();
    }

    @Override
    public JSONObject send(final JSONStreamAware request, UUID chainId) throws PeerNotConnectedException {
        if (getState() != PeerState.CONNECTED) {
            String errMsg = "send() called before handshake(). Handshaking to: " + getHostWithPort();
            log.debug(errMsg);
            throw new PeerNotConnectedException(errMsg);
        } else {
            return sendJSON(request, false);
        }
    }

    @Override
    public <R> R send(BaseP2PRequest request, JsonReqRespParser<R> parser) throws PeerNotConnectedException {
        if (log.isTraceEnabled()) {
            log.trace("Try to send request={} to peer={}", request, this.getAnnouncedAddress());
        }
        checkConnectedStatus();
        try {
            JSONObject response = sendJSON(mapper.writeValueAsString(request));

            if (response == null) {
                log.debug("Response is null.");
                return null;
            }
            if (parser == null) {
                return null;
            }
            return parser.parse(response);
        } catch (JsonProcessingException e) {
            log.debug(CAN_NOT_DESERIALIZE_REQUEST_MSG);
            return null;
        }
    }

    @Override
    public void sendAsync(BaseP2PRequest request) {
        asyncExecutor.submit(() -> {
            try {
                checkConnectedStatus();
            } catch (PeerNotConnectedException e) {
                log.debug("Peer is not connected: {}", getHostWithPort());
                return;
            }
            try {
                sendJSONAsync(mapper.writeValueAsString(request));
            } catch (JsonProcessingException e) {
                log.debug(CAN_NOT_DESERIALIZE_REQUEST_MSG);
            }
        });

    }

    private void checkConnectedStatus() throws PeerNotConnectedException {
        if (getState() != PeerState.CONNECTED) {
            String errMsg = "send() called before handshake(). Handshaking to: " + getHostWithPort();
            log.debug(errMsg);
            throw new PeerNotConnectedException(errMsg);
        }
    }

    @Override
    public JSONObject send(BaseP2PRequest request) throws PeerNotConnectedException {
        return send(request, json -> json);
    }

    private JSONObject sendJSON(JSONStreamAware request, boolean async) {
        String stringRequest = requestToString(request);
        if (StringUtils.isBlank(stringRequest)) {
            return null;
        }
        if (async) {
            return sendJSONAsync(stringRequest);
        } else {
            return sendJSON(stringRequest);
        }
    }

    private String requestToString(JSONStreamAware request) {
        StringWriter wsWriter = new StringWriter(PeersService.MAX_REQUEST_SIZE);
        try {
            request.writeJSONString(wsWriter);
        } catch (IOException ex) {
            log.debug(CAN_NOT_DESERIALIZE_REQUEST_MSG);
            return null;
        }
        return wsWriter.toString();
    }

    private JSONObject sendJSON(String rq) {
        JSONObject response = null;

        try {
            String resp = p2pTransport.sendAndWaitResponse(rq);
            if (resp == null) {
                log.trace("Null response from: {}", getHostWithPort());
                return response;
            }
            response = (JSONObject) JSONValue.parseWithException(resp);
            //
            // Check for an error response
            //
            if (response != null && response.get("error") != null) {
                log.debug("Peer: {} RESPONSE = {}", getHostWithPort(), response);
                if (Errors.SEQUENCE_ERROR.equals(response.get("error"))) {
                    log.debug("Sequence error received, reconnecting to {}", host);
                    deactivate("Sequence error, need to handshake");
                } else {
                    processError(response);
                }
            }
        } catch (RuntimeException | ParseException e) {
            log.debug("Exception while sending request to " + getHostWithPort(), e);
            deactivate("Exception while sending request: " + e.getMessage());
        }
        return response;
    }


    private JSONObject sendJSONAsync(String rq) {
        JSONObject response = null;

        try {
            Long resp = p2pTransport.sendRequest(rq);
            if (resp == null) {
                log.trace("Null response from: {}", getHostWithPort());
                return response;
            }
        } catch (RuntimeException e) {
            log.debug("Exception while sending request to " + getHostWithPort(), e);
            deactivate("Exception while sending request: " + e.getMessage());
        }
        return response;
    }

    @Override
    //TODO: check this wander
    public int compareTo(Peer o) {
        if (getWeight() > o.getWeight()) {
            return -1;
        } else if (getWeight() < o.getWeight()) {
            return 1;
        }
        return getHostWithPort().compareTo(o.getHostWithPort());
    }

    /**
     * first blacklist and then forget peers that are not connectable
     * or reset counter on success
     *
     * @param failed true marks failed connect attempt, false for successful connection
     */
    private int processConnectAttempt(boolean failed) {
        if (failed) {
            failedConnectAttempts++;
            if (failedConnectAttempts >= Constants.PEER_RECONNECT_ATTMEPTS_MAX) {
                log.debug("Peer {} in not connectable, removing", getAnnouncedAddress());
                peers.removePeer(this);
            }
        } else {  //reset on success
            failedConnectAttempts = 0;
        }
        return failedConnectAttempts;
    }

    public synchronized boolean handshake() {
        UUID targetChainId = peers.blockchainConfig.getChain().getChainId();
        if (getState() == PeerState.CONNECTED) {
            log.trace("Peers {} is already connected.", getHostWithPort());
            return true;
        }
        log.trace("Start handshake  to chainId = {}...", targetChainId);
        lastConnectAttempt = timeService.getEpochTime();
        try {
            JSONObject response = sendJSON(peers.getMyPeerInfoRequest(), false);
            if (response != null) {
                log.trace("handshake Response = '{}'", response != null ? response.toJSONString() : "NULL");
                if (processError(response)) {
                    log.debug("Error response on handshake from {}", getHostWithPort());
                    return false;
                }
                // parse in new_pi
                PeerInfo newPi = mapper.convertValue(response, PeerInfo.class);

                if (!setApplication(newPi.getApplication())) {
                    log.trace("Peer: {} has different Application value '{}', removing",
                        getHostWithPort(), newPi.getApplication());
                    remove();
                    return false;
                }

                if (newPi.getChainId() == null || !targetChainId.equals(UUID.fromString(newPi.getChainId()))) {
                    log.trace("Peer: {} has different chainId: '{}', removing",
                        getHostWithPort(), newPi.getChainId());
                    remove();
                    return false;
                }
                Version peerVersion = new Version(newPi.getVersion());
                setVersion(peerVersion);
                if (isOldVersion) {
                    log.debug("PEER-Connect host{}: version: {} is too old, blacklisting", host, peerVersion);
                    blacklist("Old version: " + peerVersion.toString());
                    return false;
                }
                if (!analyzeHallmark(newPi.getHallmark())) {
                    log.debug("PEER-Connect host {}: version: {} hallmark failed, blacklisting",
                        host, peerVersion);
                    blacklist("Bad hallmark");
                    return false;
                }

                chainId.set(UUID.fromString(newPi.getChainId()));
                String servicesString = (String) response.get("services");

                long origServices = getServices();
                setServices(servicesString != null ? Long.parseUnsignedLong(servicesString) : 0);

                setApiPort(newPi.getApiPort());
                setApiSSLPort(newPi.getApiSSLPort());
                setDisabledAPIs(newPi.getDisabledAPIs());
                setBlockchainState(newPi.getBlockchainState());
                lastUpdated = lastConnectAttempt;

                setPlatform(newPi.getPlatform());
                setShareAddress(newPi.getShareAddress());
                setX509pem(newPi);

                if (!PeersService.ignorePeerAnnouncedAddress) {
                    if (newPi.getAnnouncedAddress() != null && newPi.getShareAddress()) {
                        if (!verifyAnnouncedAddress(newPi.getAnnouncedAddress())) {
                            log.debug("Connect: new announced address: {} for host: {}  not accepted", newPi.getAnnouncedAddress(), host);
                            deactivate("Bad announced address");
                            return false;
                        }
                        if (!newPi.getAnnouncedAddress().equalsIgnoreCase(pi.getAnnouncedAddress())) {
                            log.debug("peer '{}' has new announced address '{}', old is '{}'",
                                host, newPi.getAnnouncedAddress(), pi.getAnnouncedAddress());
                            peers.setAnnouncedAddress(this, newPi.getAnnouncedAddress());
                            // force checking connectivity to new announced port
                            deactivate("Announced address change");
                            return false;
                        }
                    }
                }
                setState(PeerState.CONNECTED);
                if (getServices() != origServices) {
                    peers.notifyListeners(this, PeersService.Event.CHANGED_SERVICES);
                }
                log.debug("Handshake as client is OK with peer: {} ", getHostWithPort());
                processConnectAttempt(false);
            } else {
                int t = processConnectAttempt(true);
                log.debug("Failed to connect to peer: {} ({}) this:{}", getHostWithPort(), t, System.identityHashCode(this));
                deactivate("NULL json Response on handshake");
                return false;
            }
        } catch (RuntimeException e) {
            log.debug("RuntimeException. Blacklisting {}", getHostWithPort(), e);
            processConnectAttempt(true);
            blacklist(e);
            return false;
        }
        return true;
    }

    public boolean verifyAnnouncedAddress(String newAnnouncedAddress) {
        if (newAnnouncedAddress == null || newAnnouncedAddress.isEmpty()) {
            return true;
        }
        PeerAddress pa = new PeerAddress(newAnnouncedAddress);
        int announcedPort = pa.getPort();
        if (hallmark != null && announcedPort != hallmark.getPort()) {
            log.debug("Announced port {} does not match hallmark {}, ignoring hallmark for {}",
                announcedPort, hallmark.getPort(), host
            );
            unsetHallmark();
            return false;
        }

        return true;
    }

    public boolean analyzeHallmark(final String hallmarkString) {
        if (isLightClient) {
            return true;
        }

        if (hallmarkString == null && this.hallmark == null) {
            return true;
        }

        if (this.hallmark != null && this.hallmark.getHallmarkString().equals(hallmarkString)) {
            return true;
        }

        if (hallmarkString == null) {
            unsetHallmark();
            return true;
        }

        try {

            long maxBalanceAPL = blockchainConfig.getCurrentConfig().getMaxBalanceAPL();
            Hallmark hallmarkNew = Hallmark.parseHallmark(hallmarkString, maxBalanceAPL);
            if (!hallmarkNew.isValid()) {
                log.debug("Invalid hallmark {} for {}", hallmarkString, host);
                unsetHallmark();
                return false;
            }
            if (!hallmarkNew.getHost().equals(host)) {
                InetAddress hostAddress = InetAddress.getByName(host);
                boolean validHost = false;
                for (InetAddress nextHallmark : InetAddress.getAllByName(hallmarkNew.getHost())) {
                    if (hostAddress.equals(nextHallmark)) {
                        validHost = true;
                        break;
                    }
                }
                if (!validHost) {
                    log.debug("Hallmark host {} doesn't match {}", hallmarkNew.getHost(), host);
                    unsetHallmark();
                    return false;
                }
            }
            setHallmark(hallmarkNew);
            long accountId = AccountService.getId(hallmark.getPublicKey());
            List<PeerImpl> groupedPeers = new ArrayList<>();
            int mostRecentDate = 0;
            long totalWeight = 0;
            for (Peer p : peers.getAllConnectablePeers()) {
                PeerImpl peer = (PeerImpl) p;
                if (peer.hallmark == null) {
                    continue;
                }
                if (accountId == peer.hallmark.getAccountId()) {
                    groupedPeers.add(peer);
                    if (peer.hallmark.getDate() > mostRecentDate) {
                        mostRecentDate = peer.hallmark.getDate();
                        totalWeight = peer.getHallmarkWeight(mostRecentDate);
                    } else {
                        totalWeight += peer.getHallmarkWeight(mostRecentDate);
                    }
                }
            }

            for (PeerImpl peer : groupedPeers) {
                peer.adjustedWeight = blockchainConfig.getCurrentConfig().getMaxBalanceAPL() * peer.getHallmarkWeight(mostRecentDate) / totalWeight;
                peers.notifyListeners(peer, PeersService.Event.WEIGHT);
            }

            return true;

        } catch (UnknownHostException ignore) {
        } catch (RuntimeException e) {
            log.debug("Failed to analyze hallmark for peer " + host + ", " + e.toString(), e);
        }
        unsetHallmark();
        return false;

    }

    private int getHallmarkWeight(int date) {
        if (hallmark == null || !hallmark.isValid() || hallmark.getDate() != date) {
            return 0;
        }
        return hallmark.getWeight();
    }

    private void unsetHallmark() {
        removeService(Service.HALLMARK, false);
        this.hallmark = null;
    }

    private void addService(Service service, boolean doNotify) {
        boolean notifyListeners;
        synchronized (servicesMonitor) {
            notifyListeners = ((services & service.getCode()) == 0);
            services |= service.getCode();
        }
        if (notifyListeners && doNotify) {
            peers.notifyListeners(this, PeersService.Event.CHANGED_SERVICES);
        }
    }

    private void removeService(Service service, boolean doNotify) {
        boolean notifyListeners;
        synchronized (servicesMonitor) {
            notifyListeners = ((services & service.getCode()) != 0);
            services &= (~service.getCode());
        }
        if (notifyListeners && doNotify) {
            peers.notifyListeners(this, PeersService.Event.CHANGED_SERVICES);
        }
    }

    @Override
    public long getServices() {
        synchronized (servicesMonitor) {
            return services;
        }
    }

    @Override
    public void setServices(long services) {
        synchronized (servicesMonitor) {
            this.services = services;
        }
    }

    @Override
    public boolean providesService(Service service) {
        boolean isProvided;
        synchronized (servicesMonitor) {
            isProvided = ((services & service.getCode()) != 0);
        }
        return isProvided;
    }

    @Override
    public boolean providesServices(long services) {
        boolean isProvided;
        synchronized (servicesMonitor) {
            isProvided = (services & this.services) == services;
        }
        return isProvided;
    }

    @Override
    public boolean isOpenAPI() {
        return providesService(Peer.Service.API) || providesService(Peer.Service.API_SSL);
    }

    @Override
    public boolean isApiConnectable() {
        return isOpenAPI() && getState() == PeerState.CONNECTED
            && !Version.isOldVersion(version, Constants.MIN_PROXY_VERSION)
            && !Version.isNewVersion(version)
            && blockchainState == BlockchainState.UP_TO_DATE;
    }

    @Override
    public StringBuilder getPeerApiUri() {
        StringBuilder uri = new StringBuilder();
        if (providesService(Peer.Service.API_SSL)) {
            uri.append("https://");
        } else {
            uri.append("http://");
        }
        uri.append(host).append(":");
        if (providesService(Peer.Service.API_SSL)) {
            uri.append(pi.getApiSSLPort());
        } else {
            uri.append(pi.getApiPort());
        }
        return uri;
    }


    /**
     * process error from transport and application level
     */
    boolean processError(String message) {
        boolean res = false;
        if (StringUtils.isBlank(message)) {
            log.debug("Blank message from {}", getHostWithPort());
            deactivate("Null message");
            res = true;
        } else {
            try {
                BaseP2PResponse resp = mapper.readValue(message, BaseP2PResponse.class);
                if (resp != null && !StringUtils.isBlank(resp.error)) {
                    log.debug("Parsed error response from: {}. Error: {}", getHostWithPort(), resp.error);
                    if (Errors.BLACKLISTED.equalsIgnoreCase(resp.error)) {
                        String msg = String.format("We are blacklisted by %s, cause: %s", getHostWithPort(), resp.cause);
                        log.debug("Deactivating: {}", msg);
                        deactivate(msg);
                    } else if (Errors.MAX_INBOUND_CONNECTIONS.equalsIgnoreCase(resp.error)) {
                        deactivate(Errors.MAX_INBOUND_CONNECTIONS);
                    } else if (Errors.INVALID_ANNOUNCED_ADDRESS.equalsIgnoreCase(resp.error)) {
                        deactivate(Errors.INVALID_ANNOUNCED_ADDRESS);
                    } else if (Errors.UNSUPPORTED_PROTOCOL.equalsIgnoreCase(resp.error)) {
                        deactivate(Errors.UNSUPPORTED_PROTOCOL);
                    }
                    //check any other error to deactivate?
                    res = true;
                }
            } catch (IOException ex) {
                log.debug("This is not P2P response from {}", getHostWithPort(), ex);
            }
        }
        return res;
    }

    @Override
    public boolean processError(JSONObject message) {
        if (message != null) {
            return processError(message.toJSONString());
        } else {
            log.debug("null message from {}, deactivating", getHostWithPort());
            deactivate(host);
            return true;
        }
    }
    
//--------- X.509 certificates related methods
    @Override
    public String getX509pem() {
        return pi.getX509_cert();
    }

    public void setX509pem(PeerInfo pi) {
        String pem = pi.getX509_cert();

        if (pem == null || pem.isEmpty()) {
            return;
        }
        try {
            ExtCert xc = CertKeyPersistence.loadCertPEMFromStream(new ByteArrayInputStream(pem.getBytes()));
            if (xc == null) {
                log.debug("Error reading certificate of peer: {}", getHostWithPort());
                return;
            }

            IdValidator idValidator = identityService.getPeerIdValidator();
            //we should verify private key ownership by checking the signature of timestamp
            ByteBuffer bb = ByteBuffer.allocate(Integer.SIZE);
            bb.putInt(pi.getBlockTime());
            byte[] data = bb.array();
            byte[] signature = Hex.decode(pi.getEpochTimeSigantureHex());
            
            boolean signatureValid =  identityService.getPeerIdValidator().verifySignedData(xc.getCertificate(), data, signature);
            
            boolean timeDiffValid = abs(timeService.getEpochTime() - pi.getEpochTime()) <= MAX_TIME_DIFF;
            
            if (!timeDiffValid) {
                log.warn("Time difference exceeds max allowed value for node {}", getHostWithPort());
                return;
            }
            if (signatureValid) {
                log.debug("Ignoring self-signed certificate because timestamp signature is wrong for peer: {}" + getHostWithPort());
                return;
            }
            
            peerId = Hex.encode(xc.getActorId());
            if (xc.isSelfSigned()) {
                trustLevel = PeerTrustLevel.REGISTERED;
            } else if (idValidator.isTrusted(xc.getCertificate())) {
                trustLevel = PeerTrustLevel.TRUSTED;
                if ((xc.getAuthorityId().getActorType().getType() & ActorType.NODE_CERTIFIED_STORAGE) != 0) {
                    trustLevel = PeerTrustLevel.SYSTEM_TRUSTED;
                }
            } else {
                log.debug("Can not determine trust level of peer certificate, signed by unknown CA for peer: {}", getHostWithPort());
            }
        } catch (IOException | CertException ex) {
            log.debug("Can not read certificate of peer: {}", getHostWithPort());
        }
    }
    
    @Override
    public String getIdentity() {
        String res;
        if (peerId!=null){
            res = peerId;
        }else{
            res = getHostWithPort();
        }
        return res;
    }
    
//----------- overwitten methods of Object
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeerImpl peer = (PeerImpl) o;
        return port == peer.port &&
            Objects.equals(host, peer.host) &&
            Objects.equals(pi.getAnnouncedAddress(), peer.getAnnouncedAddress());
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, pi.getAnnouncedAddress(), port);
    }

    @Override
    public String toString() {
        return "Peer{" +
            "state=" + getState() +
            ", announcedAddress='" + pi.getAnnouncedAddress() + '\'' +
            ", services=" + services +
            ", host='" + host + '\'' +
            ", application ='" + getApplication() + '\'' +
            ", version='" + version + '\'' +
            '}';
    }

    @Override
    public String getHost() {
        return host;
    }
}
