package com.apollocurrency.aplwallet.apl.core.peer.statcheck;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author al
 */
public class PeersList {

    private final List<HasHashSum> peers = new ArrayList<>();
    
    public void add(HasHashSum p){
        peers.add(p);
    }    
    public HasHashSum getPeer(int id){
        return peers.get(id);
    }
    
    /**
     * getrandom peers
     * @param number
     * @return 
     */
    public List<HasHashSum> getPeers(int number){
       List<HasHashSum> res = new ArrayList<>();
       Set<Integer> idxSet = new HashSet<>();
       while(idxSet.size()<number){
           int i = (int)Math.round(Math.random()*(peers.size()-1));
           idxSet.add(i);
       }
       idxSet.forEach((i) -> {
           res.add(peers.get(i));
        });
       return res;
    }
    
    public HasHashSum getRandomPeer(){
       int i = (int)Math.round(Math.random()*(peers.size()-1));
       HasHashSum res = peers.get(i);
       return res;
    }    
 
}
