/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.api.p2p.FileChunk;
import com.apollocurrency.aplwallet.api.p2p.FileChunkInfo;
import com.apollocurrency.aplwallet.api.p2p.FileChunkRequest;
import com.apollocurrency.aplwallet.api.p2p.FileChunkResonse;
import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfoRequest;
import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfoResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import java.util.Objects;
import java.util.UUID;
import javax.enterprise.inject.Vetoed;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PeerClient represents requests of P2P subsystem
 * TODO: move P2P requests here
 * @author alukin@gmail.com
 */
@Vetoed
public class PeerClient {

    private final ObjectMapper mapper = new ObjectMapper();
    private Peer peer;
    private static final Logger LOG = LoggerFactory.getLogger(PeerClient.class);

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
        boolean res = false;
        String announcedAddress = peer.getAnnouncedAddress();
        Peer p = Peers.findOrCreatePeer(announcedAddress, true);
        if(p!=null){
            peer=p;
            res=true;
        }
        return res;
    }
    
    public FileDownloadInfo getFileInfo(String entityId){
        if(!checkConnection()){
            LOG.debug("Can not connect to peer: {}",peer.getAnnouncedAddress());            
            return null;
        }        
        FileDownloadInfoRequest rq = new FileDownloadInfoRequest();
        rq.fileId = entityId;
        rq.full = true;
        JSONObject req = mapper.convertValue(rq, JSONObject.class);
        JSONObject resp = peer.send(req, UUID.fromString(Peers.myPI.chainId));
        if(resp==null){
            LOG.debug("NULL FileInfo response from peer: {}",peer.getAnnouncedAddress());
            return null;
        }
        FileDownloadInfoResponse res = mapper.convertValue(resp, FileDownloadInfoResponse.class);
        if (res.errorCode != null || res.errorCode != 0 || res.error!=null) {
            LOG.debug("Error: {} FileInfo response from peer: {} code: {}",res.error, res.errorCode, peer.getAnnouncedAddress());
        }
        return res.downloadInfo;
    }

    FileChunk downloadChunk(FileChunkInfo fci) {
       FileChunk fc;
       FileChunkRequest rq = new FileChunkRequest();
       rq.fileId=fci.fileId;
       rq.id = fci.chunkId;
       rq.offset=fci.offset.intValue();
       rq.size=fci.size.intValue();
       JSONObject req = mapper.convertValue(rq, JSONObject.class);
       JSONObject resp = peer.send(req, UUID.fromString(Peers.myPI.chainId));
        if(resp==null){
            LOG.debug("NULL FileInfo response from peer: {}",peer.getAnnouncedAddress());
            return null;
        }
       FileChunkResonse res = mapper.convertValue(resp, FileChunkResonse.class);
       if(res.errorCode==0){
            fc=res.chunk;
       }else{
           fc=null;
       }
       return fc;
    }
    
}
