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

    private final List<HasHashSum> allPeers = new ArrayList<>();
    
    public void add(HasHashSum p){
        allPeers.add(p);
    }
    
    public HasHashSum getPeer(int id){
        return allPeers.get(id);
    }
    
    /**
     * get random peers or all if number is bigger then count of peers
     * @param number number of peers to get
     * @return 
     */
    public Set<HasHashSum> getPeers(int number){        
       Set<HasHashSum> res = new HashSet<>();
       Set<Integer> idxSet = new HashSet<>();
       if(number>=allPeers.size()){
           res.addAll(allPeers);
       }else{
         while(idxSet.size()<number){
             int i = (int)Math.round(Math.random()*(allPeers.size()-1));
             idxSet.add(i);
         }
         idxSet.forEach((i) -> {
             HasHashSum p = allPeers.get(i);
             if(p.retreiveHash()){
               res.add(allPeers.get(i));
             }
         });
       }
       return res;
    }
    
    public HasHashSum getRandomPeer(){
       int i = (int)Math.round(Math.random()*(allPeers.size()-1));
       HasHashSum res = allPeers.get(i);
       res.retreiveHash();
       return res;
    }  
    
    public Set<HasHashSum> getEnoughRandomPeers(long min, double min_percent){
        Long n = Math.round(allPeers.size()*min_percent/100);
        if(n<min){
            n=min;
        }
        Set<HasHashSum> is = getPeers(n.intValue());
        return is;
    }
}
