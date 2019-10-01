/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.files.statcheck;

import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.apl.core.peer.PeerClient;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

/**
 * Download File info together with PeerClient and Peer
 * @author alukin@gmail.com
 */
public class PeerFileInfo implements HasHashSum {

    private byte[] hash;
    private final PeerClient peerClient;
    private final String fileId;
    private FileDownloadInfo fdi;
    
    public PeerFileInfo(PeerClient peerClient, String fileId) {
        this.peerClient= peerClient;
        this.fileId = fileId;
    }
    
    @Override
    public byte[] getHash() {
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
    public byte[] retreiveHash() {
       fdi = peerClient.getFileInfo(fileId);
       if(fdi==null || fdi.fileInfo==null || fdi.fileInfo.hash==null){
          hash=null;
       }else{
          hash=Convert.parseHexString(fdi.fileInfo.hash); 
       }
       return hash;
    }

    public PeerClient getPeerClient() {
        return peerClient;
    }
      @Override
    public String toString() {
        String res = "Peer Id:"+getId()+" fileId:"+fileId;
        return res;
    }  
}
