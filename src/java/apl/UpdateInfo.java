package apl;

public class UpdateInfo {
    private boolean isUpdate = false;
    private int updateHeight = 0;
    private int receivedUpdateHeight = 0;
    private String updateLevel = "NO_UPDATE";
    private Version updateVersion = Version.from("1.0.0");
    private DownloadStatus status = DownloadStatus.NONE;
    private DownloadState state = DownloadState.NOT_STARTED;

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

    public synchronized String getUpdateLevel() {
        return updateLevel;
    }

    public synchronized void setUpdateLevel(String updateLevel) {
        this.updateLevel = updateLevel;
    }

    public synchronized Version getUpdateVersion() {
        return updateVersion;
    }

    public synchronized void setUpdateVersion(Version updateVersion) {
        this.updateVersion = updateVersion;
    }
}
