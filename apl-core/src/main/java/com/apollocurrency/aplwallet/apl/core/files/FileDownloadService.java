/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.files;

import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author alukin@gmail.com
 */
@Slf4j
@Singleton
public class FileDownloadService {

    private final Instance<FileDownloader> fileDownloaders;
    private final Instance<FileInfoDownloader> fileInfoDownloaders;
    private final Map<String, FileDownloadStatus> downloadStatus = new HashMap<>();
    private final DownloadableFilesManager downloadableFilesManager;
    private Event<FileEventData> fileEvent;

    @Inject
    public FileDownloadService(Instance<FileDownloader> fileDownloaders,
                               Instance<FileInfoDownloader> fileInfoDownloaders,
                               Event<FileEventData> fileEvent,
                               DownloadableFilesManager downloadableFilesManager
    ) {
        this.fileDownloaders = fileDownloaders;
        this.fileInfoDownloaders = fileInfoDownloaders;
        this.downloadableFilesManager = downloadableFilesManager;
        this.fileEvent = fileEvent;
    }

    private synchronized FileDownloadStatus prepareForDownloading(String fileId, Set<String> onlyPeers) {
        FileDownloadStatus fstatus = downloadStatus.get(fileId);
        if (fstatus != null) {
            log.warn("File {} is already in progress or downloaded. Status: {}", fileId, fstatus.toString());
            return fstatus;
        }
        FileInfoDownloader fileInfoDownloader = fileInfoDownloaders.get();
        fstatus = new FileDownloadStatus(fileId);
        downloadStatus.put(fileId, fstatus);
        fstatus.decision = fileInfoDownloader.prepareForDownloading(fileId, onlyPeers);
        fstatus.goodPeers = fileInfoDownloader.getGoodPeers();
        fstatus.fileDownloadInfo = fileInfoDownloader.getFileDownloadInfo();
        return fstatus;
    }

    public void startDownload(String fileId, Set<String> onlyPeers) {
        FileDownloadStatus fstatus = prepareForDownloading(fileId, onlyPeers);
        if (fstatus.downloaderStarted) {
            log.warn("File {} is already in progress or downloaded. Status: {}", fileId, fstatus.toString());
            return;
        }
        if (FileInfoDownloader.isNetworkUsable(fstatus.decision)) {
            FileDownloader downloader = fileDownloaders.get();
            downloader.startDownload(fstatus.getFileDownloadInfo(), fstatus, fstatus.getGoodPeers());
        } else {
            FileEventData data = new FileEventData(
                fileId,
                false,
                "File statistics is not acceptable: " + fstatus.decision.toString()
            );
            fileEvent.select(new AnnotationLiteral<FileDownloadEvent>() {
            }).fireAsync(data);
        }
    }

    public FileDownloadStatus getFileDownloadStatus(String fileId) {
        FileDownloadStatus res = downloadStatus.get(fileId);
        return res;
    }


    public boolean isFileDownloadedAlready(String fileId, String hexHashString) {
        boolean res = false;
        File zipInExportedFolder = downloadableFilesManager.mapFileIdToLocalPath(fileId).toFile();
        log.debug("Checking existence zip = '{}', ? = {}", zipInExportedFolder, zipInExportedFolder.exists());
        if (zipInExportedFolder.exists()) {
            log.info("No need to download '{}'  as it is found in path = '{}'", fileId, zipInExportedFolder.toString());
            //check integrity
            FileDownloadInfo fdi = downloadableFilesManager.updateFileDownloadInfo(fileId);
            String fileHashActual = fdi.fileInfo.hash;
            if (fileHashActual.equalsIgnoreCase(hexHashString)) {
                res = true;
                log.debug("Good zip hash was computed return '{}'...", res);
            } else {
                boolean deleteResult = zipInExportedFolder.delete();
                res = false;
                log.debug("bad shard file: '{}', received hash: '{}'. Calculated hash: '{}'. Zip is deleted = '{}'",
                    zipInExportedFolder.getAbsolutePath(), hexHashString, fileHashActual, deleteResult);
            }
        }

        return res;
    }
}
