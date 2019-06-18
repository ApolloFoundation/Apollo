/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer.endpoint;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

import com.apollocurrency.aplwallet.api.p2p.ShardInfo;
import com.apollocurrency.aplwallet.api.p2p.ShardingInfoRequest;
import com.apollocurrency.aplwallet.api.p2p.ShardingInfoResponse;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import com.apollocurrency.aplwallet.apl.core.peer.DownloadableFilesManager;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.Peers;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

/**
 *
 * @author alukin@gmail.com
 */
@Slf4j
public class GetShardingInfo extends PeerRequestHandler{
    @Inject
    DownloadableFilesManager fm;
    @Inject @Setter
    private ShardDao shardDao;

    @Override
    public JSONStreamAware processRequest(JSONObject request, Peer peer) {
        ShardingInfoResponse res = new ShardingInfoResponse();
        ShardingInfoRequest rq  = mapper.convertValue(request, ShardingInfoRequest.class);
        log.trace("ShardingInfoRequest = {}", rq);

        List<Shard> allShards = shardDao.getAllShard(); //TODO: we can thing to use only - shardDao.getLastShard()
        log.trace("allShards = [{}] = \n{}", allShards.size(), Arrays.toString( allShards.toArray() )) ;
        for (Shard shard: allShards) {
            // create shardInfo from Shard record
            ShardInfo shardInfo = new ShardInfo(
                    shard.getShardId(),
                    null /* no chainId in db */,
                    Convert.toHexString(shard.getShardHash()),
                    Convert.toHexString(shard.getZipHashCrc()),
                    null, /* no created date in db */
                    shard.getShardHeight().longValue()
            );
            res.shardingInfo.shards.add(shardInfo);
        }
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
