package com.apollocurrency.aplwallet.apl.core.files.statcheck;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author al
 */
@Slf4j
public class PeersList {

    private final List<PeerFileHashSum> allPeers = new ArrayList<>();

    public void add(PeerFileHashSum p) {
        allPeers.add(p);
    }

    public PeerFileHashSum getPeer(int idx) {
        return allPeers.get(idx);
    }

    /**
     * get random peers or all if number is bigger then count of peers
     *
     * @param number number of peers to get
     * @return set of peers that have requested file
     */
    public Set<PeerFileHashSum> getNPeers(int number) {
        Set<PeerFileHashSum> res = new HashSet<>();
        Set<Integer> idxSet = new HashSet<>();
        if (number > allPeers.size()) { //add all if we have less the required
            for (int i = 0; i < allPeers.size(); i++) {
                idxSet.add(i);
            }
        } else {
            while (idxSet.size() < number) { //add random peers from list
                int i = (int) Math.round(Math.random() * (allPeers.size() - 1));
                idxSet.add(i);
            }
        }
        //well, now we have to get hash of entity from peers
        //but there coud be not enough of them
        //TODO: what to do if we have less? Just request bigger numbers?
        for (int idx : idxSet) {
            PeerFileHashSum p = allPeers.get(idx);
            if (p != null && p.getHash() != null) {
                res.add(p);
            } else {
                log.trace("Can not get hash from {}", p == null ? "null" : p.getPeerId());
            }
        }
        return res;
    }

    public PeerFileHashSum getRandomPeer() {
        int i = (int) Math.round(Math.random() * (allPeers.size() - 1));
        PeerFileHashSum res = allPeers.get(i);
        return res;
    }

    public Set<PeerFileHashSum> getEnoughRandomPeers(long min, double min_percent) {
        Long n = Math.round(allPeers.size() * min_percent / 100);
        if (n < min) {
            n = min;
        }
        Set<PeerFileHashSum> is = getNPeers(n.intValue());
        return is;
    }
}
