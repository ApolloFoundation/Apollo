/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer.endpoint;

import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfoRequest;
import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfoResponse;
import com.apollocurrency.aplwallet.api.p2p.ShardingInfoRequest;
import com.apollocurrency.aplwallet.api.p2p.ShardingInfoResponse;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import com.apollocurrency.aplwallet.apl.core.peer.DownloadableFilesManager;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.Peers;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

/**
 *
 * @author alukin@gmail.com
 */
public class GetShardingInfo extends PeerRequestHandler{
    @Inject
    DownloadableFilesManager fm;
    
    @Override
    public JSONStreamAware processRequest(JSONObject request, Peer peer) {
        ShardingInfoResponse res = new ShardingInfoResponse();
        ShardingInfoRequest rq  = mapper.convertValue(request, ShardingInfoRequest.class);
        //TODO: get all available shards
        //for(){
        //    Shard s=new Shard();
        //    res.shardingInfo.shards.add(s)
        //}
        if(rq.full){ //add list of known peers 
            for(Peer p: Peers.getAllPeers()){
                String address = p.getAnnouncedAddress();
                if(StringUtils.isBlank(address)){
                    address=p.getHostWithPort();
                }
                res.shardingInfo.knownPeers.add(address);
            }
        }
        JSONObject response = mapper.convertValue(res, JSONObject.class);
        return response;        
    }

    @Override
    public boolean rejectWhileDownloading() {
       return false;
    }
    
}
