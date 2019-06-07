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

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.api.p2p.PeerInfo;
import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.http.API;
import com.apollocurrency.aplwallet.apl.core.http.APIEnum;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.Errors;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.CountingInputReader;
import com.apollocurrency.aplwallet.apl.util.CountingInputStream;
import com.apollocurrency.aplwallet.apl.util.CountingOutputWriter;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

public final class PeerImpl implements Peer {
    private static final Logger LOG = getLogger(PeerImpl.class);
    
    private final String host;
    private final PeerWebSocket webSocket;
    private volatile PeerWebSocket inboundSocket;
    private volatile boolean useWebSocket;
    private volatile int port;
    private volatile Hallmark hallmark;
    private volatile EnumSet<APIEnum> disabledAPIs;
    private volatile Version version;
    private volatile boolean isOldVersion;
    private volatile long adjustedWeight;
    private volatile int blacklistingTime;
    private volatile String blacklistingCause;
    private volatile PeerState state;
    private volatile long downloadedVolume;
    private volatile long uploadedVolume;
    private volatile int lastUpdated;
    private volatile int lastConnectAttempt;
    private volatile int lastInboundRequest;
    private volatile long hallmarkBalance = -1;
    private volatile int hallmarkBalanceHeight;
    private volatile long services;
    private volatile BlockchainState blockchainState;
    private final AtomicReference<UUID> chainId = new AtomicReference<>();
    
    private final boolean isLightClient;
    private final BlockchainConfig blockchainConfig;
    private final Blockchain blockchain;
    private volatile EpochTime timeService;
    private final PropertiesHolder propertiesHolder;
    
    private PeerInfo pi = new PeerInfo();
    //Jackson JSON
    private final  ObjectMapper mapper = new ObjectMapper();
    
    PeerImpl(String host, 
            String announcedAddress,
            BlockchainConfig blockchainConfig,
            Blockchain blockchain,
            EpochTime timeService,
            PropertiesHolder propertiesHolder
    ) {
        //TODO: remove Json.org entirely from P2P
        mapper.registerModule(new JsonOrgModule());
        
        this.host = host;
        this.propertiesHolder=propertiesHolder;
        pi.announcedAddress = announcedAddress;
        pi.shareAddress = true;
        PeerAddress pa;
        if(announcedAddress==null || announcedAddress.isEmpty()){
            LOG.trace("got empty announcedAddress from host {}",host);
            pa= new PeerAddress(host);
        }else{
            pa = new PeerAddress(announcedAddress);
        }
        this.port = pa.getPort();
        this.state = PeerState.NON_CONNECTED;
        this.webSocket = new PeerWebSocket();
        this.useWebSocket = Peers.useWebSockets && !Peers.useProxy;
        this.disabledAPIs = EnumSet.noneOf(APIEnum.class);
        pi.apiServerIdleTimeout = API.apiServerIdleTimeout;
        this.blockchainState = BlockchainState.UP_TO_DATE;
        this.blockchainConfig=blockchainConfig;
        this.blockchain = blockchain;
        this.timeService=timeService;
        isLightClient=propertiesHolder.isLightClient();
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
        return state;
    }

    private void setState(PeerState state) {
        if (state != PeerState.CONNECTED)
            webSocket.close();
        if (this.state == state) {
            return;
        }
        if (this.state == PeerState.NON_CONNECTED) {
            this.state = state;
            Peers.notifyListeners(this, Peers.Event.ADDED_ACTIVE_PEER);
        } else if (state != PeerState.NON_CONNECTED) {
            this.state = state;
            Peers.notifyListeners(this, Peers.Event.CHANGED_ACTIVE_PEER);
        } else {
            this.state = state;
        }
    }

    @Override
    public long getDownloadedVolume() {
        return downloadedVolume;
    }

    void updateDownloadedVolume(long volume) {
        synchronized (this) {
            downloadedVolume += volume;
        }
        Peers.notifyListeners(this, Peers.Event.DOWNLOADED_VOLUME);
    }

    @Override
    public long getUploadedVolume() {
        return uploadedVolume;
    }

    void updateUploadedVolume(long volume) {
        synchronized (this) {
            uploadedVolume += volume;
        }
        Peers.notifyListeners(this, Peers.Event.UPLOADED_VOLUME);
    }

    @Override
    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        boolean versionChanged = version == null || !version.equals(this.version);
        this.version = version;
        isOldVersion = false;
        if (Constants.APPLICATION.equals(pi.application)) {
            isOldVersion = Version.isOldVersion(version, Constants.MIN_VERSION);
            if (isOldVersion) {
                if (versionChanged) {
                    LOG.debug(String.format("Blacklisting %s version %s", host, version));
                }
                blacklistingCause = "Old version: " + version;
                lastInboundRequest = 0;
                setState(PeerState.NON_CONNECTED);
                Peers.notifyListeners(this, Peers.Event.BLACKLIST);
            }
        }
        LOG.trace("VERSION - Peer - {} set version - {}", host, version);
    }

    @Override
    public String getApplication() {
        return pi.application;
    }

    public boolean setApplication(String application) {
        boolean res = true;
        if (application == null 
                || application.length() > Peers.MAX_APPLICATION_LENGTH
                || ! application.equalsIgnoreCase(Constants.APPLICATION)
           ) {
            LOG.debug("Invalid application: {} from host:{}",application,host);
            res=false;
        }        
        this.pi.application = application;
        return res;
    }

    @Override
    public String getPlatform() {
        return pi.platform;
    }

    public void setPlatform(String platform) {
        if (platform != null && platform.length() > PeerHttpServer.MAX_PLATFORM_LENGTH) {
            throw new IllegalArgumentException("Invalid platform length: " + platform.length());
        }
        pi.platform = platform;
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
        return Convert.truncate(pi.application, "?", 10, false)
                + " (" + Convert.truncate(version.toString(), "?", 10, false) + ")"
                + " @ " + Convert.truncate(pi.platform, "?", 10, false);
    }

    @Override
    public int getApiPort() {
        return pi.apiPort;
    }

    public void setApiPort(Object apiPortValue) {
        if (apiPortValue != null) {
            try {
                pi.apiPort = (Integer)apiPortValue;
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Invalid peer apiPort " + apiPortValue);
            }
        }
    }

    @Override
    public int getApiSSLPort() {
        return pi.apiSSLPort;
    }

    public void setApiSSLPort(Object apiSSLPortValue) {
        if (apiSSLPortValue != null) {
            try {
                pi.apiSSLPort = (Integer)apiSSLPortValue;
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Invalid peer apiSSLPort " + apiSSLPortValue);
            }
        }
    }

    @Override
    public Set<APIEnum> getDisabledAPIs() {
        return Collections.unmodifiableSet(disabledAPIs);
    }

    public void setDisabledAPIs(Object apiSetBase64) {
        if (apiSetBase64 instanceof String) {
            disabledAPIs = APIEnum.base64StringToEnumSet((String) apiSetBase64);
        }
    }

    @Override
    public int getApiServerIdleTimeout() {
        return pi.apiServerIdleTimeout;
    }

    @Override
    public BlockchainState getBlockchainState() {
        return blockchainState;
    }

    public void setBlockchainState(Object blockchainStateObj) {
        if (blockchainStateObj instanceof Integer) {
            int blockchainStateInt = (int)blockchainStateObj;
            if (blockchainStateInt >= 0 && blockchainStateInt < BlockchainState.values().length) {
                this.blockchainState = BlockchainState.values()[blockchainStateInt];
            }
        }
    }

    @Override
    public boolean shareAddress() {
        return pi.shareAddress;
    }

    public void setShareAddress(boolean shareAddress) {
        pi.shareAddress = shareAddress;
    }

    @Override
    public String getAnnouncedAddress() {
        return pi.announcedAddress;
    }

    void setAnnouncedAddress(String announcedAddress) throws MalformedURLException, UnknownHostException {
        if (announcedAddress != null && announcedAddress.length() > Peers.MAX_ANNOUNCED_ADDRESS_LENGTH) {
            throw new IllegalArgumentException("Announced address too long: " + announcedAddress.length());
        }
        PeerAddress pa = new PeerAddress(announcedAddress);
        pi.announcedAddress = pa.getAddrWithPort();
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
        return blacklistingTime > 0 || isOldVersion || Peers.knownBlacklistedPeers.contains(host)
                || (pi.announcedAddress != null && Peers.knownBlacklistedPeers.contains(pi.announcedAddress));
    }

    @Override
    public void blacklist(Exception cause) {
        if (cause instanceof AplException.NotCurrentlyValidException || cause instanceof BlockchainProcessor.BlockOutOfOrderException
                || cause instanceof SQLException || cause.getCause() instanceof SQLException) {
            // don't blacklist peers just because a feature is not yet enabled, or because of database timeouts
            // prevents erroneous blacklisting during loading of blockchain from scratch
            return;
        }
        if (cause instanceof ParseException && Errors.END_OF_FILE.equals(cause.toString())) {
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
        blacklist(cause.toString() == null || Peers.hideErrorDetails ? cause.getClass().getName() : cause.toString());
    }

    @Override
    public void blacklist(String cause) {
        blacklistingTime = timeService.getEpochTime();
        blacklistingCause = cause;
        setState(PeerState.NON_CONNECTED);
        lastInboundRequest = 0;
        Peers.notifyListeners(this, Peers.Event.BLACKLIST);
    }

    @Override
    public void unBlacklist() {
        if (blacklistingTime == 0 ) {
            return;
        }
        LOG.debug("Unblacklisting " + host);
        setState(PeerState.NON_CONNECTED);
        blacklistingTime = 0;
        blacklistingCause = null;
        Peers.notifyListeners(this, Peers.Event.UNBLACKLIST);
    }

    void updateBlacklistedStatus(int curTime) {
        if (blacklistingTime > 0 && blacklistingTime + Peers.blacklistingPeriod <= curTime) {
            unBlacklist();
        }
        if (isOldVersion && lastUpdated < curTime - 3600) {
            isOldVersion = false;
        }
    }

    @Override
    public void deactivate() {
        if (state == PeerState.CONNECTED) {
            setState(PeerState.DISCONNECTED);
        } else {
            setState(PeerState.NON_CONNECTED);
        }
        Peers.notifyListeners(this, Peers.Event.DEACTIVATE);
    }

    @Override
    public void remove() {
        setState(PeerState.NON_CONNECTED);
        webSocket.close();
        Peers.removePeer(this);
        Peers.notifyListeners(this, Peers.Event.REMOVE);
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
        return lastInboundRequest != 0;
    }

    public int getLastInboundRequest() {
        return lastInboundRequest;
    }

    void setLastInboundRequest(int now) {
        lastInboundRequest = now;
    }

    void setInboundWebSocket(PeerWebSocket inboundSocket) {
        this.inboundSocket = inboundSocket;
    }

    @Override
    public boolean isInboundWebSocket() {
        PeerWebSocket s;
        return ((s=inboundSocket) != null && s.isOpen());
    }

    @Override
    public boolean isOutboundWebSocket() {
        return webSocket.isOpen();
    }

    @Override
    public String getBlacklistingCause() {
        return blacklistingCause == null ? "unknown" : blacklistingCause;
    }

    @Override
    public int getLastConnectAttempt() {
        return lastConnectAttempt;
    }

    @Override
    public JSONObject send(final JSONStreamAware request, UUID chainId) {
        if(state!=PeerState.CONNECTED){
            LOG.trace("send() called before handshake(). Handshering");
            handshake(chainId);
        }
        if(state!=PeerState.CONNECTED){
            LOG.error("Peer: {}  handshake failed.", getAnnouncedAddress());
            return null;
        }else{        
            return send(request, chainId, Peers.MAX_RESPONSE_SIZE);
        }
    }
    
//    //TODO: implement
//    public HttpURLConnection connectMeHTTP(boolean useHTTPS){
//        HttpURLConnection connection = null;
//        return connection;
//    }
//    //TODO: implement
//    public boolean connectMeWS(boolean useHTTPS){
//       boolean res = false; 
//       return res; 
//    }
    

    private JSONObject send(final JSONStreamAware request, UUID targetChainId, int maxResponseSize) {
        if (LOG.isTraceEnabled()) {
            StringWriter out = new StringWriter();
            String reqAsString = null;
            try {
                request.writeJSONString(out);
                reqAsString = out.toString();
            } catch (IOException e) {
                LOG.warn("IOExeception wile writing Peer request", e);
            }
            LOG.trace("SEND() Request = '{}'\n, host='{}'", reqAsString, host);
        }
        JSONObject response = null;
        String log = "";
        boolean showLog = false;
        HttpURLConnection connection = null;
        int communicationLoggingMask = Peers.communicationLoggingMask;

        try {
            //
            // Create a new WebSocket session if we don't have one
            //
            if (useWebSocket && !webSocket.isOpen()) {
                String wsConnectString = "ws://" + host + ":" + getPort() + "/apl";
                LOG.debug("Connecting to '{}'...", wsConnectString);
                useWebSocket = webSocket.startClient(URI.create(wsConnectString));
                LOG.trace("Connected '{}'... ? = {}", wsConnectString, useWebSocket);
            }
            //
            // Send the request and process the response
            //
            if (useWebSocket) {
                //
                // Send the request using the WebSocket session
                //
                //TODO: check
                maxResponseSize=Peers.MAX_MESSAGE_SIZE;
                StringWriter wsWriter = new StringWriter(1000);
                request.writeJSONString(wsWriter);
                String wsRequest = wsWriter.toString();
                if (communicationLoggingMask != 0)
                    log = "WebSocket " + host + ": " + wsRequest;
                String wsResponse = webSocket.doPost(wsRequest);
                LOG.trace("WS Response = '{}'", (wsResponse != null && wsResponse.length() > 350 ? wsResponse.length() : wsResponse));
                updateUploadedVolume(wsRequest.length());
                if (maxResponseSize > 0) {
                    if ((communicationLoggingMask & Peers.LOGGING_MASK_200_RESPONSES) != 0) {
                        log += " >>> " + wsResponse;
                        showLog = true;
                    }
                    if (wsResponse.length() > maxResponseSize)
                        throw new AplException.AplIOException("Maximum size exceeded: " + wsResponse.length());
                    response = (JSONObject)JSONValue.parseWithException(wsResponse);
                    updateDownloadedVolume(wsResponse.length());
                }
            } else {
                //
                // Send the request using HTTP
                //
                String urlString = "http://" + getHostWithPort() + "/apl";
                URL url = new URL(urlString);
                LOG.trace("Connecting to URL = {}...", urlString);
                if (communicationLoggingMask != 0)
                    log = "\"" + url.toString() + "\": " + JSON.toString(request);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(Peers.connectTimeout);
                connection.setReadTimeout(Peers.readTimeout);
                connection.setRequestProperty("Accept-Encoding", "gzip");
                connection.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
                try (Writer writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"))) {
                    CountingOutputWriter cow = new CountingOutputWriter(writer);
                    request.writeJSONString(cow);
                    updateUploadedVolume(cow.getCount());
                }
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    if (maxResponseSize > 0) {
                        if ((communicationLoggingMask & Peers.LOGGING_MASK_200_RESPONSES) != 0) {
                            CountingInputStream cis = new CountingInputStream(connection.getInputStream(), maxResponseSize);
                            InputStream responseStream = cis;
                            if ("gzip".equals(connection.getHeaderField("Content-Encoding")))
                                responseStream = new GZIPInputStream(cis);
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            byte[] buffer = new byte[1024];
                            int numberOfBytes;
                            try (InputStream inputStream = responseStream) {
                                while ((numberOfBytes = inputStream.read(buffer, 0, buffer.length)) > 0)
                                    byteArrayOutputStream.write(buffer, 0, numberOfBytes);
                            }
                            String responseValue = byteArrayOutputStream.toString("UTF-8");
                            if (responseValue.length() > 0 && responseStream instanceof GZIPInputStream)
                                log += String.format("[length: %d, compression ratio: %.2f]",
                                              cis.getCount(), (double)cis.getCount()/(double) responseValue.length());
                            log += " >>> " + responseValue;
                            showLog = true;
                            response = (JSONObject) JSONValue.parseWithException(responseValue);
                            updateDownloadedVolume(responseValue.length());
                        } else {
                            InputStream responseStream = connection.getInputStream();
                            if ("gzip".equals(connection.getHeaderField("Content-Encoding")))
                                responseStream = new GZIPInputStream(responseStream);
                            try (Reader reader = new BufferedReader(new InputStreamReader(responseStream, "UTF-8"))) {
                                CountingInputReader cir = new CountingInputReader(reader, maxResponseSize);
                                response = (JSONObject)JSONValue.parseWithException(cir);
                                updateDownloadedVolume(cir.getCount());
                            }
                        }
                    }
                } else {
                    if ((communicationLoggingMask & Peers.LOGGING_MASK_NON200_RESPONSES) != 0) {
                        log += " >>> Peer responded with HTTP " + connection.getResponseCode() + " code!";
                        showLog = true;
                    }
                    LOG.debug("Peer " + host + " responded with HTTP " + connection.getResponseCode());
                    deactivate();
                    connection.disconnect();
                }
            }
            //
            // Check for an error response
            //
            if (response != null && response.get("error") != null) {
                LOG.error("Peer: {} RESPONSE = {}", getAnnouncedAddress(), response);
                deactivate();
                if (Errors.SEQUENCE_ERROR.equals(response.get("error")) && request != Peers.getMyPeerInfoRequest()) {
                    LOG.debug("Sequence error, reconnecting to " + host);
                    handshake(targetChainId);
                } else {
                    LOG.debug("Peer " + host + " version " + version + " returned error: " +
                            response.toJSONString() + ", request was: " + JSON.toString(request) +
                            ", disconnecting");
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        } catch (AplException.AplIOException e) {
            blacklist(e);
            if (connection != null) {
                connection.disconnect();
            }
        } catch (RuntimeException|ParseException|IOException e) {
            if (!(e instanceof UnknownHostException || e instanceof SocketTimeoutException ||
                                        e instanceof SocketException || Errors.END_OF_FILE.equals(e.getMessage()))) {
                LOG.debug(String.format("Error sending request to peer %s: %s",
                                       host, e.getMessage()!=null ? e.getMessage() : e.toString()));
            }
            if ((communicationLoggingMask & Peers.LOGGING_MASK_EXCEPTIONS) != 0) {
                log += " >>> " + e.toString();
                showLog = true;
            }
            deactivate();
            if (connection != null) {
                connection.disconnect();
            }
        }
        if (showLog) {
            LOG.info(log);
        }

        return response;
    }

    @Override
    public int compareTo(Peer o) {
        if (getWeight() > o.getWeight()) {
            return -1;
        } else if (getWeight() < o.getWeight()) {
            return 1;
        }
        return getHost().compareTo(o.getHost());
    }
    
    public URI getURI(boolean useTLS) throws URISyntaxException{
        String prefix;
        if(useTLS){
           prefix="http://"; 
        }else{
           prefix="https://";             
        }
        return new URI(prefix + pi.announcedAddress);
    }
    
    @Override   
    public void handshake(UUID targetChainId) {
        lastConnectAttempt = timeService.getEpochTime();
        try {
            if (!Peers.ignorePeerAnnouncedAddress && pi.announcedAddress != null) {
                try {
                    URI uri = getURI(false);
                    InetAddress inetAddress = InetAddress.getByName(uri.getHost());
                    if (!inetAddress.equals(InetAddress.getByName(host))) {
                        LOG.debug("Connect: announced address " + pi.announcedAddress + " now points to " + inetAddress.getHostAddress() + ", replacing peer " + host);
                        Peers.removePeer(this);
                        PeerImpl newPeer = Peers.findOrCreatePeer(inetAddress, pi.announcedAddress, true);
                        if (newPeer != null) {
                            Peers.addPeer(newPeer);
                            newPeer.handshake(targetChainId);
                        }
                        return;
                    }
                } catch (URISyntaxException | UnknownHostException e) {
                    blacklist(e);
                    return;
                }
            }
            JSONObject response = send(Peers.getMyPeerInfoRequest(), targetChainId, Peers.MAX_RESPONSE_SIZE);
            PeerInfo newPi;
            if (response != null) {
                //TODO: parse in new_pi
                newPi = mapper.convertValue(response, PeerInfo.class);
                if (newPi.errorCode != null && newPi.errorCode!=0) {
                    setState(PeerState.NON_CONNECTED);
                    LOG.debug("NULL or error response from {}",host);
                    return;
                }
                String app = (String)response.get("application");
                if(!setApplication(app)){
                    remove();
                    return;
                }

                if (newPi.chainId == null || ! targetChainId.equals(UUID.fromString(newPi.chainId))) {
                    LOG.debug("Peer: {} has different chainId: {}, removing",getHost(),newPi.chainId);
                    remove();
                    return;
                }
                Version peerVersion = new Version(newPi.version);
                setVersion(peerVersion);
                if(isOldVersion){
                    LOG.debug("PEER-Connect host{}: version: {} is too old, blacklisting",host, peerVersion);
                    blacklist("Old version: "+peerVersion.toString());
                    return;
                }
                if(!analyzeHallmark(newPi.hallmark)){
                    LOG.debug("PEER-Connect host{}: version: {} hallmark failed, blacklisting",host, peerVersion); 
                    blacklist("Old version: "+peerVersion.toString());
                    return;
                }
                
                chainId.set(UUID.fromString(newPi.chainId));     
                String servicesString = (String)response.get("services");
                long origServices = services;                
                services = (servicesString != null ? Long.parseUnsignedLong(servicesString) : 0);
                setApiPort(newPi.apiPort);
                setApiSSLPort(newPi.apiSSLPort);
                setDisabledAPIs(newPi.disabledAPIs);
                setBlockchainState(newPi.blockchainState);
                lastUpdated = lastConnectAttempt;

                setPlatform(newPi.platform);
                setShareAddress(newPi.shareAddress);
 
                if (!Peers.ignorePeerAnnouncedAddress) {
                    if (newPi.announcedAddress != null) {
                            if (!verifyAnnouncedAddress(newPi.announcedAddress)) {
                                LOG.debug("Connect: new announced address: {} for host: {}  not accepted", newPi.announcedAddress, host);
                                setState(PeerState.NON_CONNECTED);
                                return;
                            }
                            if (!newPi.announcedAddress.equalsIgnoreCase(pi.announcedAddress)) {
                                LOG.debug("Connect: peer " + host + " has new announced address " + newPi.announcedAddress + ", old is " + pi.announcedAddress);
                                int oldPort = getPort();
                                Peers.setAnnouncedAddress(this, newPi.announcedAddress);
                                if (getPort() != oldPort) {
                                    // force checking connectivity to new announced port
                                    setState(PeerState.NON_CONNECTED);
                                    return;
                                }
                            }
                    } else {
                        Peers.setAnnouncedAddress(this, host);
                    }
                }
                  setState(PeerState.CONNECTED);
                  if (services != origServices) {
                        Peers.notifyListeners(this, Peers.Event.CHANGED_SERVICES);
                  }
               } else {
                LOG.debug("Failed to connect to peer: {} ", getAnnouncedAddress());
                setState(PeerState.NON_CONNECTED);
            }
        } catch (RuntimeException e) {
            blacklist(e);
        }
    }

    public boolean verifyAnnouncedAddress(String newAnnouncedAddress) {
        if (newAnnouncedAddress == null || newAnnouncedAddress.isEmpty()) {
            return true;
        }
        try {

            PeerAddress pa = new PeerAddress(newAnnouncedAddress);
            int announcedPort = pa.getPort();
            if (hallmark != null && announcedPort != hallmark.getPort()) {
                LOG.debug("Announced port " + announcedPort + " does not match hallmark " + hallmark.getPort() + ", ignoring hallmark for " + host);
                unsetHallmark();
            }            
            InetAddress address = InetAddress.getByName(host);
            for (InetAddress inetAddress : InetAddress.getAllByName(pa.getHostName())) {
                if (inetAddress.equals(address)) {
                    return true;
                }
            }
            LOG.debug("Announced address " + newAnnouncedAddress + " does not match: " + host);
        } catch (RuntimeException|UnknownHostException e) {
            LOG.trace("Unresolved announced address: {}",newAnnouncedAddress);
            blacklist(e);
        }
        return false;
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

            Hallmark hallmark = Hallmark.parseHallmark(hallmarkString);
            if (!hallmark.isValid()) {
                LOG.debug("Invalid hallmark " + hallmarkString + " for " + host);
                unsetHallmark();
                return false;
            }
            if (!hallmark.getHost().equals(host)) {
                InetAddress hostAddress = InetAddress.getByName(host);
                boolean validHost = false;
                for (InetAddress nextHallmark : InetAddress.getAllByName(hallmark.getHost())) {
                    if (hostAddress.equals(nextHallmark)) {
                        validHost = true;
                        break;
                    }
                }
                if (!validHost) {
                    LOG.debug("Hallmark host " + hallmark.getHost() + " doesn't match " + host);
                    unsetHallmark();
                    return false;
                }
            }
            setHallmark(hallmark);
            long accountId = Account.getId(hallmark.getPublicKey());
            List<PeerImpl> groupedPeers = new ArrayList<>();
            int mostRecentDate = 0;
            long totalWeight = 0;
            for (Peer p : Peers.getAllPeers()) {
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
                Peers.notifyListeners(peer, Peers.Event.WEIGHT);
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
        synchronized (this) {
            notifyListeners = ((services & service.getCode()) == 0);
            services |= service.getCode();
        }
        if (notifyListeners && doNotify) {
            Peers.notifyListeners(this, Peers.Event.CHANGED_SERVICES);
        }
    }

    private void removeService(Service service, boolean doNotify) {
        boolean notifyListeners;
        synchronized (this) {
            notifyListeners = ((services & service.getCode()) != 0);
            services &= (~service.getCode());
        }
        if (notifyListeners && doNotify) {
            Peers.notifyListeners(this, Peers.Event.CHANGED_SERVICES);
        }
    }

    public long getServices() {
        synchronized (this) {
            return services;
        }
    }

    public void setServices(long services) {
        synchronized (this) {
            this.services = services;
        }
    }

    @Override
    public boolean providesService(Service service) {
        boolean isProvided;
        synchronized (this) {
            isProvided = ((services & service.getCode()) != 0);
        }
        return isProvided;
    }

    @Override
    public boolean providesServices(long services) {
        boolean isProvided;
        synchronized (this) {
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
        return isOpenAPI() && state == PeerState.CONNECTED
                && !Version.isOldVersion(version, Constants.MIN_PROXY_VERSION)
                && !Version.isNewVersion(version)
                && blockchainState == BlockchainState.UP_TO_DATE;
    }

    public StringBuilder getPeerApiUri() {
        StringBuilder uri = new StringBuilder();
        if (providesService(Peer.Service.API_SSL)) {
            uri.append("https://");
        } else {
            uri.append("http://");
        }
        uri.append(host).append(":");
        if (providesService(Peer.Service.API_SSL)) {
            uri.append(pi.apiSSLPort);
        } else {
            uri.append(pi.apiPort);
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
                Objects.equals(pi.announcedAddress, peer.getAnnouncedAddress());
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, pi.announcedAddress, port);
    }

    @Override
    public String toString() {
        return "Peer{" +
                "state=" + state +
                ", announcedAddress='" + pi.announcedAddress + '\'' +
                ", services=" + services +
                ", host='" + host + '\'' +
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
        pi.apiServerIdleTimeout=apiServerIdleTimeout;
    }
}
