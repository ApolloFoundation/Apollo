/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.api.p2p.FileChunk;
import com.apollocurrency.aplwallet.api.p2p.FileChunkInfo;
import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.api.p2p.ShardingInfo;
import com.apollocurrency.aplwallet.api.p2p.request.FileChunkRequest;
import com.apollocurrency.aplwallet.api.p2p.request.FileDownloadInfoRequest;
import com.apollocurrency.aplwallet.api.p2p.request.ShardingInfoRequest;
import com.apollocurrency.aplwallet.api.p2p.respons.FileChunkResponse;
import com.apollocurrency.aplwallet.api.p2p.respons.FileDownloadInfoResponse;
import com.apollocurrency.aplwallet.api.p2p.respons.ShardingInfoResponse;
import com.apollocurrency.aplwallet.apl.core.peer.parser.FileChunkResponseParser;
import com.apollocurrency.aplwallet.apl.core.peer.parser.FileDownloadInfoResponseParser;
import com.apollocurrency.aplwallet.apl.core.peer.parser.ShardingInfoResponseParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.Vetoed;
import java.util.Objects;
import java.util.UUID;

/**
 * PeerClient represents requests of P2P subsystem
 * TODO: move P2P requests here
 *
 * @author alukin@gmail.com
 */
@Vetoed
public class PeerClient {

    private static final Logger log = LoggerFactory.getLogger(PeerClient.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final Peer peer;

    public PeerClient(Peer peer) {
        Objects.requireNonNull(peer);
        //TODO: remove Json.org entirely from P2P
        mapper.registerModule(new JsonOrgModule());
        this.peer = peer;
    }

    public Peer gePeer() {
        return peer;
    }

    public boolean checkConnection() {
        boolean res = peer.getState() == PeerState.CONNECTED;
        return res;
    }

    public FileDownloadInfo getFileInfo(String entityId) {
        log.debug("getFileInfo() entityId = {}", entityId);
        if (!checkConnection()) {
            log.debug("Peer: {} is not connected", peer.getAnnouncedAddress());
            return null;
        }
        FileDownloadInfoRequest rq = new FileDownloadInfoRequest(UUID.fromString(PeersService.myPI.getChainId()));
        rq.fileId = entityId;
        rq.full = true;
        FileDownloadInfoResponse resp;
        try {
            resp = peer.send(rq, new FileDownloadInfoResponseParser());
        } catch (PeerNotConnectedException ex) {
            resp = null;
        }

        if (resp == null) {
            log.debug("NULL FileInfo response from peer: {}", peer.getAnnouncedAddress());

            resp = new FileDownloadInfoResponse();
            resp.errorCode = -3;
            resp.error = "Null returned from peer";
        }

        if (resp.errorCode != 0 || resp.error != null) {
            log.debug("Error code: {}  peer: {} file: {} error: {}", resp.errorCode, peer.getAnnouncedAddress(), entityId, resp.error);
        }
        return resp.downloadInfo;
    }

    public FileChunk downloadChunk(FileChunkInfo fci) {
        log.trace("downloadChunk() fci = {}", fci);
        if (!checkConnection()) {
            log.debug("Can not connect to peer: {}", peer.getAnnouncedAddress());
            return null;
        }
        FileChunk fc;
        FileChunkRequest rq = new FileChunkRequest(UUID.fromString(PeersService.myPI.getChainId()));
        rq.setFileId(fci.fileId);
        rq.setId(fci.chunkId);
        rq.setOffset(fci.offset);
        rq.setSize(fci.size);

        FileChunkResponse resp;
        try {
            resp = peer.send(rq, new FileChunkResponseParser());
        } catch (PeerNotConnectedException ex) {
            log.debug("NULL FileInfo response from peer: {}", peer.getAnnouncedAddress());
            return null;
        }

        if (resp != null && resp.errorCode == 0) {
            fc = resp.chunk;
        } else {
            fc = null;
        }
        log.trace("downloadChunk() result = {}", fc == null ? "null" : fc.info.toString());
        return fc;
    }

    public ShardingInfo getShardingInfo() {
        if (!checkConnection()) {
            log.debug("Can not connect to peer: {}", peer.getAnnouncedAddress());
            return null;
        }
        ShardingInfoRequest rq = new ShardingInfoRequest(true, UUID.fromString(PeersService.myPI.getChainId()));

        ShardingInfoResponse resp = null;
        try {
            resp = peer.send(rq, new ShardingInfoResponseParser());
        } catch (PeerNotConnectedException ex) {
            log.warn("PeerNotConnectedException, {}", peer.getAnnouncedAddress());
        }

        log.trace("shardInfo respond = {}", resp);
        if (resp == null) {
            log.debug("NULL ShardInfo response from peer: {}", peer.getAnnouncedAddress());
            return null;
        }

        return resp.shardingInfo;
    }
}
