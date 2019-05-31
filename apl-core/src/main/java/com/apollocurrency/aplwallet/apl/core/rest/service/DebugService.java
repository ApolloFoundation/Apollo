/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.api.p2p.FileInfo;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.FileDownloader;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.FileDownloadDecision;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.PeerValidityDecisionMaker;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 *
 * @author alukin@gmail.com
 */
@Singleton
public class DebugService {
    @Inject 
    FileDownloader downloader;

    
    public FileDownloadInfo startFileDownload(String id, String adminPassword){
        downloader.startDownload(id);
        Set<Peer> peers = downloader.getAllAvailablePeers();
        FileDownloadDecision decision = downloader.prepareForDownloading();
        FileDownloadInfo fdi = new FileDownloadInfo();
        FileInfo fi = new FileInfo();
        fdi.fileInfo=fi;
        return fdi;
    }
}
