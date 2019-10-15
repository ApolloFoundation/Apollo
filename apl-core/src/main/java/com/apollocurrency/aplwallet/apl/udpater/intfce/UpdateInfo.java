/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.udpater.intfce;

import com.apollocurrency.aplwallet.apl.util.Version;
import org.json.simple.JSONObject;

import javax.inject.Singleton;

@Singleton
public class UpdateInfo implements Cloneable{
    private long id;
    private boolean isUpdate;
    private Level level;
    private Version version;
    private UpdateState updateState = UpdateState.NONE;
    private DownloadInfo downloadInfo = new DownloadInfo();

    public UpdateInfo(boolean isUpdate, long id, Level level, Version version, UpdateState updateState) {
        this.isUpdate = isUpdate;
        this.id = id;
        this.level = level;
        this.version = version;
        this.updateState = updateState;
    }
    public UpdateInfo(boolean isUpdate, long id, Level level, Version version) {
        this(isUpdate, id, level, version, UpdateState.NONE);
    }

    public UpdateInfo() {
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
            result.put("downloadStatus", downloadInfo.getDownloadStatus());
            result.put("downloadState", downloadInfo.getDownloadState());
            result.put("updateState", updateState);
        }
        return result;
    }

    public synchronized long getId() {
        return id;
    }

    public synchronized void setId(long id) {
        this.id = id;
    }

    public synchronized void setDownloadInfo(DownloadInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
    }

    public enum UpdateState {
        NONE, IN_PROGRESS, REQUIRED_START, REQUIRED_MANUAL_INSTALL, RE_PLANNING, FINISHED, FAILED_REQUIRED_START
    }

    @Override
    public UpdateInfo clone() {
        UpdateInfo clone;
        try {
            clone = (UpdateInfo) super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException(e.toString(), e);
        }
        if (this.version != null) {
            clone.setVersion(new Version(version.toString()));
        }
        DownloadInfo downloadInfo = new DownloadInfo();
        downloadInfo.setDownloadState(this.downloadInfo.getDownloadState());
        downloadInfo.setDownloadStatus(this.downloadInfo.getDownloadStatus());
        clone.setDownloadInfo(downloadInfo);
        return clone;
    }
}
