/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfoRequest;
import com.apollocurrency.aplwallet.api.p2p.FileInfoResponse;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import java.math.BigInteger;
import org.json.simple.JSONObject;

/**
 * PeerClient represents requests of P2P subsystem
 *
 * @author alukin@gmail.com
 */
public class PeerClient {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Peer peer;
    

    public PeerClient(Peer peer) {
        //TODO: remove Json.org entirely from P2P
        mapper.registerModule(new JsonOrgModule());        
        this.peer=peer;
    }
    
    public Peer gePeer(){
        return peer;
    }
    public BigInteger retreiveHash(String entityId) {
        FileDownloadInfoRequest rq = new FileDownloadInfoRequest();
        rq.fileId = entityId;
        rq.full = true;
        JSONObject req = mapper.convertValue(rq, JSONObject.class);
        JSONObject resp = peer.send(req, peer.getChainId());
        FileInfoResponse res = mapper.convertValue(resp, FileInfoResponse.class);
        if (res.errorCode != null && res.errorCode != 0) {
            return null;
        }
        byte[] hash = Convert.parseHexString(res.fileInfo.hash);
        return new BigInteger(hash);
    }
}
