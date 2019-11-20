/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.files.shards;

import com.apollocurrency.aplwallet.api.p2p.ShardInfo;
import com.apollocurrency.aplwallet.api.p2p.ShardingInfo;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.FileDownloadDecision;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.PeerValidityDecisionMaker;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.PeersList;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerClient;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.shard.ShardNameHelper;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.PeerFileHashSum;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;

/**
 *
 * @author alukin@gmailk.com
 */
@Slf4j
public class ShardInfoDownloader {

    private final static int ENOUGH_PEERS_FOR_SHARD_INFO = 15; //15 elements is enough for decision
    private final static int ENOUGH_PEERS_FOR_SHARD_INFO_TOTAL = 40; // question 30 peers and surrender
    private final Set<String> additionalPeers;
    // shardId:shardInfo map
    @Getter
    private final Map<Long, Set<ShardInfo>> sortedByIdShards;
    //shardId:peerId map
    @Getter
    private final Map<Long, Set<String>> shardsPeers;
    @Getter
    //shardId:decision
    private final Map<Long,FileDownloadDecision> shardsDesisons = new HashMap<>();
    //peerId:alShards map
    @Getter
    private Map<String,ShardingInfo> shardInfoByPeers;
    @Getter
    //shardId:PeerFileHashSum
    private final Map<Long,Set<PeerFileHashSum>> goodPeersMap=new HashMap<>();
    //shardId:PeerFileHashSum
    @Getter
    private final Map<Long,Set<PeerFileHashSum>> badPeersMap=new HashMap<>();

    private final PeersService peers;
    private final UUID myChainId;
       
    @Inject
    public ShardInfoDownloader( PeersService peers) {
        this.additionalPeers = ConcurrentHashMap.newKeySet();
        this.sortedByIdShards = new ConcurrentHashMap();
        this.shardsPeers = new ConcurrentHashMap();
        this.shardInfoByPeers = new ConcurrentHashMap();
        this.peers = Objects.requireNonNull(peers, "peersService is NULL");;
        this.myChainId = peers.getBlockchainConfig().getChain().getChainId();
    }
    
    /**
     * This is for unit tests only
     * @param testData 
     */
    public void setShardInfoByPeers(Map<String,ShardingInfo> testData){
        additionalPeers.clear();
        sortedByIdShards.clear();
        shardsPeers.clear();
        shardInfoByPeers.clear();
        shardsDesisons.clear();
        goodPeersMap.clear();
        badPeersMap.clear();
        shardInfoByPeers=testData;
        
    }
    
    public void processAllPeersShardingInfo(){
        //remove not-sharding peers
        shardInfoByPeers.entrySet().removeIf(
                entry -> (entry.getValue().isShardingOff==true)
        );
        shardInfoByPeers.keySet().forEach((pa) -> {
            processPeerShardingInfo(pa, shardInfoByPeers.get(pa));
        });
        log.debug("ShardingInfo requesting result {}", sortedByIdShards);
        sortedByIdShards.keySet().forEach((idx) -> {
            shardsDesisons.put(idx,checkShard(idx));
        });       
    }
    
    public boolean processPeerShardingInfo(String pa, ShardingInfo si) {
        Objects.requireNonNull(pa, "peerAddress is NULL");
        Objects.requireNonNull(si, "shardInfo is NULL");
        boolean haveShard = false;
        log.trace("shardInfo = {}", si);
        if (si != null) {
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
                        Set<ShardInfo> rs = sortedByIdShards.get(s.shardId);
                        if (rs == null) {
                            rs = new HashSet<>();
                            sortedByIdShards.putIfAbsent(s.shardId, rs);
                        }
                        rs.add(s);
                    }
                }
            }
        }
        return haveShard;
    }
    
    public ShardingInfo getShardingInfoFromPeer(String addr) {
        if (StringUtils.isBlank(addr)) {
            String error = String.format("address is EMPTY or NULL : '%s'", addr);
            log.error(error);
            throw new RuntimeException(error);
        }
        ShardingInfo res = null;
        //here we are trying to create peers and trying to connect
         Peer p = peers.findOrCreatePeer(null, addr, true);
         if(p!=null){
             if( peers.connectPeer(p)){
                PeerClient pc = new PeerClient(p);
                res = pc.getShardingInfo();
             }else{
                 log.debug("Can not connect to peer: {}",addr);
             }
         }else{
             log.debug("Can not create peer: {}",addr);
         }  
         return res;
    }
    
    private List<String> randomizeOrder(Set<String> ss){
      List<String> sl = new ArrayList<>(ss); 
      Collections.shuffle(sl);
      return sl;
    }
    
    public Map<String,ShardingInfo> getShardInfoFromPeers() {
        log.debug("Requesting ShardingInfo from Peers...");
        int counterTotal = 0;        

        Set<Peer> kp = peers.getAllConnectedPeers();
        Set<String> knownPeers = new HashSet<>();
        kp.forEach((p) -> {
                knownPeers.add(p.getHostWithPort());
        });
        log.trace("ShardInfo knownPeers {}", knownPeers);
        //get sharding info from known peers
        for (String pa : randomizeOrder(knownPeers)) {
            ShardingInfo si = getShardingInfoFromPeer(pa);
            //do not count peers that do not create shrds
            if(si != null && si.isShardingOff == false){
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
            //we need new ones only here 
            additionalPeers.removeAll(knownPeers);
            for (String pa : randomizeOrder(additionalPeers)) {
                ShardingInfo si = getShardingInfoFromPeer(pa);
                if (si != null && si.isShardingOff == false) {
                    shardInfoByPeers.put(pa, si);
                } else {
                    log.warn("No shardInfo '{}' from peerAddress = {}", si, pa);
                }
                if (shardInfoByPeers.size() > ENOUGH_PEERS_FOR_SHARD_INFO) {
                    log.debug("counter > ENOUGH_PEERS_FOR_SHARD_INFO {}", true);
                    break;
                }
                counterTotal++;
                if (counterTotal > ENOUGH_PEERS_FOR_SHARD_INFO_TOTAL) {
                    break;
                }
            }
        }
        log.debug("<< Found shardInfoByPeers [{}]", shardInfoByPeers.size());
        log.trace("<< shardInfoByPeers {}", shardInfoByPeers);
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
        Objects.requireNonNull(shardId, "shardId is NULL");
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
        goodPeersMap.putIfAbsent(shardId,pvdm.getValidPeers());
        badPeersMap.putIfAbsent(shardId,pvdm.getInvalidPeers());
        log.debug("prepareForDownloading(), res = {}, goodPeers = {}, badPeers = {}", res, goodPeersMap.get(shardId), badPeersMap.get(shardId));
        return res;
    }

    /**
     * 
     * @param shardId
     * @return 
     */
    public ShardInfo getShardInfo(Long shardId) {
        Objects.requireNonNull(shardId, "shardId is NULL");
        ShardInfo res = null;
        PeerFileHashSum pfhs = goodPeersMap.get(shardId).iterator().next();
        if(pfhs==null){
            return res;
        }
        String peerId=pfhs.getPeerId();
        ShardingInfo srdInfo = shardInfoByPeers.get(peerId);
        //in our case we have just reverted array, but it could be in any order. So we should find by iteration
        for(ShardInfo si:srdInfo.getShards()){
            if(si.shardId.longValue()==shardId.longValue()){
                res=si;
                break;
            }
        }
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
    public double getShardWeight(Long shardId) {
        Objects.requireNonNull(shardId, "shardId is NULL");
        double res = 0.0D;
        int allPeersNumber = shardInfoByPeers.size();
        Set<PeerFileHashSum> goodPeers = goodPeersMap.get(shardId);
        int goodPeersNumber = ( goodPeers == null ? 0 : goodPeers.size() );
        Set<PeerFileHashSum> badPeers = badPeersMap.get(shardId);
        int badPeersNumber = ( badPeers == null ? 0 : badPeers.size() );
        int noShardPeersNumber = allPeersNumber - (goodPeersNumber+badPeersNumber);
        res = (1.0*goodPeersNumber-1.0*badPeersNumber-1.0*noShardPeersNumber)/allPeersNumber;
        log.debug("Shard: {} good: {} bad: {} no shard: {} weight: {}",shardId,goodPeersNumber,badPeersNumber,noShardPeersNumber,res);
        if(goodPeers!=null){
          log.debug("Good peers: {}",goodPeers.stream().map(e->e.getPeerId()+" ").reduce(String::concat));
        }
        if(badPeers!=null){
          log.debug("Bad peers: {}",badPeers.stream().map(e->e.getPeerId()+" ").reduce(String::concat));            
        }
        return res;
    }
    
 /**
  * Calculates relative weight of each available shard
  * Weight is less for older shard
  * 
  * @return map of relative weight of each shard.
  */   
    public Map<Long, Double> getShardRelativeWeights() {
        Map<Long,Double> res = new HashMap<>();
        if(sortedByIdShards.isEmpty()){
            return res;
        }       
        for(Long shardId: sortedByIdShards.keySet()){
            //1.0 for last shard and then less
            Double k = 1.0*shardId/sortedByIdShards.keySet().size();
            // make 11 shard biggest for debugging
            //if(shardId==10L){
            //    k=20.0;
            //}
            Double weight = getShardWeight(shardId)*k;
            res.put(shardId,weight);
        }
        return res;
    }
}
