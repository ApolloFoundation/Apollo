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

import javax.enterprise.inject.spi.CDI;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.Time;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessorImpl;
import com.apollocurrency.aplwallet.apl.util.CountingInputReader;
import com.apollocurrency.aplwallet.apl.util.CountingOutputWriter;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PeerServlet extends WebSocketServlet {
    private static final Logger LOG = LoggerFactory.getLogger(PeerServlet.class);
    private static PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get(); 
    
    abstract static class PeerRequestHandler {

        abstract JSONStreamAware processRequest(JSONObject request, Peer peer);
        abstract boolean rejectWhileDownloading();

        protected boolean isChainIdProtected() {
            return true;
        }
        private Blockchain blockchain;
        private BlockchainProcessor blockchainProcessor;
        private TransactionProcessor transactionProcessor;
        private static volatile EpochTime timeService = CDI.current().select(EpochTime.class).get();

        protected Blockchain lookupBlockchain() {
            if (blockchain == null) blockchain = CDI.current().select(BlockchainImpl.class).get();
            return blockchain;
        }

        protected BlockchainProcessor lookupBlockchainProcessor() {
            if (blockchainProcessor == null) blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
            return blockchainProcessor;
        }

        protected TransactionProcessor lookupTransactionProcessor() {
            if (transactionProcessor == null) transactionProcessor = CDI.current().select(TransactionProcessorImpl.class).get();
            return transactionProcessor;
        }
    }

    private static final Map<String,PeerRequestHandler> peerRequestHandlers;

    static {
        Map<String,PeerRequestHandler> map = new HashMap<>();
        map.put("addPeers", AddPeers.getInstance());
        map.put("getCumulativeDifficulty", GetCumulativeDifficulty.getInstance());
        map.put("getInfo", GetInfo.getInstance());
        map.put("getMilestoneBlockIds", GetMilestoneBlockIds.getInstance());
        map.put("getNextBlockIds", GetNextBlockIds.getInstance());
        map.put("getNextBlocks", GetNextBlocks.getInstance());
        map.put("getPeers", GetPeers.getInstance());
        map.put("getTransactions", GetTransactions.getInstance());
        map.put("getUnconfirmedTransactions", GetUnconfirmedTransactions.getInstance());
        map.put("processBlock", ProcessBlock.getInstance());
        map.put("processTransactions", ProcessTransactions.getInstance());
        peerRequestHandlers = Collections.unmodifiableMap(map);
    }

    static final JSONStreamAware UNSUPPORTED_REQUEST_TYPE;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.UNSUPPORTED_REQUEST_TYPE);
        UNSUPPORTED_REQUEST_TYPE = JSON.prepare(response);
    }
    private static final JSONStreamAware CONNECTION_TIMEOUT;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.CONNECTION_TIMEOUT);
        CONNECTION_TIMEOUT = JSON.prepare(response);
    }

    private static final JSONStreamAware UNSUPPORTED_PROTOCOL;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.UNSUPPORTED_PROTOCOL);
        UNSUPPORTED_PROTOCOL = JSON.prepare(response);
    }

    private static final JSONStreamAware UNKNOWN_PEER;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.UNKNOWN_PEER);
        UNKNOWN_PEER = JSON.prepare(response);
    }

    private static final JSONStreamAware SEQUENCE_ERROR;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.SEQUENCE_ERROR);
        SEQUENCE_ERROR = JSON.prepare(response);
    }
    private static final JSONStreamAware INCORRECT_CHAIN_ID;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.CHAIN_ID_ERROR);
        INCORRECT_CHAIN_ID = JSON.prepare(response);
    }

    private static final JSONStreamAware MAX_INBOUND_CONNECTIONS;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.MAX_INBOUND_CONNECTIONS);
        MAX_INBOUND_CONNECTIONS = JSON.prepare(response);
    }

    private static final JSONStreamAware DOWNLOADING;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.DOWNLOADING);
        DOWNLOADING = JSON.prepare(response);
    }

    private static final JSONStreamAware LIGHT_CLIENT;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.LIGHT_CLIENT);
        LIGHT_CLIENT = JSON.prepare(response);
    }

    private static BlockchainProcessor blockchainProcessor;
    private static volatile EpochTime timeService = CDI.current().select(EpochTime.class).get();
    protected BlockchainProcessor lookupBlockchainProcessor() {
        if (blockchainProcessor == null) blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
        return blockchainProcessor;
    }


    static JSONStreamAware error(Exception e) {
        JSONObject response = new JSONObject();
        response.put("error", Peers.hideErrorDetails ? e.getClass().getName() : e.toString());
        return response;
    }

    /**
     * Configure the WebSocket factory
     *
     * @param   factory             WebSocket factory
     */
    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(Peers.webSocketIdleTimeout);
        factory.getPolicy().setMaxBinaryMessageSize(Peers.MAX_MESSAGE_SIZE);
        factory.setCreator(new PeerSocketCreator());
    }

    /**
     * Process HTTP POST request
     *
     * @param   req                 HTTP request
     * @param   resp                HTTP response
     * @throws  ServletException    Servlet processing error
     * @throws  IOException         I/O error
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JSONStreamAware jsonResponse;
        //
        // Process the peer request
        //
        PeerImpl peer = Peers.findOrCreatePeer(req.getRemoteAddr());
        if (peer == null) {
            jsonResponse = UNKNOWN_PEER;
        } else {
            jsonResponse = process(peer, req.getReader());
        }
        //
        // Return the response
        //

        resp.setContentType("text/plain; charset=UTF-8");
        try (CountingOutputWriter writer = new CountingOutputWriter(resp.getWriter())) {
            JSON.writeJSONString(jsonResponse, writer);
            if (peer != null) {
                peer.updateUploadedVolume(writer.getCount());
            }
        } catch (RuntimeException | IOException e) {
            processException(peer, e);
            throw e;
        }
    }

    private void processException(PeerImpl peer, Exception e) {
        if (peer != null) {
            if ((Peers.communicationLoggingMask & Peers.LOGGING_MASK_EXCEPTIONS) != 0) {
                if (e instanceof RuntimeException) {
                    LOG.debug("Error sending response to peer " + peer.getHost(), e);
                } else {
                    LOG.debug(String.format("Error sending response to peer %s: %s",
                        peer.getHost(), e.getMessage() != null ? e.getMessage() : e.toString()));
                }
            }
            peer.blacklist(e);
        }
    }


    protected boolean chainIdProtected() {
        return true;
    }
    /**
     * Process WebSocket POST request
     *
     * @param   webSocket           WebSocket for the connection
     * @param   requestId           Request identifier
     * @param   request             Request message
     */
    void doPost(PeerWebSocket webSocket, long requestId, String request) {
        JSONStreamAware jsonResponse;
        //
        // Process the peer request
        //
        InetSocketAddress socketAddress = webSocket.getRemoteAddress();
        if (socketAddress == null) {
            return;
        }
        String remoteAddress = socketAddress.getHostString();
        PeerImpl peer = Peers.findOrCreatePeer(remoteAddress);
        if (peer == null) {
            jsonResponse = UNKNOWN_PEER;
        } else {
            peer.setInboundWebSocket(webSocket);
            jsonResponse = process(peer, new StringReader(request));
            if (chainIdProtected()) {

            }
        }
        //
        // Return the response
        //

        try {
            StringWriter writer = new StringWriter(1000);
            JSON.writeJSONString(jsonResponse, writer);
            String response = writer.toString();
            webSocket.sendResponse(requestId, response);
            if (peer != null) {
                peer.updateUploadedVolume(response.length());
            }
        } catch (RuntimeException | IOException e) {
            processException(peer, e);
        }
    }

    /**
     * Process the peer request
     *
     * @param   peer                Peer
     * @param   inputReader         Input reader
     * @return                      JSON response
     */
    private JSONStreamAware process(PeerImpl peer, Reader inputReader) {
        //
        // Check for blacklisted peer
        //
        if (peer.isBlacklisted()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", Errors.BLACKLISTED);
            jsonObject.put("cause", peer.getBlacklistingCause());
            return jsonObject;
        }
        Peers.addPeer(peer);
        //
        // Process the request
        //
        try (CountingInputReader cr = new CountingInputReader(inputReader, Peers.MAX_REQUEST_SIZE)) {
            JSONObject request = (JSONObject)JSONValue.parseWithException(cr);
            peer.updateDownloadedVolume(cr.getCount());
            if (request.get("protocol") == null || ((Number)request.get("protocol")).intValue() != 1) {
                LOG.debug("Unsupported protocol " + request.get("protocol"));
                return UNSUPPORTED_PROTOCOL;
            }
            PeerRequestHandler peerRequestHandler = peerRequestHandlers.get((String)request.get("requestType"));
            if (peerRequestHandler == null) {
                return UNSUPPORTED_REQUEST_TYPE;
            }
//            uncomment this to check requests from peers
            //            if (peerRequestHandler.isChainIdProtected()) {
//                UUID chainId = blockchainConfig.getChain().getChainId();
//                Object chainIdObject = request.get("chainId");
//                if (chainIdObject == null || !chainId.toString().equals((chainIdObject.toString()))) {
//                    Peers.removePeer(peer);
//                    return INCORRECT_CHAIN_ID;
//                }
//            }
            if (peer.getState() == Peer.State.DISCONNECTED) {
                peer.setState(Peer.State.CONNECTED);
            }
            if (peer.getVersion() == null && !"getInfo".equals(request.get("requestType"))) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("ERROR: Peer - {}, Request = {}", peer, request.toJSONString());
                    LOG.debug("Peer List =[{}], dumping...", Peers.getAllPeers().size());
                    Peers.getAllPeers().stream().forEach(peerHost -> LOG.debug("{}", peerHost));
                }
                return SEQUENCE_ERROR;
            }
            if (!peer.isInbound()) {
                if (Peers.hasTooManyInboundPeers()) {
                    return MAX_INBOUND_CONNECTIONS;
                }
                Peers.notifyListeners(peer, Peers.Event.ADD_INBOUND);
            }
            peer.setLastInboundRequest(timeService.getEpochTime());
            if (peerRequestHandler.rejectWhileDownloading()) {
                if (blockchainProcessor.isDownloading()) {
                    return DOWNLOADING;
                }
                if (propertiesHolder.isLightClient()) {
                    return LIGHT_CLIENT;
                }
            }
            return peerRequestHandler.processRequest(request, peer);
        } catch (RuntimeException| ParseException |IOException e) {
            LOG.debug("Error processing POST request: " + e.toString());
            peer.blacklist(e);
            return error(e);
        }
    }

    /**
     * WebSocket creator for peer connections
     */
    private class PeerSocketCreator implements WebSocketCreator  {
        /**
         * Create a peer WebSocket
         *
         * @param   req             WebSocket upgrade request
         * @param   resp            WebSocket upgrade response
         * @return                  WebSocket
         */
        @Override
        public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
            return Peers.useWebSockets ? new PeerWebSocket(PeerServlet.this) : null;
        }
    }
}
