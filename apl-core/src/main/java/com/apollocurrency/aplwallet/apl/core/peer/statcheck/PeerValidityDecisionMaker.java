/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer.statcheck;

import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;


/**
 *
 * @author alukin@gmail.com
 */
@Slf4j
public class PeerValidityDecisionMaker {

    public final static int MAX_TRIES_TO_GET_NEW=30;
    public final static int MIN_PEERS=20;
    public final static double MIN_PEERS_PRCENT=10.0;
    
    
    static int peersFirst=20;
    static int peersAddedLater=30;
     
    private final PeersList peers;
    PeerInfoStatistics stats = new PeerInfoStatistics();

    private Map<BigInteger, ProbabInfo> initialProbabilities;
    private Map<BigInteger, ProbabInfo> currentProbabilities = new HashMap<>();
    private static final double trasholdAbsOK=0.95D; 
    private static final double trasholdOK=0.8D; 
    private static final double trasholdRisky=0.6D;
    private static final double trasholdInvestigation=0.5D;//0.5 is critical probability value
     
    public PeerValidityDecisionMaker(PeersList peers) {
        this.peers = peers;
    }

    /**
     * Take n peers ans calculate hypotetic probablities of each hash value
     *
     * @param n number of peers to get
     * @return map of hypotetic probabilities for each discovered hash value
     */
    public Map<BigInteger, ProbabInfo> calculateInitialProb(int n) throws NotEnoughDataException {
        List<HasHashSum> first = new ArrayList<>();
        first.addAll(peers.getEnoughRandomPeers(MIN_PEERS, MIN_PEERS_PRCENT));
        //sort by hash
        for (HasHashSum pi : first) {
            stats.add(pi);
        }
        //calculate initial probabilities as frequences
        initialProbabilities = stats.getFrequences();
        //copy to current
        currentProbabilities.putAll(initialProbabilities);
        return initialProbabilities;
    }

    public Map.Entry<BigInteger, ProbabInfo> getMostProbable() {
        double pMax = -1.0D;
        BigInteger hashMax = null;
        for (BigInteger hash : currentProbabilities.keySet()) {
            double p = currentProbabilities.get(hash).frequency;
            if (p > pMax) {
                pMax = p;
                hashMax = hash;
            }
        }
        return new AbstractMap.SimpleEntry<>(hashMax, currentProbabilities.get(hashMax));
    }

    private HasHashSum getOneMorePeer() {
        boolean isNew = false;
        int nTries = 0;
        HasHashSum pi = null;
        while(isNew){
            pi = peers.getRandomPeer();
            isNew = !stats.isAlreadyCounted(pi);
            if(!isNew){
                continue;
            }
            nTries++;
            if (nTries > MAX_TRIES_TO_GET_NEW) {
                pi = null;
                break;
            }
        }
        return pi;
    }

    public Map<BigInteger, ProbabInfo> calculateByAddingPeers(int nPeers) throws NotEnoughDataException{
        for (int i = 0; i < nPeers; i++) {
            HasHashSum pi = getOneMorePeer();
            //re-calculate frequencies/probabilities simply adding more peers.
            stats.add(pi);
        }
        currentProbabilities = stats.getFrequences();
        return currentProbabilities;
    }
    
    /**
     * Gets all peers that have most probable hash value
     * @return list of peers with most probable hash value
     */
    public List<HasHashSum> getValidPeers(){
        Map.Entry<BigInteger, ProbabInfo> mp = getMostProbable();
        return stats.getByHash(mp.getKey());
    }
    
    /**
     * Gets all peers that have hash values different from most probale
     * @return list of bad peers
     */
    public List<HasHashSum> getInvalidPeers(){
        Map.Entry<BigInteger, ProbabInfo> mp = getMostProbable();
        return stats.getAllExceptHash(mp.getKey());      
    }
    
    /**
     * Calculates network statistics and tells
     * can we use it or can not
     * @return true is network is usable;
     */
    public boolean isNetworkUsable(){
        FileDownloadDecision d = calcualteNetworkState();
        boolean usable = false;
        switch(d){
            case AbsOK: usable = true;
            break;
            case OK: usable = true;
            break;
            case Risky: usable = true;
            break;             
        }
        return usable;
    }
    
    /**
     * Calculates usability of network by sampling
     * @return Decision from AbsOK to Bad
     */
    public FileDownloadDecision calcualteNetworkState(){
         stats.crlear();
        try {
            calculateInitialProb(peersFirst);
        } catch (NotEnoughDataException ex) {
            log.debug("Not enough statistics");
            return FileDownloadDecision.NoPeers;
        }
         Map.Entry<BigInteger, ProbabInfo> mp = getMostProbable();
         //check most probable hash higher value
         double pMin=mp.getValue().frequency-mp.getValue().confidenceEpsilon;
         if(pMin>=trasholdAbsOK){
             return FileDownloadDecision.AbsOK;
         }
        try {
            calculateByAddingPeers(peersAddedLater);
        } catch (NotEnoughDataException ex) {
            log.debug("Not enough statistics");
            return FileDownloadDecision.NoPeers;
        }
         Map.Entry<BigInteger, ProbabInfo> mp2 = getMostProbable();
         double pMin2=mp2.getValue().frequency-mp2.getValue().confidenceEpsilon;
         if(mp2.getKey()!=mp.getKey()){ //second portion of peers gives differrent most probable hash
             if(pMin2>trasholdInvestigation){
                return FileDownloadDecision.NeedsInvestigation; 
             }else{
                return FileDownloadDecision.Bad;
             }
         }
         //second portion of peers gives the same most probable hash
         if(pMin2>=trasholdAbsOK){
             return FileDownloadDecision.AbsOK;
         }
         if(pMin2>=trasholdOK){
             return FileDownloadDecision.OK;
         }
         if(pMin2>=trasholdRisky){
             return FileDownloadDecision.Risky;
         }
         if(pMin2>=trasholdInvestigation){
             return FileDownloadDecision.NeedsInvestigation;
         }
         return FileDownloadDecision.Bad;
    }
}
