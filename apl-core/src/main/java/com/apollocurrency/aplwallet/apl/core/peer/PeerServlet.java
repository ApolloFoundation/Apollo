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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetSocketAddress;

import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
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
import com.apollocurrency.aplwallet.apl.util.CountingInputReader;
import com.apollocurrency.aplwallet.apl.util.CountingOutputWriter;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import java.nio.channels.ClosedChannelException;
import java.util.UUID;
import javax.inject.Inject;
import javax.servlet.ServletException;
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
    @Inject
    private PropertiesHolder propertiesHolder;
    @Inject
    private BlockchainProcessor blockchainProcessor;
    @Inject
    private volatile EpochTime timeService;   
    @Inject
    private ShardDao shardDao;
    @Inject
    private BlockchainConfig blockchainConfig;
    @Inject
    private DownloadableFilesManager downloadableFilesManager;

    @Override
    public void init() throws ServletException {
        super.init(); 
        lookupComponents();
    }

    protected void lookupComponents() {
        if (blockchainProcessor == null) blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
        if (shardDao == null) shardDao = CDI.current().select(ShardDao.class).get();
        if (blockchainConfig == null) blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
        if (downloadableFilesManager == null) downloadableFilesManager = CDI.current().select(DownloadableFilesManager.class).get();
        if (timeService ==null) timeService = CDI.current().select(EpochTime.class).get();
        if (propertiesHolder==null) propertiesHolder = CDI.current().select(PropertiesHolder.class).get(); 
    }  
    
    public PeerRequestHandler getHandler(String rtype) {
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
        lookupComponents();
        //
        // Process the peer request
        //
        PeerAddress pa = new PeerAddress(req.getLocalPort(), req.getRemoteAddr());
        PeerImpl peer = Peers.findOrCreatePeer(pa.getAddrWithPort());
        //PeerImpl peer = Peers.findOrCreatePeer(req.getRemoteAddr());
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

//jetty misused this, ignore            
            if(!(e instanceof ClosedChannelException )){
                LOG.debug("Error sending response to peer " + peer.getHost(), e);
                peer.blacklist(e);
            }else{
                LOG.trace("Error sending response to peer " + peer.getHost(), e);
            }
        }
    }

    /**
     * Process WebSocket POST request
     *
     * @param   webSocket           WebSocket for the connection
     * @param   requestId           Request identifier
     * @param   request             Request message
     */
    void doPost(PeerWebSocket webSocket, long requestId, String request) {
        lookupComponents();
        JSONStreamAware jsonResponse;
        //
        // Process the peer request
        //

        PeerImpl peer = (PeerImpl)webSocket.getPeer();
        if(peer==null){
            jsonResponse = PeerResponses.UNKNOWN_PEER;
        } else {
            jsonResponse = process(peer, new StringReader(request));
        }
        //
        // Return the response
        //

        try {
            StringWriter writer = new StringWriter(1000);
            JSON.writeJSONString(jsonResponse, writer);
            String response = writer.toString();
            webSocket.send(response,requestId);
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
        lookupComponents();
        //
        // Check for blacklisted peer
        //
        if (peer.isBlacklisted()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", Errors.BLACKLISTED);
            jsonObject.put("cause", peer.getBlacklistingCause());
            return jsonObject;
        }
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
            peer.setLastInboundRequestTime(timeService.getEpochTime());
            if (peerRequestHandler.rejectWhileDownloading()) {
                if (blockchainProcessor.isDownloading()) {
                    return PeerResponses.DOWNLOADING;
                }
                if (propertiesHolder.isLightClient()) {
                    return PeerResponses.LIGHT_CLIENT;
                }
            }
            return peerRequestHandler.processRequest(request, peer);
        } catch (RuntimeException| ParseException |IOException e) {
            LOG.debug("Error processing POST request, host = '{}', error = {}", peer.getHostWithPort(), e.toString());
            if(! (e instanceof  ClosedChannelException) ){
              peer.blacklist(e);
            }
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
            Object res = null;
            if (Peers.useWebSockets) {
                String host = req.getRemoteAddress();
//                int port=req.getRemotePort();
//we ignore remote port to be able to connect back in case of inbound socket close
                Peer peer = Peers.findOrCreatePeer(host);
                if (peer != null) {
                    PeerWebSocket pws = new PeerWebSocket(peer, PeerServlet.this);
                    ((PeerImpl) peer).setInboundWebSocket(pws);
                    res = pws;
                }
            }
            return res;
        }
    }
}
