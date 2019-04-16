/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.udpater.intfce.DownloadInfo;
import com.apollocurrency.aplwallet.apl.updater.downloader.DownloadExecutor;
import com.apollocurrency.aplwallet.apl.updater.downloader.Downloader;
import com.apollocurrency.aplwallet.apl.updater.downloader.DownloaderImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import javax.xml.bind.DatatypeConverter;

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
        assertNull(result);
        assertEquals(downloadInfo.getDownloadState(), DownloadInfo.DownloadState.FINISHED);
        assertEquals(downloadInfo.getDownloadStatus(), DownloadInfo.DownloadStatus.FAIL);

    }

    /**
     * test with successful download scenario
     */
    @Disabled
    public void testTryDownloadWithSignedValidPayload() {
        DownloadInfo downloadInfo = new DownloadInfo();
        Downloader downloader = new DownloaderImpl(downloadInfo, 1, 10, (path, hash) -> true, new ResourceDownloadExecutor());
        Path result = downloader.tryDownload("testDownloaderPayload.txt", new byte[0]);

        assertNotNull(result);
        assertEquals(downloadInfo.getDownloadState(), DownloadInfo.DownloadState.FINISHED);
        assertEquals(downloadInfo.getDownloadStatus(), DownloadInfo.DownloadStatus.OK);

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
//            try {
//                return UpdaterUtil.loadResourcePath(uri);
//            } catch (URISyntaxException e) {
//                e.printStackTrace();
//                throw new IOException("can't load resource");
//            }
            return null;
        }

    }

}
