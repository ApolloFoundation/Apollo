/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.api.p2p.ShardInfo;
import com.apollocurrency.aplwallet.api.p2p.ShardingInfo;
import com.apollocurrency.aplwallet.apl.core.chainid.ChainsConfigHolder;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.FileDownloadDecision;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;

/**
 *
 * @author alukin@gmailk.com
 */
public class ShardDownloader {

    private final static int ENOUGH_PEERS_FOR_SHARD_INFO = 20;
    private final FileDownloader fileDownloader;
    private Set<String> additionalPeers;
    private UUID myChainId;
    private Map<Long, Set<ShardInfo>> sortedShards;

    @Inject
    public ShardDownloader(FileDownloader fileDownloader, ChainsConfigHolder chainsConfig) {
        this.myChainId = chainsConfig.getActiveChain().getChainId();
        this.additionalPeers = new HashSet<>();
        this.fileDownloader = fileDownloader;
        this.sortedShards = new HashMap<>();
    }

    private boolean processPeerShardInfo(Peer p) {
        boolean haveShard = false;
        PeerClient pc = new PeerClient(p);
        ShardingInfo si = pc.getShardingInfo();
        additionalPeers.addAll(si.knownPeers);
        for (ShardInfo s : si.shards) {
            if (myChainId.equals(UUID.fromString(s.chainId))) {
                haveShard = true;
                si.source = p.getAnnouncedAddress();
                Set<ShardInfo> rs = sortedShards.get(s.shardId);
                if (rs == null) {
                    rs = new HashSet<>();
                    sortedShards.put(s.shardId, rs);
                }
                rs.add(s);
            }
        }
        return haveShard;
    }

    public Map<Long, Set<ShardInfo>> getShardInfoFromPeers() {
        int counter = 0;

        Set<Peer> knownPeers = fileDownloader.getAllAvailablePeers();
        //get sharding info from known peers
        for (Peer p : knownPeers) {
            if (processPeerShardInfo(p)) {
                counter++;
            }
            if (counter > ENOUGH_PEERS_FOR_SHARD_INFO) {
                break;
            }
        }
        //we have not enoug known peers, connect to additionals
        if (counter < ENOUGH_PEERS_FOR_SHARD_INFO) {
            for (String pa : additionalPeers) {
                Peer p = Peers.findOrCreatePeer(pa, true);
                if (processPeerShardInfo(p)) {
                    counter++;
                }
                if (counter > ENOUGH_PEERS_FOR_SHARD_INFO) {
                    break;
                }
            }
        }
        return sortedShards;
    }

    public FileDownloadDecision prepareForDownloading() {
        FileDownloadDecision res = FileDownloadDecision.NotReady;
        if(sortedShards.isEmpty()){
            getShardInfoFromPeers();
        }
        if (sortedShards.isEmpty()) {
            res = FileDownloadDecision.NoPeers;
        } else {
            //we have some shards available on the networks, let's decide what to do

        }
        return res;
    }
}
