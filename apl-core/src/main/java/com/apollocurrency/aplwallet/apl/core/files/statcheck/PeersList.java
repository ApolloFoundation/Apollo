package com.apollocurrency.aplwallet.apl.core.files.statcheck;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author al
 */
@Slf4j
public class PeersList< T extends HasHashSum> {

    private final List<T> allPeers = new ArrayList<>();
    
    public void add(T p){
        allPeers.add(p);
    }
    
    public HasHashSum getPeer(int id){
        return allPeers.get(id);
    }
    
    /**
     * get random peers or all if number is bigger then count of peers
     * @param number number of peers to get
     * @return set of peers that have requested file
     */
    public Set<T> getPeersWithRequestedFile(int number){        
       Set<T> res = new HashSet<>();
       Set<Integer> idxSet = new HashSet<>();
       if(number>allPeers.size()){ //add all if we have less the required
           for(int i=0; i<allPeers.size(); i++){
               idxSet.add(i);
           }
       }else{
         while(idxSet.size()<number){ //add random peers from list
             int i = (int)Math.round(Math.random()*(allPeers.size()-1));
             idxSet.add(i);
         }
       }
       //well, now we have to get hash of entity from peers
       //but there coud be not enough of them
       //TODO: what to do if we have less? Just request bigger numbers?
       for(int idx: idxSet) {
             T p = allPeers.get(idx);
             if(p!=null && p.getHash()==null){
                 if(p.retreiveHash()!=null){
                   res.add(p);
                   
                 }else{                                         
                   log.trace("Can not get hash from {}",p.getId());
                 }
             } else{
                   res.add(p);
             }
       }
       return res;
    }
    
    public T getRandomPeer(){
       int i = (int)Math.round(Math.random()*(allPeers.size()-1));
       T res = allPeers.get(i);
       res.retreiveHash();
       return res;
    }  
    
    public Set<T> getEnoughRandomPeers(long min, double min_percent){
        Long n = Math.round(allPeers.size()*min_percent/100);
        if(n<min){
            n=min;
        }
        Set<T> is = getPeersWithRequestedFile(n.intValue());
        return is;
    }
}
