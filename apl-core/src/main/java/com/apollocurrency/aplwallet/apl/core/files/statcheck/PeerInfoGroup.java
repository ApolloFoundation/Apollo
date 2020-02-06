package com.apollocurrency.aplwallet.apl.core.files.statcheck;

import java.util.ArrayList;
import java.util.List;

/**
 * PeerInfo list with the same hash parameter
 * and additional informations like probablities history
 * @author al
 */
public class PeerInfoGroup {
    private String hash;
    List<PeerFileHashSum> pl = new ArrayList<>();    

    public PeerInfoGroup(String hash) {
        this.hash = hash;
    }

    public void add(PeerFileHashSum pi) {
        pl.add(pi);
    }
    
    public int count(){
        return pl.size();
    }

    boolean contains(PeerFileHashSum pi) {
        return pl.contains(pi);
    }
    
}
