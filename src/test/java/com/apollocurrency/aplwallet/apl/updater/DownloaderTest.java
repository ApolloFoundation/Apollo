/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.updater.downloader.DownloadExecutor;
import com.apollocurrency.aplwallet.apl.updater.downloader.Downloader;
import com.apollocurrency.aplwallet.apl.updater.downloader.DownloaderImpl;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

public class DownloaderTest {
        private static final Logger LOG = getLogger(DownloaderTest.class);

    /**
     * Pre-calculated sha256 of "test-downloader-payload";
     * this string should be stored as is without line breaker
     * in resources/testDownloaderPayload.txt
     */
    private static final byte[] TEST_PAYLOAD_SHA256 =
            DatatypeConverter.parseHexBinary("fbfb91711bdba87aebb3b6011c99ba3d84823ec729695c348a42b41393046a9c");

    /**
     * test with failing download scenario
     */
    @Test
    public void testTryDownloadWithInvalidUrl() {

        DownloadInfo downloadInfo = new DownloadInfo();
        Downloader downloader = new DownloaderImpl(downloadInfo, 1, 10, (path, hash) -> true, new ThrowingDownloadExecutor());

        Path result = downloader.tryDownload("unknow.url", new byte[0]);
        Assert.assertNull(result);
        Assert.assertEquals(downloadInfo.getDownloadState(), DownloadInfo.DownloadState.FINISHED);
        Assert.assertEquals(downloadInfo.getDownloadStatus(), DownloadInfo.DownloadStatus.FAIL);

    }

    /**
     * test with successful download scenario
     */
    @Test
    public void testTryDownloadWithSignedValidPayload() {
        DownloadInfo downloadInfo = new DownloadInfo();
        Downloader downloader = new DownloaderImpl(downloadInfo, 1, 10, (path, hash) -> true, new ResourceDownloadExecutor());
        Path result = downloader.tryDownload("testDownloaderPayload.txt", new byte[0]);

        Assert.assertNotNull(result);
        Assert.assertEquals(downloadInfo.getDownloadState(), DownloadInfo.DownloadState.FINISHED);
        Assert.assertEquals(downloadInfo.getDownloadStatus(), DownloadInfo.DownloadStatus.OK);

    }

    private static class ThrowingDownloadExecutor implements DownloadExecutor {

        @Override
        public Path download(String uri) throws IOException {
            throw new IOException("Can't download file");
        }

    }

    private static class ResourceDownloadExecutor implements DownloadExecutor {

        @Override
        public Path download(String uri) throws IOException {
            try {
                return UpdaterUtil.loadResourcePath(uri);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                throw new IOException("can't load resource");
            }
        }

    }

}
