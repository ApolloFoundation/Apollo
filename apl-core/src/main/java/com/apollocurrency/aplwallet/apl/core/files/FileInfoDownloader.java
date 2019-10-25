/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.files;

import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.FileDownloadDecision;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.HasHashSum;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.PeerFileInfo;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.PeerValidityDecisionMaker;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.PeersList;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerClient;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author alukin@gmail.com
 */
@Slf4j
public class FileInfoDownloader {
    private final PeersService peers;
    private List<HasHashSum> goodPeers;
    private List<HasHashSum> badPeers;
    private FileDownloadInfo downloadInfo;
     
    @Inject
    public FileInfoDownloader(PeersService peers) {
        this.peers = peers;
    }
      
    public FileDownloadDecision prepareForDownloading(String fileID, Set<Peer> onlyPeers) {
        log.debug("prepareForDownloading()...");
        FileDownloadDecision res;
        Set<Peer> allPeers;
        if(onlyPeers==null || onlyPeers.isEmpty()){
           allPeers = peers.getAllConnectedPeers();
        }else{
           allPeers=new HashSet<>(); 
           allPeers.addAll(onlyPeers);
        }
        log.debug("prepareForDownloading(), allPeers = {}", allPeers);
        PeersList<PeerFileInfo> pl = new PeersList<>();
        allPeers.forEach((pi) -> {
            PeerFileInfo pfi = new PeerFileInfo(new PeerClient(pi), fileID);
            if(pfi.retreiveHash()!=null){
              pl.add(pfi);
            }
        });
        log.debug("prepareForDownloading(), pl = {}", pl);
        PeerValidityDecisionMaker pvdm = new PeerValidityDecisionMaker(pl);
        res = pvdm.calcualteNetworkState();
        goodPeers = pvdm.getValidPeers();
        badPeers = pvdm.getInvalidPeers();
        log.debug("prepareForDownloading(), res = {}, goodPeers = {}, badPeers = {}", res, goodPeers, badPeers);
        if(pvdm.isNetworkUsable()){ // we have nough good peers and can start downloadinig
            PeerFileInfo pfi = (PeerFileInfo)goodPeers.get(0);
            downloadInfo = pfi.getFdi();
        }
        log.debug("prepareForDownloading(), res = {}", res);
        return res;
    }
  

    public List<HasHashSum> getGoodPeers() {
        return goodPeers;
    }
    
}
