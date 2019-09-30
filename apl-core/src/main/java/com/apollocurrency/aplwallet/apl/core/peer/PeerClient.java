/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.api.p2p.FileChunk;
import com.apollocurrency.aplwallet.api.p2p.FileChunkInfo;
import com.apollocurrency.aplwallet.api.p2p.FileChunkRequest;
import com.apollocurrency.aplwallet.api.p2p.FileChunkResponse;
import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfoRequest;
import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfoResponse;
import com.apollocurrency.aplwallet.api.p2p.ShardingInfo;
import com.apollocurrency.aplwallet.api.p2p.ShardingInfoRequest;
import com.apollocurrency.aplwallet.api.p2p.ShardingInfoResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.Vetoed;
import java.util.Objects;
import java.util.UUID;

/**
 * PeerClient represents requests of P2P subsystem
 * TODO: move P2P requests here
 * @author alukin@gmail.com
 */
@Vetoed
public class PeerClient {

    private final ObjectMapper mapper = new ObjectMapper();
    private Peer peer;
    private static final Logger log = LoggerFactory.getLogger(PeerClient.class);

    public PeerClient(Peer peer) {
        Objects.requireNonNull(peer);
        //TODO: remove Json.org entirely from P2P
        mapper.registerModule(new JsonOrgModule());        
        this.peer=peer;
    }
    
    public Peer gePeer(){
        return peer;
    }
    
    public boolean checkConnection(){
        boolean res = peer.getState() == PeerState.CONNECTED;
        return res;
    }
    
    public FileDownloadInfo getFileInfo(String entityId){
        log.debug("getFileInfo() entityId = {}", entityId);
        if(!checkConnection()){
            log.debug("Peer: {} is not connected", peer.getAnnouncedAddress());
            return null;
        }        
        FileDownloadInfoRequest rq = new FileDownloadInfoRequest();
        rq.fileId = entityId;
        rq.full = true;
        JSONObject req = mapper.convertValue(rq, JSONObject.class);
        JSONObject resp;
        try {
            resp = peer.send(req, UUID.fromString(PeersService.myPI.getChainId()));
        } catch (PeerNotConnectedException ex) {
            resp=null;
        }
        if(resp == null){
            log.debug("NULL FileInfo response from peer: {}",peer.getAnnouncedAddress());
        }else{
            log.trace("getFileInfo() resp = {}", resp.toJSONString());
        }
        FileDownloadInfoResponse res = mapper.convertValue(resp, FileDownloadInfoResponse.class);
        if(res==null){
            res=new FileDownloadInfoResponse();
            res.errorCode=-3;
            res.error="Null returned from peer";
        }
        if (res.errorCode != 0 || res.error!=null) {
            log.debug("Error: {} FileInfo response from peer: {} code: {}",res.error, res.errorCode, peer.getAnnouncedAddress());
        }
        return res.downloadInfo;
    }

    public FileChunk downloadChunk(FileChunkInfo fci) {
        log.trace("downloadChunk() fci = {}", fci);
        if(!checkConnection()){
            log.debug("Can not connect to peer: {}",peer.getAnnouncedAddress());
            return null;
        }         
       FileChunk fc;
       FileChunkRequest rq = new FileChunkRequest();
       rq.fileId=fci.fileId;
       rq.id = fci.chunkId;
       rq.offset=fci.offset;
       rq.size=fci.size;
       JSONObject req = mapper.convertValue(rq, JSONObject.class);
       JSONObject resp;
        try {
            resp = peer.send(req, UUID.fromString(PeersService.myPI.getChainId()));
        } catch (PeerNotConnectedException ex) {
            resp=null;
        }
        if(resp==null){
            log.debug("NULL FileInfo response from peer: {}",peer.getAnnouncedAddress());
            return null;
        }
       FileChunkResponse res = mapper.convertValue(resp, FileChunkResponse.class);
       if(res.errorCode==0){
            fc=res.chunk;
       }else{
           fc=null;
       }
        log.trace("downloadChunk() result = {}", fc.info);
        return fc;
    }
    
    public ShardingInfo getShardingInfo(){
        if(!checkConnection()){
            log.debug("Can not connect to peer: {}",peer.getAnnouncedAddress());
            return null;
        }         
        ShardingInfoRequest rq = new ShardingInfoRequest();
        rq.full=true;
        JSONObject req = mapper.convertValue(rq, JSONObject.class);
        JSONObject resp;
        try {
            resp = peer.send(req, UUID.fromString(PeersService.myPI.getChainId()));
        } catch (PeerNotConnectedException ex) {
            resp=null;
        }
        log.trace("shardInfo respond = {}", resp);
        if(resp==null){
            log.debug("NULL ShardInfo response from peer: {}",peer.getAnnouncedAddress());
            return null;
        }
        ShardingInfoResponse res = mapper.convertValue(resp, ShardingInfoResponse.class);
        log.trace("getShardingInfo() = {}", res);
        return res.shardingInfo;
    }     
}
