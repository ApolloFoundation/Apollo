/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.files;

import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.FileDownloadDecision;
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
import com.apollocurrency.aplwallet.apl.core.files.statcheck.PeerFileHashSum;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 *
 * @author alukin@gmail.com
 */
@Slf4j
public class FileInfoDownloader {
    private final PeersService peers;
    @Getter
    private Set<PeerFileHashSum> goodPeers;
    @Getter
    private Set<PeerFileHashSum> badPeers;
    private final Map<String,FileDownloadInfo> peersDownloadInfo  = new HashMap<>();
    @Getter
    private FileDownloadInfo fileDownloadInfo; 
    
    @Inject
    public FileInfoDownloader(PeersService peers) {
        this.peers = peers;
    }
      
    public FileDownloadDecision prepareForDownloading(String fileID, Set<String> onlyPeers) {
        log.debug("prepareForDownloading()...");
        FileDownloadDecision res;
        Set<Peer> allPeers;
        if(onlyPeers==null || onlyPeers.isEmpty()){
           allPeers = peers.getAllConnectedPeers();
        }else{
           allPeers=new HashSet<>();
           onlyPeers.stream().map((peerAddr) -> peers.findOrCreatePeer(null, peerAddr, true)).forEachOrdered((p) -> {
               allPeers.add(p);
            });
        }
        log.debug("prepareForDownloading(), allPeers = {}", allPeers);
        PeersList pl = new PeersList();
        allPeers.forEach((p) -> {
            PeerClient pc = new PeerClient(p);
            FileDownloadInfo fdi = pc.getFileInfo(fileID);
            if(fdi!=null){
              byte[] hash = Convert.parseHexString(fdi.fileInfo.hash);
              PeerFileHashSum pfhs = new PeerFileHashSum(hash, fileID, fileID);
              pl.add(pfhs);
              peersDownloadInfo.put(p.getHostWithPort(), fdi);
            }
        });
        log.debug("prepareForDownloading(), pl = {}", pl);
        PeerValidityDecisionMaker pvdm = new PeerValidityDecisionMaker(pl);
        res = pvdm.calcualteNetworkState();
        goodPeers = pvdm.getValidPeers();
        badPeers = pvdm.getInvalidPeers();
        log.debug("prepareForDownloading(), res = {}, goodPeers = {}, badPeers = {}", res, goodPeers, badPeers);
        if(pvdm.isNetworkUsable()){ // we have nough good peers and can start downloadinig
            PeerFileHashSum pfi = goodPeers.iterator().next();
            fileDownloadInfo = peersDownloadInfo.get(pfi.getPeerId());            
        }
        log.debug("prepareForDownloading(), res = {}", res);
        return res;
    }
    
}
