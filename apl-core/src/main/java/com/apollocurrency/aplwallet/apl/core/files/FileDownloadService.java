/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.files;

import com.apollocurrency.aplwallet.apl.core.files.statcheck.FileDownloadDecision;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author alukin@gmail.com
 */
@Slf4j
public class FileDownloadService {

    private final Instance<FileDownloader> fileDownloaders;
    private final Instance<FileInfoDownloader> fileInfoDownloaders;
    private final Map<String, FileDownloadStatus> downloadStatus = new HashMap<>();

    @Inject
    public FileDownloadService(Instance<FileDownloader> fileDownloaders,
            Instance<FileInfoDownloader> fileInfoDownloaders
    ) {
        this.fileDownloaders = fileDownloaders;
        this.fileInfoDownloaders = fileInfoDownloaders;
    }

    private synchronized FileDownloadStatus prepareForDownloading(String fileId, Set<String> onlyPeers) {
        FileDownloadStatus fstatus = downloadStatus.get(fileId);
        if (fstatus != null) {
            log.warn("File {} is already in progress or downloaded. Status: {}", fileId, fstatus.toString());
            return fstatus;
        }
        FileInfoDownloader fileInfoDownloader = fileInfoDownloaders.get();
        fileInfoDownloader.prepareForDownloading(fileId, onlyPeers);
        fstatus = new FileDownloadStatus(fileId);
        downloadStatus.put(fileId, fstatus);
        fstatus.decision = fileInfoDownloader.prepareForDownloading(fileId, onlyPeers);
        fstatus.goodPeers = fileInfoDownloader.getGoodPeers();
        fstatus.fileDownloadInfo = fileInfoDownloader.getFileDownloadInfo();
        return fstatus;
    }

    public void startDownload(String fileId, Set<String> onlyPeers) {
        FileDownloadStatus fstatus = prepareForDownloading(fileId, onlyPeers);
        if(fstatus.downloaderStarted){
            log.warn("File {} is already in progress or downloaded. Status: {}", fileId, fstatus.toString());
            return;
        }
        if (isAccaptable(fstatus.decision)) {            
            FileDownloader downloader = fileDownloaders.get();
            downloader.startDownload(fstatus.getFileDownloadInfo(), fstatus, fstatus.getGoodPeers());
        }
    }

    public FileDownloadStatus getFileDownloadStatus(String fileId) {
        FileDownloadStatus res = downloadStatus.get(fileId);
        return res;
    }

    public static boolean isAccaptable(FileDownloadDecision decision) {
        boolean res = (decision == FileDownloadDecision.AbsOK) || (decision == FileDownloadDecision.OK);
        return res;
    }

}
