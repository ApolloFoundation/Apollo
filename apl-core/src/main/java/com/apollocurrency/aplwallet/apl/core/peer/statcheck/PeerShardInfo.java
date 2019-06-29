/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer.statcheck;

import com.apollocurrency.aplwallet.api.p2p.ShardInfo;
import com.apollocurrency.aplwallet.api.p2p.ShardingInfo;
import com.apollocurrency.aplwallet.apl.core.peer.PeerClient;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import java.math.BigInteger;
import java.util.UUID;

/**
 * Download File info together with PeerClient and Peer
 * @author alukin@gmail.com
 */
public class PeerShardInfo implements HasHashSum {

    private BigInteger hash;
    private final PeerClient peerClient;
    private final Long shardId;
    private UUID chainId;
    private ShardingInfo si;
    
    public PeerShardInfo(PeerClient peerClient, Long shardId, UUID chainId) {
        this.peerClient= peerClient;
        this.shardId = shardId;
        this.chainId=chainId;
        this.hash=null;
    }
    
    @Override
    public BigInteger getHash() {
        return hash;
    }

    public void setHash(BigInteger hash) {
        this.hash = hash;
    }
    
    @Override
    public String getId() {
        return peerClient.gePeer().getAnnouncedAddress();
    }
    
    @Override
    public BigInteger retreiveHash() {
       si = peerClient.getShardingInfo();
       if(si==null || si.shards==null || si.shards.isEmpty()){
          hash=null;
       }else{
          for(ShardInfo s: si.shards){
             if(chainId.equals(UUID.fromString(s.chainId)) && shardId.equals(s.shardId)){                      
              hash=new BigInteger(Convert.parseHexString(s.zipCrcHash)); 
              break;
             }
          } 
       }
       return hash;
    }

    public PeerClient getPeerClient() {
        return peerClient;
    }

    @Override
    public String toString() {
        String res = "Peer Id:"+getId()+" shardId:"+shardId+" chain:"+chainId.toString();
        return res;
    }
    
}
