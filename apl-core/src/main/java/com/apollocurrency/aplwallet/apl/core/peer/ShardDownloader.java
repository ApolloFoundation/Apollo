/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.api.p2p.ShardInfo;
import com.apollocurrency.aplwallet.api.p2p.ShardingInfo;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.ChainsConfigHolder;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.FileDownloadDecision;
import com.apollocurrency.aplwallet.apl.core.shard.ShardNameHelper;
import com.apollocurrency.aplwallet.apl.core.shard.ShardPresentData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private javax.enterprise.event.Event<ShardPresentData> presentDataEvent;
    private static final Logger log = LoggerFactory.getLogger(ShardDownloader.class);
    
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

    public FileDownloadDecision prepareAndStartDownload() {
        FileDownloadDecision res = FileDownloadDecision.NotReady;
        if(sortedShards.isEmpty()){
            getShardInfoFromPeers();
        }
        if (sortedShards.isEmpty()) {
            res = FileDownloadDecision.NoPeers;
            //FIRE event when shard is NOT PRESENT
            ShardPresentData shardPresentData = new ShardPresentData();
            presentDataEvent.select(literal(ShardPresentEventType.NO_SHARD)).fireAsync(shardPresentData); // data is ignored            
            
        } else {
            //we have some shards available on the networks, let's decide what to do
            List<Long> shardIds = new ArrayList(sortedShards.entrySet());
            Collections.sort(shardIds);
            Long lastShard=shardIds.get(shardIds.size()-1);
            ShardNameHelper snh = new ShardNameHelper();
            String fileID=snh.getShardNameByShardId(lastShard, myChainId);
            fileDownloader.setFileId(fileID);
            res = fileDownloader.prepareForDownloading();
            if(res==FileDownloadDecision.AbsOK ||res==FileDownloadDecision.OK || res==FileDownloadDecision.Risky){
                log.debug("Starting shard downlading: {}",fileID);
                fileDownloader.startDownload();
                //TODO: how to file event when file is downloaded and OK?
            }else{
                log.debug("Can not find enough peers with good shard: {}", fileID);
                //FIRE event when shard is NOT PRESENT
                ShardPresentData shardPresentData = new ShardPresentData();
                presentDataEvent.select(literal(ShardPresentEventType.NO_SHARD)).fireAsync(shardPresentData); // data is ignored                 
            }
        }
        return res;
    }
    
    private AnnotationLiteral<ShardPresentEvent> literal(ShardPresentEventType shardPresentEventType) {
        return new ShardPresentEventBinding() {
            @Override
            public ShardPresentEventType value() {
                return shardPresentEventType;
            }
        };
    }    
}
