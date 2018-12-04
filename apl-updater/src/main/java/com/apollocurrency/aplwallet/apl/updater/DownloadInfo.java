/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;


public class DownloadInfo {
    private volatile DownloadStatus downloadStatus = DownloadStatus.NONE;
    private volatile DownloadState downloadState = DownloadState.NOT_STARTED;

    public DownloadInfo() {
    }

    public DownloadStatus getDownloadStatus() {
        return downloadStatus;
    }

    public void setDownloadStatus(DownloadStatus downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    public DownloadState getDownloadState() {
        return downloadState;
    }

    public void setDownloadState(DownloadState downloadState) {
        this.downloadState = downloadState;
    }

    public enum DownloadStatus {
        NONE,
        OK,
        STARTED,
        CONNECTION_FAILURE,
        INCONSISTENT,
        FAIL
    }

    public enum DownloadState {
        NOT_STARTED,
        IN_PROGRESS,
        TIMEOUT,
        FINISHED
    }
}
