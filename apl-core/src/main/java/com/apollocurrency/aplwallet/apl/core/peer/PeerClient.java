/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfoRequest;
import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfoResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import org.json.simple.JSONObject;

/**
 * PeerClient represents requests of P2P subsystem
 *
 * @author alukin@gmail.com
 */
public class PeerClient {

    private final ObjectMapper mapper = new ObjectMapper();
    private Peer peer;
    

    public PeerClient(Peer peer) {
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
            return null;
        }        
        FileDownloadInfoRequest rq = new FileDownloadInfoRequest();
        rq.fileId = entityId;
        rq.full = true;
        JSONObject req = mapper.convertValue(rq, JSONObject.class);
        JSONObject resp = peer.send(req, peer.getChainId());
        FileDownloadInfoResponse res = mapper.convertValue(resp, FileDownloadInfoResponse.class);
        if (res.errorCode != null && res.errorCode != 0) {
            return null;
        }
        return res.downloadInfo;
    }
    
}
