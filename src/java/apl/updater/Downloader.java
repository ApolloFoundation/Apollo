/*
 * Copyright © 2017-2018 Apollo Foundation
 */

package apl.updater;

import apl.UpdateInfo;
import apl.UpdaterMediator;
import apl.util.Logger;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static apl.updater.UpdaterConstants.*;

public class Downloader {
    private UpdaterMediator mediator = UpdaterMediator.getInstance();

    private Downloader() {
    }

    public static Downloader getInstance() {
        return DownloaderHolder.INSTANCE;
    }

    Path downloadAttempt(String url, String tempDirPrefix, String downloadedFileName) throws IOException {
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
        } catch (Exception e) {
            //delete failed file and directory
            Files.deleteIfExists(downloadedFilePath);
            Files.deleteIfExists(tempDir);
            //rethrow exception
            throw e;
        }
        return downloadedFilePath;
    }

    private boolean checkConsistency(Path file, byte hash[]) {
        try {
            byte[] actualHash = calclulateHash(file);
            if (Arrays.equals(hash, actualHash)) {
                return true;
            }
        } catch (Exception e) {
            Logger.logErrorMessage("Cannot calculate checksum for file: " + file, e);
        }
        return false;
    }

    private byte[] calclulateHash(Path file) throws IOException, NoSuchAlgorithmException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] block = new byte[4096];
            int length;
            while ((length = in.read(block)) > 0) {
                digest.update(block, 0, length);
            }
            return digest.digest();
        }
    }

    public Path tryDownload(String url, byte[] hash) {
        int attemptsCounter = 0;
        mediator.setStatus(UpdateInfo.DownloadStatus.STARTED);
        while (attemptsCounter != UpdaterConstants.DOWNLOAD_ATTEMPTS) {
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
                TimeUnit.SECONDS.sleep(NEXT_ATTEMPT_TIMEOUT);
            } catch (IOException e) {
                Logger.logErrorMessage("Unable to download update from: " + url, e);
                mediator.setState(UpdateInfo.DownloadState.TIMEOUT);
                mediator.setStatus(UpdateInfo.DownloadStatus.CONNECTION_FAILURE);
            } catch (InterruptedException e) {
                Logger.logInfoMessage("Downloader was awakened", e);
            }
        }
        mediator.setState(UpdateInfo.DownloadState.FINISHED);
        mediator.setStatus(UpdateInfo.DownloadStatus.FAIL);
        return null;
    }

    private static class DownloaderHolder {
        private static final Downloader INSTANCE = new Downloader();
    }
}
