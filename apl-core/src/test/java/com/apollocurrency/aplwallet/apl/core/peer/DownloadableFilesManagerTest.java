package com.apollocurrency.aplwallet.apl.core.peer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.slf4j.LoggerFactory.getLogger;

import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import java.nio.file.Path;

import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.testutil.ResourceFileLoader;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

@EnableWeld
class DownloadableFilesManagerTest {
    private static final Logger log = getLogger(DownloadableFilesManagerTest.class);
    private Path csvResourcesPath = new ResourceFileLoader().getResourcePath().toAbsolutePath();
    private final Bean<Path> dataExportDir = MockBean.of(csvResourcesPath, Path.class);
    private DirProvider dirProvider = mock(DirProvider.class);
    {
        doReturn(csvResourcesPath).when(dirProvider).getDataExportDir();
    }

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(DownloadableFilesManager.class)
            .addBeans(MockBean.of(dirProvider, DirProvider.class))
            .build();

    String zipFileName = "apl-blockchain-arch-1.zip";

    @Inject
    private DownloadableFilesManager filesManager;

    @Test
    void getFileDownloadInfo() {
        // create ZIP in temp folder for unit test
        FileDownloadInfo fi = filesManager.getFileDownloadInfo(zipFileName);
        assertNotNull(fi);
        log.debug("File download Info = {}", fi);
        assertEquals(
                "c8d3a0c3d323366c711e4d05d5706db27da385a0181ae7584013b641c2b97221",
                fi.fileInfo.hash);
        log.debug("Parsed bytes from string = {}", Convert.parseHexString(fi.fileInfo.hash) );
        assertEquals(zipFileName, fi.fileInfo.fileId);
    }

    @Test
    void getMissingResource() {
        String zipFileName = "MISSING-archive.zip";

        FileDownloadInfo fi = filesManager.getFileDownloadInfo(zipFileName);
        assertNull(fi);
    }

    @Test
    void mapFileIdToLocalPath() {
        // parse real/existing shard name + ID
        Path pathToShardArchive = filesManager.mapFileIdToLocalPath("shard::1");
        assertNotNull(pathToShardArchive);
        assertEquals(zipFileName, pathToShardArchive.getFileName().toString());

        // parse simple file name
        pathToShardArchive = filesManager.mapFileIdToLocalPath("phasing_poll.csv");
        assertNotNull(pathToShardArchive);
        assertEquals("phasing_poll.csv", pathToShardArchive.getFileName().toString());

        pathToShardArchive = filesManager.mapFileIdToLocalPath("shard::");
        assertNull(pathToShardArchive);

        assertThrows(NullPointerException.class, () -> filesManager.mapFileIdToLocalPath(null));

        pathToShardArchive = filesManager.mapFileIdToLocalPath("");
        assertNull(pathToShardArchive);
    }
}