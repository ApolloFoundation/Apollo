/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.downloader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
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

    private static Path downloadAttempt(String url, String tempDirPrefix, String downloadedFileName) throws IOException {
        Path tempDir = Files.createTempDirectory(tempDirPrefix);
        Path downloadedFilePath = tempDir.resolve(Paths.get(downloadedFileName));
        URL webUrl = new URL(url);
        try (
                ReadableByteChannel rbc = Channels.newChannel(webUrl.openStream());
                FileOutputStream fos = new FileOutputStream(downloadedFilePath.toFile())) {

            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
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

    public Path download(String uri) throws IOException {
        return downloadAttempt(uri, tempDirPrefix, downloadedFileName);
    }

}
