/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.files;

import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import static com.apollocurrency.aplwallet.apl.core.files.FileDownloader.DOWNLOAD_THREADS;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.FileDownloadDecision;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.PeerValidityDecisionMaker;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.PeersList;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerClient;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.PeerFileHashSum;
import com.apollocurrency.aplwallet.apl.core.peer.PeerAddress;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.Getter;

/**
 *
 * @author alukin@gmail.com
 */
@Slf4j
public class FileInfoDownloader {
    private final PeersService peersService;
    @Getter
    private Set<PeerFileHashSum> goodPeers;
    @Getter
    private Set<PeerFileHashSum> badPeers;
    private final Map<String,FileDownloadInfo> peersDownloadInfo  = new HashMap<>();
    @Getter
    private FileDownloadInfo fileDownloadInfo; 
    private final ExecutorService executor;
    private final Map<String,Future<FileDownloadInfo>> runningDownloaders = new HashMap<>(); 
    
    @Inject
    public FileInfoDownloader(PeersService peersService) {
        this.peersService = peersService;
        this.executor = Executors.newFixedThreadPool(DOWNLOAD_THREADS);
    }
    
    public FileDownloadInfo getFileDownloadInfoFromPeer(String pa, String fileId) {
        Objects.requireNonNull(pa, "pa is NULL");
        Objects.requireNonNull(fileId, "fileId is NULL");
        FileDownloadInfo res = null;
        PeerAddress peerAddr = new PeerAddress(pa);
        Peer p = peersService.findOrCreatePeer(peerAddr, null, true);
        if(p!=null){
           PeerClient pc = new PeerClient(p);
           res = pc.getFileInfo(fileId);
           if(res!=null){
               peersDownloadInfo.put(pa, res);
           }
        }
        return res;
    }
    
    public Map<String,FileDownloadInfo> getFileDownloadInfo(String fileID, Set<String> peers) throws ExecutionException {
        Objects.requireNonNull(fileID, "fileId is NULL");
        Objects.requireNonNull(peers, "peers is NULL");
        for(String pa: peers){
           Future<FileDownloadInfo> dn_res = executor.submit(new Callable<FileDownloadInfo>(){
               @Override
               public FileDownloadInfo call() throws Exception {
                   return getFileDownloadInfoFromPeer(pa, fileID);
               }
               
           });
           runningDownloaders.put(pa,dn_res);
        }
        for( String df: runningDownloaders.keySet()){
            FileDownloadInfo fdi;
            try {
                fdi = runningDownloaders.get(df).get(); //get future
                if(fdi!=null){
                  peersDownloadInfo.put(df,fdi); 
                }                
            } catch (InterruptedException ex) {
                log.info("FileInfoDownloader was interrupted", ex);
                Thread.currentThread().interrupt();
            }   
        }
        return peersDownloadInfo;
    }
    
    public FileDownloadDecision processFileDownloadInfo() {
        FileDownloadDecision res;
        PeersList pl = new PeersList();
        for(String pa: peersDownloadInfo.keySet()){
            FileDownloadInfo fdi = peersDownloadInfo.get(pa);
              byte[] hash = Convert.parseHexString(fdi.fileInfo.hash);
              PeerFileHashSum pfhs = new PeerFileHashSum(hash, pa, fdi.fileInfo.fileId);
              pl.add(pfhs);
        }
        log.debug("perr list = {}", pl);
        PeerValidityDecisionMaker pvdm = new PeerValidityDecisionMaker(pl);
        res = pvdm.calcualteNetworkState();
        goodPeers = pvdm.getValidPeers();
        badPeers = pvdm.getInvalidPeers();        
        return res;
    }
      /**
     * Calculates network statistics and tells
     * can we use it or can not
     * @param d
     * @return true is network is usable;
     */
    public boolean isNetworkUsable(FileDownloadDecision d) {
        Objects.requireNonNull(d, "decision is NULL");
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
    
    public FileDownloadDecision prepareForDownloading(String fileID, Set<String> onlyPeers) {
        log.debug("prepareForDownloading()...");
        Objects.requireNonNull(fileID, "fileId is NULL");
        Objects.requireNonNull(onlyPeers, "onlyPeers is NULL");
        FileDownloadDecision res;
        Set<String> allPeers = new HashSet<>();
        if(onlyPeers==null || onlyPeers.isEmpty()){
           for(Peer p: peersService.getAllConnectedPeers()){
               allPeers.add(p.getHostWithPort());
           }
        }else{
           allPeers=onlyPeers;
        }
        log.debug("prepareForDownloading(), allPeers = {}", allPeers);
        try {
            getFileDownloadInfo(fileID,allPeers);
        } catch (ExecutionException ex) {
            log.info("Can not execute file info download thread", ex);
        }
        res = processFileDownloadInfo();

        log.debug("prepareForDownloading(), res = {}, goodPeers = {}, badPeers = {}", res, goodPeers, badPeers);
        if(isNetworkUsable(res)){ // we have nough good peers and can start downloadinig
            PeerFileHashSum pfi = goodPeers.iterator().next();
            fileDownloadInfo = peersDownloadInfo.get(pfi.getPeerId());            
        }
        log.debug("prepareForDownloading(), res = {}", res);
        return res;
    }
    
}
