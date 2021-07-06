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

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ShardDao;
import com.apollocurrency.aplwallet.apl.core.files.DownloadableFilesManager;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.AddPeers;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.Errors;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.GetCumulativeDifficulty;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.GetFileChunk;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.GetFileDownloadInfo;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.GetInfo;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.GetMilestoneBlockIds;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.GetNextBlockIds;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.GetNextBlocks;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.GetPeers;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.GetShardingInfo;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.GetTransactions;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.GetUnconfirmedTransactions;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.PeerRequestHandler;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.PeerResponses;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.ProcessBlock;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.ProcessTransactions;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.QueuedThreadPool;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.io.CountingInputReader;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import javax.annotation.PreDestroy;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.ExecutorService;
@Slf4j
public final class PeerServlet extends WebSocketServlet {
    @Inject
    private PropertiesHolder propertiesHolder;
    @Inject
    private BlockchainProcessor blockchainProcessor;
    @Inject
    private volatile TimeService timeService;
    @Inject
    private ShardDao shardDao;
    @Inject
    private BlockchainConfig blockchainConfig;
    @Inject
    private DownloadableFilesManager downloadableFilesManager;
    @Inject
    private PeersService peersService;

    private ExecutorService threadPool;

    @Override
    public void init() throws ServletException {
        super.init();
        lookupComponents();
        threadPool = new QueuedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors() * 4, "PeersWebsocketThreadPool");
    }

    protected void lookupComponents() {
        if (blockchainProcessor == null)
            blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
        if (shardDao == null) shardDao = CDI.current().select(ShardDao.class).get();
        if (blockchainConfig == null) blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
        if (downloadableFilesManager == null)
            downloadableFilesManager = CDI.current().select(DownloadableFilesManager.class).get();
        if (timeService == null) timeService = CDI.current().select(TimeService.class).get();
        if (propertiesHolder == null) propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
        if (peersService == null) peersService = CDI.current().select(PeersService.class).get();
    }

    public PeerRequestHandler getHandler(String rtype) {
        if (rtype == null) {
            return null;
        }
        lookupComponents();
        PeerRequestHandler res = null;
        switch (rtype) {
            case "addPeers":
                res = CDI.current().select(AddPeers.class).get();
                break;
            case "getCumulativeDifficulty":
                res = CDI.current().select(GetCumulativeDifficulty.class).get();
                break;
            case "getInfo":
                res = CDI.current().select(GetInfo.class).get();
                break;
            case "getMilestoneBlockIds":
                res = CDI.current().select(GetMilestoneBlockIds.class).get();
                break;
            case "getNextBlockIds":
                res = CDI.current().select(GetNextBlockIds.class).get();
                break;
            case "getNextBlocks":
                res = CDI.current().select(GetNextBlocks.class).get();
                break;
            case "getPeers":
                res = CDI.current().select(GetPeers.class).get();
                break;
            case "getTransactions":
                res = CDI.current().select(GetTransactions.class).get();
                break;
            case "getUnconfirmedTransactions":
                res = CDI.current().select(GetUnconfirmedTransactions.class).get();
                break;
            case "processBlock":
                res = CDI.current().select(ProcessBlock.class).get();
                break;
            case "processTransactions":
                res = CDI.current().select(ProcessTransactions.class).get();
                break;
            case "getFileDownloadInfo":
                res = CDI.current().select(GetFileDownloadInfo.class).get();
                break;
            case "getFileChunk":
                res = CDI.current().select(GetFileChunk.class).get();
                break;
            case "getShardingInfo":
                res = CDI.current().select(GetShardingInfo.class).get();
                break;
        }
        return res;
    }

    /**
     * Configure the WebSocket factory
     *
     * @param factory WebSocket factory
     */
    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(PeersService.webSocketIdleTimeout);
        factory.getPolicy().setMaxBinaryMessageSize(PeersService.MAX_MESSAGE_SIZE);
        factory.setCreator(new PeerSocketCreator());
    }

    private void processException(Peer peer, Exception e) {
        if (peer != null) {
            //jetty misused this, ignore
            if (!(e instanceof ClosedChannelException)) {
                log.debug("Error sending response to peer " + peer.getHost(), e);
                peer.blacklist(e);
            } else {
                log.trace("Error sending response to peer " + peer.getHost(), e);
            }
        }
    }

    void doPostWebSocket(Peer2PeerTransport transport, Long requestId, String request) {
        threadPool.execute(() -> doPostWS(transport, requestId, request));
    }

    /**
     * Process WebSocket POST request
     *
     * @param transport WebSocket for the connection
     * @param requestId Request identifier
     * @param request   Request message
     */
    private void doPostWS(Peer2PeerTransport transport, Long requestId, String request) {

        lookupComponents();
        JSONStreamAware jsonResponse;
        //
        // Process the peer request
        //

        PeerImpl peer = (PeerImpl) transport.getPeer();
        if (JSON.emptyJSONString.equalsIgnoreCase(request)) {
            return;
        }
        if (peer == null) {
            jsonResponse = PeerResponses.UNKNOWN_PEER;
        } else {
            String name = Thread.currentThread().getName();
            int threadNumberStartIndex = name.lastIndexOf("-");
            if (threadNumberStartIndex != -1 && threadNumberStartIndex <= name.length() - 2) {
                String threadNumber = name.substring(threadNumberStartIndex + 1);
                Thread.currentThread().setName("P2P-WS-RequestProcessor-" + peer.getHostWithPort() + "-" + threadNumber);
            }
            if (peer.isBlacklisted()) {
                jsonResponse = PeerResponses.getBlackisted(peer.getBlacklistingCause());
            } else {
                log.trace("Process request: " + requestId);
                jsonResponse = process(peer, new StringReader(request));
            }
        }
        // Return the response
        try {
            StringWriter writer = new StringWriter(1000);
            try {
                JSON.writeJSONString(jsonResponse, writer);
            } catch (IOException ex) {
                log.debug("Almost impossible error: Can not write to StringWriter", ex);
            }
            String response = writer.toString();
            transport.send(response, requestId);
            //check if we returned error and should close inbound socket
            if (peer != null) {
                peer.processError(response);
            }
        } catch (RuntimeException e) {
            log.debug("Exception while responding to {}", transport.which(), e);
            processException(peer, e);
        }
    }

    /**
     * Process the peer request
     *
     * @param peer        Peer
     * @param inputReader Input reader
     * @return JSON response
     */
    private JSONStreamAware process(Peer peer, Reader inputReader) {
        lookupComponents();
        //
        // Process the request
        //
        try (CountingInputReader cr = new CountingInputReader(inputReader, PeersService.MAX_REQUEST_SIZE)) {
            JSONObject request = (JSONObject) JSONValue.parseWithException(cr);
            //we have to process errors here because of http requests
            if (peer.processError(request)) {
                return null;
            }
            if (request.get("protocol") == null || ((Number) request.get("protocol")).intValue() != 1) {
                if (log.isDebugEnabled()) {
                    String toJSONString = request.toJSONString();
                    if (log.isTraceEnabled()) {
                        log.trace("Unsupported protocol {} from {}\nRequest:\n{}", request.get("protocol"),
                            peer.getHostWithPort(), toJSONString);
                    } else {
                        // cut off response string in log
                        log.debug("Unsupported protocol {} from {}\nRequest: {}", request.get("protocol"), peer.getHostWithPort(),
                            toJSONString != null && toJSONString.length() > 200
                                ? toJSONString.substring(0, 200) : toJSONString);
                    }
                }
                return PeerResponses.UNSUPPORTED_PROTOCOL;
            }
            String requestType = (String) request.get("requestType");
            PeerRequestHandler peerRequestHandler = getHandler(requestType);
            if (peerRequestHandler == null) {
                log.warn("Unsupported request type {}", requestType);
                return PeerResponses.UNSUPPORTED_REQUEST_TYPE;
            }

            if (peer.getVersion() == null && !"getInfo".equals(requestType)) {
                if (log.isTraceEnabled()) {
                    log.trace("ERROR: Peer - {}, Request = {}", peer, request.toJSONString());
                    log.trace("Peer List =[{}], dumping...", peersService.getAllPeers().size());
                    peersService.getAllPeers().stream().forEach(peerHost -> log.trace("{}", peerHost));
                }
                return PeerResponses.SEQUENCE_ERROR;
            }
            if (peer.isInbound()) {
                if (peersService.hasTooManyInboundPeers()) {
                    peersService.removePeer(peer);
                    return PeerResponses.MAX_INBOUND_CONNECTIONS;
                }
                peersService.notifyListeners(peer, PeersService.Event.ADD_INBOUND);
            }
            if (peerRequestHandler.rejectWhileDownloading()) {
                if (blockchainProcessor.isDownloading()) {
                    log.debug("Request processing:{}, rejected request={} peer={}",
                        Errors.DOWNLOADING, requestType, peer.getHostWithPort());
                    return PeerResponses.DOWNLOADING;
                }
                if (propertiesHolder.isLightClient()) {
                    return PeerResponses.LIGHT_CLIENT;
                }
            }
            log.trace("Request Processing: request={} peer={}", requestType, peer.getHostWithPort());
            return peerRequestHandler.processRequest(request, peer);
        } catch (RuntimeException | ParseException | IOException e) {
            log.debug("Error processing POST request, host = '{}', error = {}", peer.getHostWithPort(), e.toString());
            if (!(e instanceof ClosedChannelException)) {
                peer.blacklist(e);
            }
            return PeerResponses.error(e);
        }
    }

    @PreDestroy
    public void shutdown() {
        threadPool.shutdown();
    }

    /**
     * WebSocket creator for peer connections
     */
    private class PeerSocketCreator implements WebSocketCreator {
        /**
         * Create a peer WebSocket
         *
         * @param req  WebSocket upgrade request
         * @param resp WebSocket upgrade response
         * @return WebSocket
         */
        @Override
        public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
            Object res = null;
            String host = req.getRemoteAddress();
            int port = req.getRemotePort();
            PeerAddress pa = new PeerAddress(port, host);
            //we use remote port to distinguish peers behind the NAT/UPnP
            //TODO: it is bad and we have to use reliable node ID to distinguish peers
            peersService.cleanupPeers(null);
            Peer peer = peersService.findOrCreatePeer(pa, null, true);
            if (peer != null) {
                log.trace("Create inbound websocket for {}", peer.getHostWithPort());
                PeerWebSocket pws = new PeerWebSocket(peer.getP2pTransport());
                peer.getP2pTransport().setInboundSocket(pws);
                res = pws;
            }
            return res;
        }
    }
}
