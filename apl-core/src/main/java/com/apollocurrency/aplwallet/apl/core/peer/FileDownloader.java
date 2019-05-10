/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.PeerValidityDecisionMaker;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * This class performs complete file downloading from peers
 * @author alukin@gmail.com
 */
public class FileDownloader {
    private final String fileID;
    private FileDownloadInfo downloadInfo;
    private List<Peer> goodPeers;
    private List<Peer> badPeers;
    
    public FileDownloader(String fileID) {
        this.fileID = fileID;
    }
    public class Status{
        double completed;
        int chunksTotal;
        int chunksReady;
        List<String> peers; 
    }
    
    public Status getDownloadStatus(){
        Status res = new Status();
        res.completed=1.0D*res.chunksTotal/res.chunksReady;
        return res;
    }
    
    public PeerValidityDecisionMaker.Decision prepareForDownloading(){
        PeerValidityDecisionMaker.Decision res = PeerValidityDecisionMaker.Decision.Bad;
        return res;
    }
    
    public Status download(){
       Status res = new Status();
       return res;
    }
}
