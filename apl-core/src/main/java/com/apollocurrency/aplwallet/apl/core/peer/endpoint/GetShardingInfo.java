/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer.endpoint;

import com.apollocurrency.aplwallet.api.p2p.ShardInfo;
import com.apollocurrency.aplwallet.api.p2p.request.ShardingInfoRequest;
import com.apollocurrency.aplwallet.api.p2p.respons.ShardingInfoResponse;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ShardDao;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.shard.ShardNameHelper;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author alukin@gmail.com
 */
@Slf4j
public class GetShardingInfo extends PeerRequestHandler {

    private final ShardDao shardDao;
    private final BlockchainConfig blockchainConfig;
    private final PeersService peers;
    private final ShardNameHelper snh = new ShardNameHelper();
    private final UUID chainId;
    private final boolean isShardingOff;

    @Inject
    public GetShardingInfo(ShardDao shardDao, BlockchainConfig blockchainConfig, PeersService peers, PropertiesHolder propertiesHolder) {
        this.shardDao = shardDao;
        this.blockchainConfig = blockchainConfig;
        this.peers = peers;
        this.isShardingOff = propertiesHolder.getBooleanProperty("apl.noshardcreate", false);
        chainId = blockchainConfig.getChain().getChainId();
    }

    @Override
    public JSONStreamAware processRequest(JSONObject request, Peer peer) {
        ShardingInfoResponse res = new ShardingInfoResponse();
        ShardingInfoRequest rq = mapper.convertValue(request, ShardingInfoRequest.class);
        log.debug("ShardingInfoRequest = {}", rq);
        if (isShardingOff) {
            res.shardingInfo.isShardingOff = true;
        } else {
            res.shardingInfo.isShardingOff = false;
            List<Shard> allShards = shardDao.getAllCompletedShards();
            log.debug("allShards = [{}] = \n{}", allShards.size(), Arrays.toString(allShards.toArray()));
            for (Shard shard : allShards) {
                List<String> adFileIDs = new ArrayList<>();
                List<String> adFileHashes = new ArrayList<>();
                //at the moment we have one additional file with prunables
                //that could be absent
                if (shard.getPrunableZipHash() != null) {
                    String prunablesFileId = snh.getFullShardPrunId(shard.getShardId(), chainId);
                    adFileIDs.add(prunablesFileId);
                    adFileHashes.add(Convert.toHexString(shard.getPrunableZipHash()));
                }
                ShardInfo shardInfo = new ShardInfo(
                    shard.getShardId(),
                    chainId.toString() /* no chainId in db */,
                    Convert.toHexString(shard.getShardHash()),
                    Convert.toHexString(shard.getCoreZipHash()),
                    shard.getShardHeight().longValue(),
                    adFileIDs,
                    adFileHashes
                );
                res.shardingInfo.shards.add(shardInfo);
            }
        }
        log.debug("allShardInfo = [{}], rq.full? = {}", res.shardingInfo.shards.size(), rq.full);
        if (rq.full) { //add list of known peers
//            for(Peer p: PeersService.getAllPeers()){ too many peers that we can't connect
            for (Peer p : peers.getActivePeers()) {
                String address = p.getAnnouncedAddress();
                if (StringUtils.isBlank(address)) {
                    address = p.getHostWithPort();
                }
                res.shardingInfo.knownPeers.add(address);
            }
        }
        JSONObject response = mapper.convertValue(res, JSONObject.class);
        log.debug("ShardingInfoResponse = {}", response);
        return response;
    }

    @Override
    public boolean rejectWhileDownloading() {
        return false;
    }

}
