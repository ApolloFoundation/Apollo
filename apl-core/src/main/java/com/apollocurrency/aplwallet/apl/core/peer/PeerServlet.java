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

import com.apollocurrency.aplwallet.apl.core.peer.endpoint.AddPeers;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.Errors;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.ProcessTransactions;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.ProcessBlock;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.PeerResponses;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.GetTransactions;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.GetMilestoneBlockIds;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.GetUnconfirmedTransactions;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.GetNextBlockIds;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.GetFileChunk;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.GetFileDownloadInfo;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.GetNextBlocks;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.GetInfo;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.GetCumulativeDifficulty;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.PeerRequestHandler;
import com.apollocurrency.aplwallet.apl.core.peer.endpoint.GetPeers;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class PeerServlet extends WebSocketServlet {
    private static final Logger LOG = LoggerFactory.getLogger(PeerServlet.class);
    private static PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get(); 
    private static BlockchainProcessor blockchainProcessor;
    private static volatile EpochTime timeService = CDI.current().select(EpochTime.class).get();
    
    protected BlockchainProcessor lookupBlockchainProcessor() {
        if (blockchainProcessor == null) blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
        return blockchainProcessor;
    }  
    
    static PeerRequestHandler getHandler(String rtype){
        PeerRequestHandler res = null;
        switch (rtype) {
            case "addPeers":
                res = new AddPeers();
                break;
            case "getCumulativeDifficulty":
                res = new GetCumulativeDifficulty();
                break;
            case "getInfo":
                res = new GetInfo();
                break;
            case "getMilestoneBlockIds":
                res = new GetMilestoneBlockIds();
                break;
            case "getNextBlockIds":
                res = new GetNextBlockIds();
                break;
            case "getNextBlocks":
                res = new GetNextBlocks();
                break;
            case "getPeers":
                res = new GetPeers();
                break;
            case "getTransactions":
                res = new GetTransactions();
                break;
            case "getUnconfirmedTransactions":
                res = new GetUnconfirmedTransactions();
                break;
            case "processBlock":
                res = new ProcessBlock();
                break;
            case "processTransactions":
                res = new ProcessTransactions();
                break;
            case "getFileDownloadInfo":
                res = new GetFileDownloadInfo();
                break;
            case "getFileChunk":
                res = new GetFileChunk();
                break;                
        }
        return res;
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
            jsonResponse = PeerResponses.UNKNOWN_PEER;
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
            jsonResponse = PeerResponses.UNKNOWN_PEER;
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
                return PeerResponses.UNSUPPORTED_PROTOCOL;
            }
            PeerRequestHandler peerRequestHandler = getHandler((String)request.get("requestType"));
            if (peerRequestHandler == null) {
                return PeerResponses.UNSUPPORTED_REQUEST_TYPE;
            }

            if (peer.getState() == Peer.State.DISCONNECTED) {
                peer.setState(Peer.State.CONNECTED);
            }
            if (peer.getVersion() == null && !"getInfo".equals(request.get("requestType"))) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("ERROR: Peer - {}, Request = {}", peer, request.toJSONString());
                    LOG.trace("Peer List =[{}], dumping...", Peers.getAllPeers().size());
                    Peers.getAllPeers().stream().forEach(peerHost -> LOG.trace("{}", peerHost));
                }
                return PeerResponses.SEQUENCE_ERROR;
            }
            if (!peer.isInbound()) {
                if (Peers.hasTooManyInboundPeers()) {
                    return PeerResponses.MAX_INBOUND_CONNECTIONS;
                }
                Peers.notifyListeners(peer, Peers.Event.ADD_INBOUND);
            }
            peer.setLastInboundRequest(timeService.getEpochTime());
            if (peerRequestHandler.rejectWhileDownloading()) {
                if (lookupBlockchainProcessor().isDownloading()) {
                    return PeerResponses.DOWNLOADING;
                }
                if (propertiesHolder.isLightClient()) {
                    return PeerResponses.LIGHT_CLIENT;
                }
            }
            return peerRequestHandler.processRequest(request, peer);
        } catch (RuntimeException| ParseException |IOException e) {
            LOG.debug("Error processing POST request: " + e.toString());
            peer.blacklist(e);
            return PeerResponses.error(e);
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
