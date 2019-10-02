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

import com.apollocurrency.aplwallet.api.p2p.BaseP2PResponse;
import com.apollocurrency.aplwallet.api.p2p.PeerInfo;
import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.http.API;
import com.apollocurrency.aplwallet.apl.core.http.APIEnum;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.Errors;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import lombok.Getter;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.slf4j.LoggerFactory.getLogger;

public final class PeerImpl implements Peer {
    private static final Logger LOG = getLogger(PeerImpl.class);
    
    private final String host;
    private volatile int port;
    private volatile Hallmark hallmark;
    private volatile EnumSet<APIEnum> disabledAPIs;
    private volatile Version version;
    private volatile boolean isOldVersion;
    private volatile long adjustedWeight;
    private volatile int blacklistingTime;
    private volatile String blacklistingCause;
    private volatile PeerState state;

    private volatile int lastUpdated;
    private volatile int lastConnectAttempt;
    private volatile long hallmarkBalance = -1;
    private volatile int hallmarkBalanceHeight;
    private volatile long services;
    private final Object servicesMonitor = new Object();
    private final ReadWriteLock stateLock = new ReentrantReadWriteLock();

    private volatile BlockchainState blockchainState;
    private final AtomicReference<UUID> chainId = new AtomicReference<>();
    
    private final boolean isLightClient;
    private final BlockchainConfig blockchainConfig;
    private final Blockchain blockchain;
    private volatile TimeService timeService;
    private final PeersService peers;
    
    private final PeerInfo pi = new PeerInfo();
    //Jackson JSON
    private final  ObjectMapper mapper = new ObjectMapper();
    
    @Getter
    private final Peer2PeerTransport p2pTransport;
    @Getter
    private volatile int  failedConnectAttempts = 0;

    PeerImpl(PeerAddress addrByFact,
             PeerAddress announcedAddress,
             BlockchainConfig blockchainConfig,
             Blockchain blockchain,
             TimeService timeService,
             PeerServlet peerServlet,
             PeersService peers
    ) {
        //TODO: remove Json.org entirely from P2P
        mapper.registerModule(new JsonOrgModule());
        this.host = addrByFact.getHost();
        this.port = addrByFact.getPort();

        if(announcedAddress==null){
            LOG.trace("got empty announcedAddress from host {}",getHostWithPort());
            pi.setShareAddress(false);
        }else{
            pi.setShareAddress(true);
            pi.setAnnouncedAddress(announcedAddress.getAddrWithPort());
        }
        this.disabledAPIs = EnumSet.noneOf(APIEnum.class);
        pi.setApiServerIdleTimeout(API.apiServerIdleTimeout);
        this.blockchainState = BlockchainState.UP_TO_DATE;
        this.blockchainConfig=blockchainConfig;
        this.blockchain = blockchain;
        this.timeService=timeService;
        this.peers = peers;
        isLightClient = peers.isLightClient;
        this.p2pTransport = new Peer2PeerTransport(this, peerServlet);
        state = PeerState.NON_CONNECTED; // set this peer its' initial state
    }
    
    @Override
    public String getHost() {
        return host;
    }
    
    @Override
    public String getHostWithPort(){
      PeerAddress pa = new PeerAddress(port,host);
      return pa.getAddrWithPort();
    }
    
    @Override
    public PeerState getState() {
        PeerState res;
        Lock lock = stateLock.readLock();
        lock.lock();
        try{
            res=state;
        }finally{
            lock.unlock();
        }
        return res;
    }

    private void setState(PeerState newState) {
        // if we are even not connected and some routine say to disconnect
        // we should close all because possily we alread tried to connect and have
        // client thread running
        PeerState oldState=getState();
        Lock lock = stateLock.writeLock();
        lock.lock();
        try{
          if (newState != PeerState.CONNECTED) {
            p2pTransport.disconnect();
          }
          this.state = newState;
        }finally{
            lock.unlock();
        }
        if (newState == PeerState.CONNECTED && oldState!=PeerState.CONNECTED) {
            peers.notifyListeners(this, PeersService.Event.ADDED_ACTIVE_PEER);
        } else if (newState == PeerState.NON_CONNECTED) {
            peers.notifyListeners(this, PeersService.Event.CHANGED_ACTIVE_PEER);
        }
        //we have to change state anyway
    }

    @Override
    public long getDownloadedVolume() {
        return p2pTransport.getDownloadedVolume();
    }


    @Override
    public long getUploadedVolume() {
        return p2pTransport.getUploadedVolume();
    }

    @Override
    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        boolean versionChanged = version == null || !version.equals(this.version);
        this.version = version;
        isOldVersion = false;
        LOG.trace("setVersion to Application = {} for pi = {}", pi.getApplication(), pi);
        if (Constants.APPLICATION.equals(pi.getApplication())) {
            isOldVersion = Version.isOldVersion(version, Constants.MIN_VERSION);
            if (isOldVersion) {
                if (versionChanged) {
                    LOG.debug(String.format("Blacklisting %s version %s", host, version));
                }
                blacklistingCause = "Old version: " + version;
                setState(PeerState.NON_CONNECTED);
                peers.notifyListeners(this, PeersService.Event.BLACKLIST);
            }
        }
        LOG.trace("VERSION - Peer - {} set version - {}", host, version);
    }

    @Override
    public String getApplication() {
        return pi.getApplication();
    }

    public boolean setApplication(String application) {
        boolean res = true;
        if (application == null
                || application.length() > PeersService.MAX_APPLICATION_LENGTH
                || ! application.equalsIgnoreCase(Constants.APPLICATION)
           ) {
            LOG.debug("Invalid application value='{}' from host:{}", application, host);
            res=false;
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

    @Override
    public BlockchainState getBlockchainState() {
        return blockchainState;
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
     Shoul not be used directly but from PeersService service only
     * @param announcedAddress address with port  optionally
     */
    void setAnnouncedAddress(String announcedAddress) {
        if (announcedAddress != null && announcedAddress.length() > PeersService.MAX_ANNOUNCED_ADDRESS_LENGTH) {
            throw new IllegalArgumentException("Announced address too long: " + announcedAddress.length());
        }
        PeerAddress pa = new PeerAddress(announcedAddress);
        pi.setAnnouncedAddress(pa.getAddrWithPort());
        this.port=pa.getPort();
    }

    @Override
    public int getPort() {
           return port;
    }

    @Override
    public Hallmark getHallmark() {
        return hallmark;
    }

    @Override
    public int getWeight() {
        if (hallmark == null) {
            return 0;
        }
        if (hallmarkBalance == -1 || hallmarkBalanceHeight < blockchain.getHeight() - 60) {
            long accountId = hallmark.getAccountId();
            Account account = Account.getAccount(accountId);
            hallmarkBalance = account == null ? 0 : account.getBalanceATM();
            hallmarkBalanceHeight = blockchain.getHeight();
        }
        return (int)(adjustedWeight * (hallmarkBalance / Constants.ONE_APL) / blockchainConfig.getCurrentConfig().getMaxBalanceAPL());
    }

    @Override
    public boolean isBlacklisted() {
        return blacklistingTime > 0 || isOldVersion || peers.knownBlacklistedPeers.contains(host)
                || (pi.getAnnouncedAddress() != null && peers.knownBlacklistedPeers.contains(pi.getAnnouncedAddress()));
    }

    @Override
    public void blacklist(Exception cause) {
        deactivate("Exception: "+cause.getMessage());
        if (cause instanceof AplException.NotCurrentlyValidException || cause instanceof BlockchainProcessor.BlockOutOfOrderException
                || cause instanceof SQLException || cause.getCause() instanceof SQLException) {
            // don't blacklist peers just because a feature is not yet enabled, or because of database timeouts
            // prevents erroneous blacklisting during loading of blockchain from scratch
            return;
        }
        if (cause instanceof ParseException && Errors.END_OF_FILE.equals(cause.toString())) {
            return;
        }
        if(cause instanceof ClosedChannelException){ //debug mode of jetty
            return;
        }
        if (! isBlacklisted()) {
            LOG.trace("Connect error", cause);
            if (cause instanceof IOException || cause instanceof ParseException || cause instanceof IllegalArgumentException) {
                LOG.debug("Blacklisting " + host + " because of: " + cause.toString());
            } else {
                LOG.debug("Blacklisting " + host + " because of: " + cause.toString(), cause);
            }
        }
        blacklist(cause.toString() == null || PeersService.hideErrorDetails ? cause.getClass().getName() : cause.toString());
    }

    @Override
    public void blacklist(String cause) {
        blacklistingTime = timeService.getEpochTime();
        blacklistingCause = cause;
        deactivate("Blacklisting because of: "+cause);
        peers.notifyListeners(this, PeersService.Event.BLACKLIST);
    }

    @Override
    public void unBlacklist() {
        if (blacklistingTime == 0 ) {
            return;
        }
        LOG.debug("Unblacklisting " + host);
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
        LOG.trace("Deactivating peer {}. Reason: {}",getHostWithPort(),reason);
        peers.notifyListeners(this, PeersService.Event.DEACTIVATE);
    }

    @Override
    public void remove() {
        deactivate("Remove peer");
        peers.removePeer(this);
        peers.notifyListeners(this, PeersService.Event.REMOVE);
    }

    @Override
    public int getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(int lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public boolean isInbound() {
        return pi.getAnnouncedAddress()==null;
    }

    @Override
    public boolean isOutbound() {
        return pi.getAnnouncedAddress()!=null;
    }
    @Override
    public boolean isInboundSocket() {
        return p2pTransport.isInbound();
    }

    @Override
    public boolean isOutboundSocket() {
        return p2pTransport.isOutbound();
    }
    @Override
    public String getBlacklistingCause() {
        return blacklistingCause == null ? "unknown" : blacklistingCause;
    }

    @Override
    public int getLastConnectAttempt() {
        return lastConnectAttempt;
    }

    public long getLastActivityTime(){
        return p2pTransport.getLastActivity();
    }

    @Override
    public JSONObject send(final JSONStreamAware request, UUID chainId) throws PeerNotConnectedException{

        if(getState()!=PeerState.CONNECTED){
            LOG.debug("send() called before handshake(). Handshacking to: {}",getHostWithPort());
            throw new PeerNotConnectedException("send() called before handshake(). Handshacking");
        }else{
            return send(request);
        }
    }

    private JSONObject send(final JSONStreamAware request) {

        JSONObject response =null;
        StringWriter wsWriter = new StringWriter(PeersService.MAX_REQUEST_SIZE);
        try {
            request.writeJSONString(wsWriter);
        } catch (IOException ex) {
            LOG.debug("Can not deserialize request");
            return response;
        }

        try {
            String rq = wsWriter.toString();
            String resp = p2pTransport.sendAndWaitResponse(rq);
            if(resp==null){
                LOG.trace("Null response from: ",getHostWithPort());
                return response;
            }
            response = (JSONObject) JSONValue.parseWithException(resp);
            //
            // Check for an error response
            //
            if (response != null && response.get("error") != null) {
                LOG.debug("Peer: {} RESPONSE = {}", getHostWithPort(), response);
                if (Errors.SEQUENCE_ERROR.equals(response.get("error"))){
                    LOG.debug("Sequence error received, reconnecting to " + host);
                    deactivate("Sequence error, need to handshake");
                } else {
                    processError(response);
                }
            }
        } catch (RuntimeException|ParseException e) {
            LOG.debug("Exception while sending request to '{}'",getHostWithPort(),e);
            deactivate("Exception while sending request: "+e.getMessage());
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
     * @param failed true marks failed connect attempt, false for successful connection
     */
    private int processConnectAttempt(boolean failed){
        if(failed){
          failedConnectAttempts++;
          if(failedConnectAttempts>=Constants.PEER_RECONNECT_ATTMEPTS_MAX){
              LOG.debug("Peer {} in noit connecatable, removing",getAnnouncedAddress());
              peers.removePeer(this);
          }
        }else{  //reset on success
            failedConnectAttempts = 0;
        }
        return failedConnectAttempts;
    }
    
    @Override   
    public synchronized boolean handshake(UUID targetChainId) {
        if(getState()==PeerState.CONNECTED){
            LOG.trace("Peers {} is already connected.",getHostWithPort());
            return true;
        }
        LOG.trace("Start handshake  to chainId = {}...", targetChainId);
        lastConnectAttempt = timeService.getEpochTime();
        try {
            JSONObject response = send(peers.getMyPeerInfoRequest());
            if (response != null) {
                LOG.trace("handshake Response = '{}'", response != null ? response.toJSONString() : "NULL");
                if(processError(response)){
                    LOG.debug("Error response on handshake from {}",getHostWithPort());
                    return false;
                }
                // parse in new_pi
                PeerInfo newPi = mapper.convertValue(response, PeerInfo.class);

                if(!setApplication(newPi.getApplication())){
                    LOG.trace("Peer: {} has different Application value '{}', removing",
                            getHost(), newPi.getApplication());
                    remove();
                    return false;
                }

                if (newPi.getChainId() == null || !targetChainId.equals(UUID.fromString(newPi.getChainId()))) {
                    LOG.trace("Peer: {} has different chainId: '{}', removing",
                            getHost(), newPi.getChainId());
                    remove();
                    return false;
                }
                Version peerVersion = new Version(newPi.getVersion());
                setVersion(peerVersion);
                if(isOldVersion){
                    LOG.debug("PEER-Connect host{}: version: {} is too old, blacklisting",host, peerVersion);
                    blacklist("Old version: "+peerVersion.toString());
                    return false;
                }
                if(!analyzeHallmark(newPi.getHallmark())){
                    LOG.debug("PEER-Connect host {}: version: {} hallmark failed, blacklisting",
                            host, peerVersion);
                    blacklist("Bad hallmark");
                    return false;
                }
                
                chainId.set(UUID.fromString(newPi.getChainId()));
                String servicesString = (String)response.get("services");

                long origServices = getServices();
                setServices(servicesString != null ? Long.parseUnsignedLong(servicesString) : 0);

                setApiPort(newPi.getApiPort());
                setApiSSLPort(newPi.getApiSSLPort());
                setDisabledAPIs(newPi.getDisabledAPIs());
                setBlockchainState(newPi.getBlockchainState());
                lastUpdated = lastConnectAttempt;

                setPlatform(newPi.getPlatform());
                setShareAddress(newPi.getShareAddress());

                if (!PeersService.ignorePeerAnnouncedAddress) {
                    if (newPi.getAnnouncedAddress() != null && newPi.getShareAddress()) {
                            if (!verifyAnnouncedAddress(newPi.getAnnouncedAddress())) {
                                LOG.debug("Connect: new announced address: {} for host: {}  not accepted", newPi.getAnnouncedAddress(), host);
                                deactivate("Bad announced address");
                                return false;
                            }
                            if (!newPi.getAnnouncedAddress().equalsIgnoreCase(pi.getAnnouncedAddress())) {
                                LOG.debug("peer '{}' has new announced address '{}', old is '{}'",
                                        host, newPi.getAnnouncedAddress(), pi.getAnnouncedAddress());
                                peers.setAnnouncedAddress(this, newPi.getAnnouncedAddress());
                                // force checking connectivity to new announced port
                                deactivate("Announced address chnage");
                                return false;
                            }
                    }
                }
                setState(PeerState.CONNECTED);
                if (getServices() != origServices) {
                    peers.notifyListeners(this, PeersService.Event.CHANGED_SERVICES);
                }
                LOG.debug("Handshake as client is OK with peer: {} ", getHostWithPort());
                processConnectAttempt(false);
            } else {
                int t = processConnectAttempt(true);
                LOG.debug("Failed to connect to peer: {} ({}) this:{}", getHostWithPort(),t, System.identityHashCode(this));
                deactivate("NULL json Response on handshake");
                return false;
            }
        } catch (RuntimeException e) {
            LOG.debug("RuntimeException. Blacklisting {}",getHostWithPort(),e);
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
                LOG.debug("Announced port " + announcedPort + " does not match hallmark " + hallmark.getPort() + ", ignoring hallmark for " + host);
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

            Hallmark hallmarkNew = Hallmark.parseHallmark(hallmarkString);
            if (!hallmarkNew.isValid()) {
                LOG.debug("Invalid hallmark " + hallmarkString + " for " + host);
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
                    LOG.debug("Hallmark host " + hallmarkNew.getHost() + " doesn't match " + host);
                    unsetHallmark();
                    return false;
                }
            }
            setHallmark(hallmarkNew);
            long accountId = Account.getId(hallmark.getPublicKey());
            List<PeerImpl> groupedPeers = new ArrayList<>();
            int mostRecentDate = 0;
            long totalWeight = 0;
            for (Peer p : peers.getAllConnectablePeers()) {
                PeerImpl peer = (PeerImpl)p;
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
            LOG.debug("Failed to analyze hallmark for peer " + host + ", " + e.toString(), e);
        }
        unsetHallmark();
        return false;

    }

    private int getHallmarkWeight(int date) {
        if (hallmark == null || ! hallmark.isValid() || hallmark.getDate() != date) {
            return 0;
        }
        return hallmark.getWeight();
    }

    private void unsetHallmark() {
        removeService(Service.HALLMARK, false);
        this.hallmark = null;
    }

    private void setHallmark(Hallmark hallmark) {
        this.hallmark = hallmark;
        addService(Service.HALLMARK, false);
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
    public boolean isTrusted() {
        return  getTrustLevel().getCode() > PeerTrustLevel.REGISTERED.getCode();                
    }

    @Override
    public PeerTrustLevel getTrustLevel() {
        //TODO implement using Apollo ID 
        return PeerTrustLevel.NOT_TRUSTED;    
    }

    public void setApiServerIdleTimeout(Integer apiServerIdleTimeout) {
        pi.setApiServerIdleTimeout(apiServerIdleTimeout);
    }

    /**
     * process error from transport and application level
     */
    boolean processError(String message) {
        boolean res = false;
        if (StringUtils.isBlank(message)) {
            LOG.debug("Blank message from {}", getHostWithPort());
            deactivate("Null message");
            res = true;
        } else {
            try {
                BaseP2PResponse resp = mapper.readValue(message, BaseP2PResponse.class);
                if (resp !=null && !StringUtils.isBlank(resp.error)) {
                    LOG.debug("Parsed error response from: {}. Error: {}", getHostWithPort(), resp.error);
                    if (Errors.BLACKLISTED.equalsIgnoreCase(resp.error)) {
                        String msg = String.format("We are blacklisted by %s, cause: %s", getHostWithPort(), resp.cause);
                        LOG.debug("Deactivating: "+msg);
                        deactivate(msg);
                    }else if (Errors.MAX_INBOUND_CONNECTIONS.equalsIgnoreCase(resp.error)) {
                        deactivate(Errors.MAX_INBOUND_CONNECTIONS);
                    }else if (Errors.INVALID_ANNOUNCED_ADDRESS.equalsIgnoreCase(resp.error)) {
                        deactivate(Errors.INVALID_ANNOUNCED_ADDRESS);
                    }else if (Errors.UNSUPPORTED_PROTOCOL.equalsIgnoreCase(resp.error)) {
                        deactivate(Errors.UNSUPPORTED_PROTOCOL);
                    }
                    //check any other error to deactivate?
                    res = true;
                }
            } catch (IOException ex) {
                LOG.debug("This is not P2P response from {}", getHostWithPort(), ex);
            }
        }
        return res;
    }

    boolean processError(JSONObject message) {
       if(message!=null){
          return processError(message.toJSONString());
       }else{
            LOG.debug("null message from {}, deactivating", getHostWithPort());
            deactivate(host);
            return true;
       }
    }

}
