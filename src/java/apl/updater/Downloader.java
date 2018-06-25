package apl.updater;

import apl.UpdateInfo;
import apl.UpdaterMediator;
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
    private static final int ATTEMPTS = 10;
    private static final int TIMEOUT = 60;
    private static final String TEMP_DIR_PREFIX = "Apollo-update";
    private static final String DOWNLOADED_FILE_NAME = "Apollo-newVersion.jar";
    private UpdaterMediator mediator = UpdaterMediator.getInstance();

    private Downloader() {}

    public static Downloader getInstance() {
        return DownloaderHolder.HOLDER_INSTANCE;
    }

    private Path downloadAttempt(String url, String tempDirPrefix, String downloadedFileName) throws IOException {
        Path tempDir = Files.createTempDirectory(tempDirPrefix);
        Path downloadedFilePath = tempDir.resolve(Paths.get(downloadedFileName));
        try {
            URL webUrl = new URL(url);
            BufferedInputStream bis = new BufferedInputStream(webUrl.openStream());
            FileOutputStream fos = new FileOutputStream(downloadedFilePath.toFile());
            byte[] buffer = new byte[1024];
            int count;
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
            //rethrow exception
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
        mediator.setStatus(UpdateInfo.DownloadStatus.STARTED);
        while (attemptsCounter != ATTEMPTS) {
            try {
                attemptsCounter++;
                mediator.setState(UpdateInfo.DownloadState.IN_PROGRESS);
                Path downloadedFile = downloadAttempt(url, TEMP_DIR_PREFIX, DOWNLOADED_FILE_NAME);
                if (checkConsistency(downloadedFile, hash)) {
                    mediator.setStatus(UpdateInfo.DownloadStatus.OK);
                    mediator.setState(UpdateInfo.DownloadState.FINISHED);
                    return downloadedFile;
                } else {
                    mediator.setStatus(UpdateInfo.DownloadStatus.INCONSISTENT);
                    Logger.logErrorMessage("Inconsistent file, downloaded from: " + url);
                }
                mediator.setState(UpdateInfo.DownloadState.TIMEOUT);
                TimeUnit.SECONDS.sleep(TIMEOUT);
            }
            catch (IOException e) {
                Logger.logErrorMessage("Unable to download update from: " + url, e);
                mediator.setState(UpdateInfo.DownloadState.TIMEOUT);
                mediator.setStatus(UpdateInfo.DownloadStatus.CONNECTION_FAILURE);
            }
            catch (InterruptedException e) {
                Logger.logInfoMessage("Downloader was awakened", e);
            }
        }
        mediator.setState(UpdateInfo.DownloadState.FINISHED);
        mediator.setStatus(UpdateInfo.DownloadStatus.FAIL);
        return null;
    }

    private static class DownloaderHolder {
        private static final Downloader HOLDER_INSTANCE = new Downloader();
    }
}
