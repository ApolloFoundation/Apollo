package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.UpdateInfo;
import com.apollocurrency.aplwallet.apl.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.updater.downloader.DownloadExecutor;
import com.apollocurrency.aplwallet.apl.updater.downloader.Downloader;
import com.apollocurrency.aplwallet.apl.util.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("com.apollocurrency.aplwallet.apl.util.Logger")
public class DownloaderTest {

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

        PowerMockito.mockStatic(Logger.class);
        PowerMockito.doNothing().when(Logger.class);

        UpdaterMediator updaterMediator = UpdaterMediator.getInstance();
        Downloader downloader = Downloader.getInstance();

        Path result = downloader.tryDownload("invalid-url", null, new ThrowingDownloadExecutor());

        Assert.assertNull(result);
        Assert.assertEquals(updaterMediator.getState(), UpdateInfo.DownloadState.FINISHED);
        Assert.assertEquals(updaterMediator.getStatus(), UpdateInfo.DownloadStatus.FAIL);

    }

    /**
     * test with successful download scenario
     */
    @Test
    public void testTryDownloadWithSignedValidPayload() {

        PowerMockito.mockStatic(Logger.class);
        PowerMockito.doNothing().when(Logger.class);

        Downloader downloader = Downloader.getInstance();
        UpdaterMediator updaterMediator = UpdaterMediator.getInstance();

        PowerMockito.mock(URL.class);

        Path result = downloader.tryDownload("testDownloaderPayload.txt", TEST_PAYLOAD_SHA256, new ResourceDownloadExecutor());

        Assert.assertNotNull(result);
        Assert.assertEquals(updaterMediator.getState(), UpdateInfo.DownloadState.FINISHED);
        Assert.assertEquals(updaterMediator.getStatus(), UpdateInfo.DownloadStatus.OK);

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
