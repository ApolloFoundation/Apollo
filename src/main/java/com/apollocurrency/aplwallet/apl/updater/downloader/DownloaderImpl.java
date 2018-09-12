/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.downloader;

import com.apollocurrency.aplwallet.apl.updater.ConsistencyVerifier;
import com.apollocurrency.aplwallet.apl.updater.DownloadInfo;
import com.apollocurrency.aplwallet.apl.util.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class DownloaderImpl implements Downloader {
    private DownloadExecutor defaultDownloadExecutor;
    private DownloadInfo info;
    private int timeout;
    private int maxAttempts;
    private ConsistencyVerifier consistencyVerifier;
    
    public DownloaderImpl(DownloadInfo downloadInfo, int timeout, int maxAttempts, ConsistencyVerifier consistencyVerifier, DownloadExecutor downloadExecutor) {
        this.info = downloadInfo;
        this.timeout = timeout;
        this.maxAttempts = maxAttempts;
        this.consistencyVerifier = consistencyVerifier;
        this.defaultDownloadExecutor = downloadExecutor == null ? this.defaultDownloadExecutor : downloadExecutor;
    }
    public DownloaderImpl(DownloadInfo downloadInfo, int timeout, int maxAttempts, ConsistencyVerifier consistencyVerifier) {
        this(downloadInfo, timeout, maxAttempts, consistencyVerifier, new DefaultDownloadExecutor("", ""));
    }


    /**
     * Download file from uri and return Path to downloaded file
     * @param uri - path from which resource will be downloaded
     * @param hash - expected hash of downloaded resource
     * @param downloadExecutor - configurable download executor
     * @return path to downloaded resource or null if downloading was failed or downloaded resource did not pass hash verification
     */
    public Path tryDownload(String uri, byte[] hash, DownloadExecutor downloadExecutor) {
        int attemptsCounter = 0;
        info.setDownloadStatus(DownloadInfo.DownloadStatus.STARTED);
        while (attemptsCounter != maxAttempts) {
            try {
                attemptsCounter++;
                info.setDownloadState(DownloadInfo.DownloadState.IN_PROGRESS);
                Path downloadedFile = downloadExecutor.download(uri);
                if (consistencyVerifier.verify(downloadedFile, hash)) {
                    info.setDownloadStatus(DownloadInfo.DownloadStatus.OK);
                    info.setDownloadState(DownloadInfo.DownloadState.FINISHED);
                    return downloadedFile;
                } else {
                    info.setDownloadStatus(DownloadInfo.DownloadStatus.INCONSISTENT);
                    Logger.logErrorMessage("Inconsistent file, downloaded from: " + uri);
                }
                info.setDownloadState(DownloadInfo.DownloadState.TIMEOUT);
                TimeUnit.SECONDS.sleep(timeout);
            }
            catch (IOException e) {
                Logger.logErrorMessage("Unable to download update from: " + uri, e);
                info.setDownloadState(DownloadInfo.DownloadState.TIMEOUT);
                info.setDownloadStatus(DownloadInfo.DownloadStatus.CONNECTION_FAILURE);
            }
            catch (InterruptedException e) {
                Logger.logInfoMessage("Downloader was awakened", e);
            }
        }
        info.setDownloadState(DownloadInfo.DownloadState.FINISHED);
        info.setDownloadStatus(DownloadInfo.DownloadStatus.FAIL);
        return null;
    }

    /**
     * Download file from url and return Path to downloaded file
     * Uses default DownloadExecutor
     * @param uri
     * @param hash
     * @return
     */
    public Path tryDownload(String uri, byte[] hash) {
        return tryDownload(uri, hash, defaultDownloadExecutor);
    }

}
