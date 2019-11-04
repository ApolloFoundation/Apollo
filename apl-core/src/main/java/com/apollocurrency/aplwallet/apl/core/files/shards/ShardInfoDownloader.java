/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.files.shards;

import com.apollocurrency.aplwallet.api.p2p.ShardInfo;
import com.apollocurrency.aplwallet.api.p2p.ShardingInfo;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.FileDownloadDecision;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.PeerValidityDecisionMaker;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.PeersList;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerClient;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.shard.ShardNameHelper;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.PeerFileHashSum;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;

/**
 *
 * @author alukin@gmailk.com
 */
@Slf4j
public class ShardInfoDownloader {

    private final static int ENOUGH_PEERS_FOR_SHARD_INFO = 6; //6 threads is enough for downloading
    private final static int ENOUGH_PEERS_FOR_SHARD_INFO_TOTAL = 20; // question 20 peers and surrender
    private final Set<String> additionalPeers;
    // shardId:shardInfo map
    @Getter
    private final Map<Long, Set<ShardInfo>> sortedShards;
    //shardId:peer map
    @Getter
    private final Map<Long, Set<Peer>> shardsPeers;
    @Getter
    private final Map<Long,FileDownloadDecision> shardsDesisons = new HashMap<>();
    //peerId:alShards map
    @Getter
    private final Map<String,ShardingInfo> shardInfoByPeers;
    @Getter
    Map<Long,Set<PeerFileHashSum>> goodPeersMap=new HashMap<>();
    Map<Long,Set<PeerFileHashSum>> badPeersMap=new HashMap<>();


    private final PeersService peers;
    private final UUID myChainId;
       
    @Inject

    public ShardInfoDownloader(
            BlockchainConfig blockchainConfig,
            PeersService peers) {

        Objects.requireNonNull(blockchainConfig, "chainId is NULL");

        this.additionalPeers = ConcurrentHashMap.newKeySet();
        this.sortedShards = new ConcurrentHashMap();
        this.shardsPeers = new ConcurrentHashMap();
        this.shardInfoByPeers = new ConcurrentHashMap();
        this.peers = peers;
        this.myChainId = blockchainConfig.getChain().getChainId();
    }

    private boolean processPeerShardInfo(Peer p) {
        boolean haveShard = false;
        PeerClient pc = new PeerClient(p);
        ShardingInfo si = pc.getShardingInfo();
        log.trace("shardInfo = {}", si);
        if (si != null) {
            shardInfoByPeers.put(p.getHostWithPort(), si);
            si.source = p.getHostWithPort();
            additionalPeers.addAll(si.knownPeers);
            for (ShardInfo s : si.shards) {
                if (myChainId.equals(UUID.fromString(s.chainId))) {
                    haveShard = true;
                    synchronized (this) {
                        Set<Peer> ps = shardsPeers.get(s.shardId);
                        if (ps == null) {
                            ps = new HashSet<>();
                            shardsPeers.put(s.shardId, ps);
                        }
                        ps.add(p);

                        Set<ShardInfo> rs = sortedShards.get(s.shardId);
                        if (rs == null) {
                            rs = new HashSet<>();
                            sortedShards.putIfAbsent(s.shardId, rs);
                        }
                        rs.add(s);
                    }
                }
            }
        }
        return haveShard;
    }

    public Map<Long, Set<ShardInfo>> getShardInfoFromPeers() {
        log.debug("Request ShardInfo from Peers...");
        int counterWinShardInfo = 0;
        int counterTotal = 0;        
    //   FileDownloader fileDownloader = fileDownloaders.get();        

        Set<Peer> knownPeers = peers.getAllConnectedPeers();
        log.trace("ShardInfo knownPeers {}", knownPeers);
        //get sharding info from known peers
        for (Peer p : knownPeers) {
            if (processPeerShardInfo(p)) {
                counterWinShardInfo++;
            }
            if (counterWinShardInfo > ENOUGH_PEERS_FOR_SHARD_INFO) {
                log.debug("counter > ENOUGH_PEERS_FOR_SHARD_INFO {}", true);
                break;
            }
            counterTotal++;
            if (counterTotal > ENOUGH_PEERS_FOR_SHARD_INFO_TOTAL) {
                break;
            }
        }
        //we have not enough known peers, connect to additional
        if (counterWinShardInfo < ENOUGH_PEERS_FOR_SHARD_INFO) {
            Set<String> additionalPeersCopy = new HashSet<>();
            additionalPeersCopy.addAll(additionalPeers);
            //avoid modification while iterating
            for (String pa : additionalPeersCopy) {
                //here we are trying to create peers
                Peer p = peers.findOrCreatePeer(null, pa, true);
                if(p!=null) {
                    if (processPeerShardInfo(p)) {
                        counterWinShardInfo++;
                    }
                    if (counterWinShardInfo > ENOUGH_PEERS_FOR_SHARD_INFO) {
                        log.debug("counter > ENOUGH_PEERS_FOR_SHARD_INFO {}", true);
                        break;
                    }
                    counterTotal++;
                    if (counterTotal > ENOUGH_PEERS_FOR_SHARD_INFO_TOTAL) {
                        break;
                    }
                }else{
                    log.debug("Can not create peer: {}",pa);
                }
            }
        }
        log.debug("Request ShardInfo result {}", sortedShards);
        sortedShards.keySet().forEach((idx) -> {
            shardsDesisons.put(idx,checkShard(idx));
        });
        return sortedShards;
    }

    private byte[] getHash(long shardId, String peerAddr) {
        byte[] res = null;
        ShardingInfo psi = shardInfoByPeers.get(peerAddr);
        if(psi!=null){
            for(ShardInfo si: psi.shards){
                if(myChainId.equals(UUID.fromString(si.chainId))
                        && si.shardId==shardId)
                {
                   res = Convert.parseHexString(si.zipCrcHash);
                   break;
                }
            }
        }
        return res;
    }

    private FileDownloadDecision checkShard(Long shardId) {
        FileDownloadDecision result;
        Set<Peer> shardPeers = shardsPeers.get(shardId);
        if (shardPeers.size() < 2) { //we cannot use Student's T distribution with 1 sample
            result = FileDownloadDecision.NoPeers;
            //FIRE event when shard is NOT PRESENT
            log.debug("Less then 2 peers. result = {}, Fire = {}", result, "NO_SHARD");
            return result;
        }        
        //do statistical analysys of shard's hashes
        PeersList shardPeerList = new PeersList();
        ShardNameHelper snh = new ShardNameHelper();
        for (Peer p : shardPeers) {
            PeerFileHashSum psi = new PeerFileHashSum(p.getHostWithPort(), snh.getFullShardId(shardId, myChainId));
            psi.setHash(getHash(shardId, p.getHostWithPort()));
            shardPeerList.add(psi);
        }
        PeerValidityDecisionMaker pvdm = new PeerValidityDecisionMaker(shardPeerList);
        FileDownloadDecision res = pvdm.calcualteNetworkState();
        goodPeersMap.put(shardId,pvdm.getValidPeers());
        badPeersMap.put(shardId,pvdm.getInvalidPeers());
        log.debug("prepareForDownloading(), res = {}, goodPeers = {}, badPeers = {}", res, goodPeersMap.get(shardId), badPeersMap.get(shardId));
        return res;
    }
    /**
     * 
     * @param shardId
     * @return 
     */
    public ShardInfo getShardInfo(Long shardId) {
        ShardInfo res = null;
        PeerFileHashSum pfhs = goodPeersMap.get(shardId).iterator().next();
        if(pfhs==null){
            return res;
        }
        String peerId=pfhs.getPeerId();
        ShardingInfo srdInfo = shardInfoByPeers.get(peerId);
        res=srdInfo.getShards().get(0);
        return res;        
    }

}
