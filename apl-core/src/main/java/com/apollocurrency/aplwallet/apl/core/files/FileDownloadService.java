/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.files;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author alukin@gmail.com
 */
@Slf4j
public class FileDownloadService {
//hope I do not need this hack
//    private final Instance<FileDownloader> fileDownloaders;

    private final FileDownloader fileDownloader;

    @Inject
    public FileDownloadService(FileDownloader fileDownloader) {
        this.fileDownloader = fileDownloader;
    }

    public boolean prepareForDownloading(String fileId) {
        boolean res = false;
        return res;
    }

    public void startDownload(String fileId) {

    }

    public FileDownloadStatus getFileDownloadStatus(String fileId) {
        FileDownloadStatus status = null;
        return status;
    }
}
