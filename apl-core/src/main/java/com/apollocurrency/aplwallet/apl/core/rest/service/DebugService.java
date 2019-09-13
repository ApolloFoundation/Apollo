/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.api.p2p.FileInfo;
import com.apollocurrency.aplwallet.apl.core.peer.DownloadableFilesManager;
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
    @Inject
    DownloadableFilesManager fileManager;
    
    public FileDownloadInfo startFileDownload(String id, String adminPassword){
//        downloader.startDownload(id);
//          downloader.setFileId(id);
//        FileDownloadDecision decision = downloader.prepareForDownloading(null);
        FileDownloadInfo fdi = fileManager.getFileDownloadInfo(id);
//        fdi  = downloader.getDownloadInfo();
        return fdi;
    }
}
