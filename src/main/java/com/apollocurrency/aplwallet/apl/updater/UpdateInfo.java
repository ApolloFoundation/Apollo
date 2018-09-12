/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.Level;
import com.apollocurrency.aplwallet.apl.Version;
import org.json.simple.JSONObject;

public class UpdateInfo {
    private boolean isUpdate;
    private int estimatedHeight;
    private int receivedHeight;
    private Level level;
    private Version version;
    private UpdateState updateState;
    private DownloadInfo downloadInfo = new DownloadInfo();

    public UpdateInfo(boolean isUpdate, int estimatedHeight, int receivedHeight, Level level, Version version, UpdateState updateState) {
        this.isUpdate = isUpdate;
        this.estimatedHeight = estimatedHeight;
        this.receivedHeight = receivedHeight;
        this.level = level;
        this.version = version;
        this.updateState = updateState;
    }
    public UpdateInfo(boolean isUpdate, int estimatedHeight, int receivedHeight, Level level, Version version) {
        this(isUpdate, estimatedHeight, receivedHeight, level, version, UpdateState.NONE);
    }

    public synchronized UpdateInfo.UpdateState getUpdateState() {
        return updateState;
    }

    public synchronized void setUpdateState(UpdateInfo.UpdateState updateState) {
        this.updateState = updateState;
    }

    public synchronized DownloadInfo getDownloadInfo() {
        return downloadInfo;
    }

    public synchronized boolean isUpdate() {
        return isUpdate;
    }

    public synchronized void setUpdate(boolean update) {
        isUpdate = update;
    }

    public synchronized int getEstimatedHeight() {
        return estimatedHeight;
    }

    public synchronized void setEstimatedHeight(int estimatedHeight) {
        this.estimatedHeight = estimatedHeight;
    }

    public synchronized int getReceivedHeight() {
        return receivedHeight;
    }

    public synchronized void setReceivedHeight(int receivedHeight) {
        this.receivedHeight = receivedHeight;
    }

    public synchronized Level getLevel() {
        return level;
    }

    public synchronized void setLevel(Level level) {
        this.level = level;
    }

    public synchronized Version getVersion() {
        return version;
    }

    public synchronized void setVersion(Version version) {
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
            result.put("downloadStatus", downloadInfo.getDownloadStatus());
            result.put("downloadState", downloadInfo.getDownloadState());
            result.put("updateState", updateState);
        }
        return result;
    }

    public synchronized boolean isStartAllowed() {
        return level == Level.MINOR && estimatedHeight == -1 && updateState == UpdateInfo.UpdateState.REQUIRED_START;
    }

    public enum UpdateState {
        NONE, IN_PROGRESS, REQUIRED_START, REQUIRED_MANUAL_INSTALL, RE_PLANNING, FINISHED, FAILED_REQUIRED_START
    }


}
