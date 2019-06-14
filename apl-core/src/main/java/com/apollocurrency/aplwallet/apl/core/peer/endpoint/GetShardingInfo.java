/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer.endpoint;

import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfoRequest;
import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfoResponse;
import com.apollocurrency.aplwallet.api.p2p.ShardingInfoRequest;
import com.apollocurrency.aplwallet.api.p2p.ShardingInfoResponse;
import com.apollocurrency.aplwallet.apl.core.peer.DownloadableFilesManager;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import javax.inject.Inject;
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
        //get available shards
        if(rq.full){ //add list of known peers 
            
        }
        JSONObject response = mapper.convertValue(res, JSONObject.class);
        return response;        
    }

    @Override
    public boolean rejectWhileDownloading() {
       return false;
    }
    
}
