package apl.updater;

import apl.util.Logger;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class Downloader {
    private static class DownloaderHolder {
        private static final Downloader HOLDER_INSTANCE = new Downloader();
    }

    public static Downloader getInstance() {
        return DownloaderHolder.HOLDER_INSTANCE;
    }

    private static final int ATTEMPTS = 10;
    private static final int TIMEOUT = 60;
    private static final String TEMP_DIR_PREFIX = "Apollo-update";
    private static final String DOWNLOADED_FILE_NAME = "Apollo-newVersion.jar";
    private static final StatusHolder HOLDER = new StatusHolder();

    private Downloader() {
    }

    private Path downloadAttempt(String url, String tempDirPrefix, String downloadedFileName) throws IOException {
        Path tempDir = Files.createTempDirectory(tempDirPrefix);
        Path downloadedFilePath = tempDir.resolve(Paths.get(downloadedFileName));
        try {
            URL webUrl = new URL(url);
            BufferedInputStream bis = new BufferedInputStream(webUrl.openStream());
            FileOutputStream fos = new FileOutputStream(downloadedFilePath.toFile());
            byte[] buffer = new byte[1024];
            int count = 0;
            while ((count = bis.read(buffer, 0, 1024)) != -1) {
                fos.write(buffer, 0, count);
            }
            fos.close();
            bis.close();
        }
        catch (Exception e) {
            //delete failed file and directory
            Files.deleteIfExists(downloadedFilePath);
            Files.deleteIfExists(tempDir);
            throw e;
        }
        return downloadedFilePath;
    }

    //todo: implement checkConsistency
    private boolean checkConsistency(Path file, byte hash[]) {
        return true;
    }

    public Path tryDownload(String url, byte[] hash) {
        int attemptsCounter = 0;
        while (attemptsCounter != ATTEMPTS) {
            try {
                attemptsCounter++;
                HOLDER.setState(DownloadState.IN_PROGRESS);
                Path downloadedFile = downloadAttempt(url, TEMP_DIR_PREFIX, DOWNLOADED_FILE_NAME);
                if (checkConsistency(downloadedFile, hash)) {
                    HOLDER.setStatus(DownloadStatus.OK);
                    HOLDER.setState(DownloadState.FINISHED);
                    return downloadedFile;
                } else {
                    HOLDER.setStatus(DownloadStatus.INCONSISTENT);
                    Logger.logErrorMessage("Inconsistent file, downloaded from: " + url);
                }
                HOLDER.setState(DownloadState.TIMEOUT);
                TimeUnit.SECONDS.sleep(TIMEOUT);
            }
            catch (IOException e) {
                Logger.logErrorMessage("Unable to download update from: " + url, e);
                HOLDER.setState(DownloadState.TIMEOUT);
                HOLDER.setStatus(DownloadStatus.CONNECTION_FAILURE);
            }
            catch (InterruptedException e) {
                Logger.logInfoMessage("Downloader was awakened", e);
            }
        }
        HOLDER.setState(DownloadState.FINISHED);
        HOLDER.setStatus(DownloadStatus.FAIL);
        return null;
    }


    public enum DownloadStatus {
        NONE,
        OK,
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

    public static class StatusHolder {
        private DownloadStatus status = DownloadStatus.NONE;
        private DownloadState state = DownloadState.NOT_STARTED;

        public StatusHolder() {
        }

        public StatusHolder(DownloadStatus status, DownloadState state) {
            this.status = status;
            this.state = state;
        }

        public synchronized DownloadStatus getStatus() {
            return status;
        }

        private synchronized void setStatus(DownloadStatus status) {
            this.status = status;
        }

        public synchronized DownloadState getState() {
            return state;
        }

        private synchronized void setState(DownloadState state) {
            this.state = state;
        }
    }

    public static StatusHolder getHolder() {
        return HOLDER;
    }
}
