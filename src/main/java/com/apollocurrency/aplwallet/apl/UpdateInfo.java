/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import org.json.simple.JSONObject;

public class UpdateInfo {
    private volatile boolean isUpdate = false;
    private volatile int estimatedHeight = 0;
    private volatile int receivedHeight = 0;
    private volatile Level level;
    private volatile Version version = Version.from("1.0.0");
    private volatile DownloadStatus downloadStatus = DownloadStatus.NONE;
    private volatile DownloadState downloadState = DownloadState.NOT_STARTED;
    private volatile UpdateState updateState = UpdateState.NONE;

    public UpdateState getUpdateState() {
        return updateState;
    }

    public void setUpdateState(UpdateState updateState) {
        this.updateState = updateState;
    }

    private static class UpdateInfoHolder {
        private static final UpdateInfo HOLDER_INSTANCE = new UpdateInfo();
    }

    public static UpdateInfo getInstance() {
        return UpdateInfoHolder.HOLDER_INSTANCE;
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

    public enum UpdateState {
        NONE, IN_PROGRESS, REQUIRED_START, REQUIRED_MANUAL_INSTALL, RE_PLANNING, FINISHED, FAILED_REQUIRED_START
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

    public synchronized boolean isUpdate() {
        return isUpdate;
    }

    public void setUpdate(boolean update) {
        isUpdate = update;
    }

    public int getEstimatedHeight() {
        return estimatedHeight;
    }

    public void setEstimatedHeight(int estimatedHeight) {
        this.estimatedHeight = estimatedHeight;
    }

    public int getReceivedHeight() {
        return receivedHeight;
    }

    public void setReceivedHeight(int receivedHeight) {
        this.receivedHeight = receivedHeight;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public synchronized JSONObject json() {
        JSONObject result = new JSONObject();
            result.put("isUpdate", isUpdate);
            if (isUpdate()) {
                result.put("level", level);
                result.put("availableVersion", version.toString());
                result.put("estimatedUpdateHeight", estimatedHeight);
                result.put("receivedHeight", receivedHeight);
                result.put("downloadStatus", downloadStatus);
                result.put("downloadState", downloadState);
                result.put("updateState", updateState);
            }
        return result;
    }

    public synchronized boolean isStartAllowed() {
        return level == Level.MINOR && estimatedHeight == -1 && updateState == UpdateState.REQUIRED_START;
    }
}
