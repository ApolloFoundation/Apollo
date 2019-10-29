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
    private final FileInfoDownloader fileInfoDownloader;
    private final Map<String, FileDownloadStatus> downloadStatus = new HashMap<>();

    @Inject
    public FileDownloadService(Instance<FileDownloader> fileDownloaders,
            FileInfoDownloader fileInfoDownloader
    ) {
        this.fileDownloaders = fileDownloaders;
        this.fileInfoDownloader = fileInfoDownloader;
    }

    public boolean prepareForDownloading(String fileId) {
        boolean res = false;
        return res;
    }

    public void startDownload(String fileId, Set<String> onlyPeers) {
        FileDownloadStatus fstatus = downloadStatus.get(fileId);
        if (fstatus != null) {
            log.warn("File {} is already in progress or downloaded. Status: {}", fileId, fstatus.toString());
            return;
        }
        fstatus = new FileDownloadStatus(fileId);
        downloadStatus.put(fileId, fstatus);
        fstatus.decision = fileInfoDownloader.prepareForDownloading(fileId, onlyPeers);
        if(isAccaptable(fstatus.decision)){
            FileDownloader downloader = fileDownloaders.get();
            downloader.startDownload(fileInfoDownloader.getFileDownloadInfo(),fstatus, fileInfoDownloader.getGoodPeers());
        }
    }

    public FileDownloadStatus getFileDownloadStatus(String fileId) {
        FileDownloadStatus res = downloadStatus.get(fileId);
        return res;
    }

    public static boolean isAccaptable(FileDownloadDecision decision) {
        boolean res = (decision==FileDownloadDecision.AbsOK) || (decision == FileDownloadDecision.OK);
        return res;
    }

}
