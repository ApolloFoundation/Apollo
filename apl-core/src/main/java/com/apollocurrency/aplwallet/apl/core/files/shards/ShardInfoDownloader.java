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
    //shardId:peerId map
    @Getter
    private final Map<Long, Set<String>> shardsPeers;
    @Getter
    //shardId:decision
    private final Map<Long,FileDownloadDecision> shardsDesisons = new HashMap<>();
    //peerId:alShards map
    @Getter
    private final Map<String,ShardingInfo> shardInfoByPeers;
    @Getter
    //shardId:PeerFileHashSum
    Map<Long,Set<PeerFileHashSum>> goodPeersMap=new HashMap<>();
    //shardId:PeerFileHashSum
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

    public void processAllPeersShardingInfo(){
        for(String pa: shardInfoByPeers.keySet()){
            processPeerShardingInfo(pa, shardInfoByPeers.get(pa));
        }
        log.debug("ShardingInfo requesting result {}", sortedShards);
        sortedShards.keySet().forEach((idx) -> {
            shardsDesisons.put(idx,checkShard(idx));
        });       
    }
    
    public boolean processPeerShardingInfo(String pa, ShardingInfo si) {
        boolean haveShard = false;
        log.trace("shardInfo = {}", si);
        if (si != null) {
            si.source = pa;
            for (ShardInfo s : si.shards) {
                if (myChainId.equals(UUID.fromString(s.chainId))) {
                    haveShard = true;
                    synchronized (this) {
                        Set<String> ps = shardsPeers.get(s.shardId);
                        if (ps == null) {
                            ps = new HashSet<>();
                            shardsPeers.put(s.shardId, ps);
                        }
                        ps.add(pa);
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
    
    public ShardingInfo getShardingInfoFromPeer(String addr){
         ShardingInfo res = null;
        //here we are trying to create peers and trying to connect
         Peer p = peers.findOrCreatePeer(null, addr, true);
         if(p!=null){
             PeerClient pc = new PeerClient(p);
             res = pc.getShardingInfo();
         }  
         return res;
    }
    
    public Map<String,ShardingInfo> getShardInfoFromPeers() {
        log.debug("Requesting ShardingInfo from Peers...");
        int counterTotal = 0;        

        Set<Peer> knownPeers = peers.getAllConnectedPeers();
        log.trace("ShardInfo knownPeers {}", knownPeers);
        //get sharding info from known peers
        for (Peer p : knownPeers) {
            String pa = p.getHostWithPort();
            ShardingInfo si = getShardingInfoFromPeer(pa);
            if(si!=null){
                shardInfoByPeers.put(pa, si);
                additionalPeers.addAll(si.knownPeers);
            }
            if (shardInfoByPeers.size() >= ENOUGH_PEERS_FOR_SHARD_INFO) {
                log.debug("counter >= ENOUGH_PEERS_FOR_SHARD_INFO {}", true);
                break;
            }
            counterTotal++;
            if (counterTotal >= ENOUGH_PEERS_FOR_SHARD_INFO_TOTAL) {
                break;
            }
        }
        //we have not enough known peers, connect to additional
        if (shardInfoByPeers.size() < ENOUGH_PEERS_FOR_SHARD_INFO) {
            Set<String> additionalPeersCopy = new HashSet<>();
            additionalPeersCopy.addAll(additionalPeers);
            //avoid modification while iterating
            for (String pa : additionalPeersCopy) {
                //here we are trying to create peers
                Peer p = peers.findOrCreatePeer(null, pa, true);
                if(p!=null) {
                    ShardingInfo si = getShardingInfoFromPeer(pa);                    
                    if (shardInfoByPeers.size() > ENOUGH_PEERS_FOR_SHARD_INFO) {
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

        return shardInfoByPeers;
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
        Set<String> shardPeers = shardsPeers.get(shardId);
        if (shardPeers.size() < 2) { //we cannot use Student's T distribution with 1 sample
            result = FileDownloadDecision.NoPeers;
            //FIRE event when shard is NOT PRESENT
            log.debug("Less then 2 peers. result = {}, Fire = {}", result, "NO_SHARD");
            return result;
        }        
        //do statistical analysys of shard's hashes
        PeersList shardPeerList = new PeersList();
        ShardNameHelper snh = new ShardNameHelper();
        for (String pa : shardPeers) {
            PeerFileHashSum psi = new PeerFileHashSum(pa, snh.getFullShardId(shardId, myChainId));
            psi.setHash(getHash(shardId, pa));
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
        res=srdInfo.getShards().get(shardId.intValue());
        return res;        
    }
    
    /**
     * Shard weight is calculated using number of peers with good shards
     * Algorithm is under development, changes are possible.
     * Aim is to assign bigger weight to shards with more good peers and less bad peers
     * @param shardId
     * @return normed weight of shard from -1.0 to 1.0. +1.0 means that all peers
     * have good shard. -1.0 means that all peers have no shard. Negative number means
     * that count of bad shards and no shards is greater then good shard count
     */
    public double getShardWeight(Long shardId){
        double res = 0.0D;
        int allPeersNumber = shardInfoByPeers.size();
        Set<PeerFileHashSum> goodPeers = goodPeersMap.get(shardId);
        int goodPeersNumber = ( goodPeers == null ? 0 : goodPeers.size() );
        Set<PeerFileHashSum> badPeers = badPeersMap.get(shardId);
        int badPeersNumber = ( badPeers == null ? 0 : badPeers.size() );
        int noShardPeersNumber = allPeersNumber - (goodPeersNumber+badPeersNumber);
        res = (1.0*goodPeersNumber-1.0*badPeersNumber-1.0*noShardPeersNumber)/allPeersNumber;
        return res;
    }
    
 /**
  * Calculates relative weight of each available shard
  * Weight is less for older shard
  * 
  * @return map of relative weight of each shard.
  */   
    public Map<Long,Double> getShardRelativeWeights(){
        Map<Long,Double> res = new HashMap<>();
        for(Long shardId: sortedShards.keySet()){
            //1.0 for last shard and then less
            Double k = 1.0*shardId/sortedShards.keySet().size()+1;
            Double weight = getShardWeight(shardId)*k;
            res.put(shardId,weight);
        }
        res.put(6L,1.0);
        return res;
    }
}
