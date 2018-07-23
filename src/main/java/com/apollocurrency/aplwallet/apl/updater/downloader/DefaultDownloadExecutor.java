package com.apollocurrency.aplwallet.apl.updater.downloader;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DefaultDownloadExecutor implements DownloadExecutor {

    private String tempDirPrefix;
    private String downloadedFileName;

    public DefaultDownloadExecutor(String tempDirPrefix, String downloadedFileName) {
        this.tempDirPrefix = tempDirPrefix;
        this.downloadedFileName = downloadedFileName;
    }

    public Path download(String uri) throws IOException {
        return downloadAttempt(uri, tempDirPrefix, downloadedFileName);
    }

    private static Path downloadAttempt(String url, String tempDirPrefix, String downloadedFileName) throws IOException {
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

}
