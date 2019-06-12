/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer.statcheck;

import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.apl.core.peer.PeerClient;
import java.math.BigInteger;

/**
 * Download File info together with PeerClient and Peer
 * @author alukin@gmail.com
 */
public class PeerFileInfo implements HasHashSum {

    private BigInteger hash;
    private final PeerClient peerClient;
    private final String fileId;
    private FileDownloadInfo fdi;
    
    public PeerFileInfo(PeerClient peerClient, String fileId) {
        this.peerClient= peerClient;
        this.fileId = fileId;
    }
    
    @Override
    public BigInteger getHash() {
        return hash;
    }

    @Override
    public String getId() {
        return peerClient.gePeer().getAnnouncedAddress();
    }

    public FileDownloadInfo getFdi() {
        return fdi;
    }


    @Override
    public BigInteger retreiveHash() {
       fdi = peerClient.getFileInfo(fileId);
       if(fdi==null){
           return null;
       }
       hash=new BigInteger(fdi.fileInfo.hash); 
       return hash;
    }

    public PeerClient getPeerClient() {
        return peerClient;
    }
    
}
