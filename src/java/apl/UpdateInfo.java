package apl;

import org.json.simple.JSONObject;

public class UpdateInfo {
    private boolean isUpdate = false;
    private int updateHeight = 0;
    private int receivedUpdateHeight = 0;
    private Level updateLevel;
    private Version updateVersion = Version.from("1.0.0");
    private DownloadStatus status = DownloadStatus.NONE;
    private DownloadState state = DownloadState.NOT_STARTED;
    private UpdateState updateState = UpdateState.NONE;

    public synchronized UpdateState getUpdateState() {
        return updateState;
    }

    public synchronized void setUpdateState(UpdateState updateState) {
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
        NONE, IN_PROGRESS, REQUIRED_START, REQUIRED_MANUAL_INSTALL, RE_PLANNING, FINISHED
    }

    public synchronized DownloadStatus getStatus() {
        return status;
    }

    public synchronized void setStatus(DownloadStatus status) {
        this.status = status;
    }

    public synchronized DownloadState getState() {
        return state;
    }

    public synchronized void setState(DownloadState state) {
        this.state = state;
    }

    public synchronized boolean isUpdate() {
        return isUpdate;
    }

    public synchronized void setUpdate(boolean update) {
        isUpdate = update;
    }

    public synchronized int getUpdateHeight() {
        return updateHeight;
    }

    public synchronized void setUpdateHeight(int updateHeight) {
        this.updateHeight = updateHeight;
    }

    public synchronized int getReceivedUpdateHeight() {
        return receivedUpdateHeight;
    }

    public synchronized void setReceivedUpdateHeight(int receivedUpdateHeight) {
        this.receivedUpdateHeight = receivedUpdateHeight;
    }

    public synchronized Level getUpdateLevel() {
        return updateLevel;
    }

    public synchronized void setUpdateLevel(Level updateLevel) {
        this.updateLevel = updateLevel;
    }

    public synchronized Version getUpdateVersion() {
        return updateVersion;
    }

    public synchronized void setUpdateVersion(Version updateVersion) {
        this.updateVersion = updateVersion;
    }

    public synchronized JSONObject json() {
        JSONObject result = new JSONObject();
            result.put("isUpdate", isUpdate());
            if (isUpdate()) {
                result.put("level", getUpdateLevel());
                result.put("availableVersion", getUpdateVersion().toString());
                result.put("estimatedUpdateHeight", getUpdateHeight());
                result.put("receivedUpdateHeight", getReceivedUpdateHeight());
                result.put("downloadStatus", getStatus());
                result.put("downloadState", getState());
            }
        return result;
    }

    public synchronized boolean isStartAllowed() {
        return updateLevel == Level.MINOR && updateHeight == -1 && updateState == UpdateState.REQUIRED_START;
    }
}
