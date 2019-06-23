/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.api.p2p.FileInfo;
import com.apollocurrency.aplwallet.apl.core.peer.FileDownloader;
import com.apollocurrency.aplwallet.apl.core.peer.statcheck.FileDownloadDecision;
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
//        downloader.startDownload(id);
        FileDownloadDecision decision = downloader.prepareForDownloading(null);
        FileDownloadInfo fdi = downloader.getDownloadInfo();
        FileInfo fi = new FileInfo();
        fdi.fileInfo=fi;
        return fdi;
    }
}
