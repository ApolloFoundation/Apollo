package com.apollocurrency.aplwallet.apl.core.peer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.nio.file.Path;
import java.util.UUID;

import com.apollocurrency.aplwallet.api.p2p.FileDownloadInfo;
import com.apollocurrency.aplwallet.apl.testutil.ResourceFileLoader;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProviderFactory;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.PredefinedDirLocations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class DownloadableFilesManagerTest {
    private static final Logger log = getLogger(DownloadableFilesManagerTest.class);

    private DownloadableFilesManager filesManager;
    private Path csvResourcesPath;

    @BeforeEach
    void setUp() {
        ResourceFileLoader resourceFileLoader = new ResourceFileLoader();
        csvResourcesPath = resourceFileLoader.getResourcePath(); // default test resource folder
        assertNotNull(csvResourcesPath);
        PredefinedDirLocations dirLocations = new PredefinedDirLocations(
                csvResourcesPath.toAbsolutePath().toString(),
                null, null, null, null,
                csvResourcesPath.toAbsolutePath().toString());
        DirProviderFactory.setup(true, UUID.randomUUID(), "Default", dirLocations);
        DirProvider dirProvider = DirProviderFactory.getProvider();
        filesManager = new DownloadableFilesManager(dirProvider);
    }

    @Test
    void getFileDownloadInfo() {
        // create ZIP in temp folder for unit test
        String zipFileName = "test-csv-archive-1.zip";

        FileDownloadInfo fi = filesManager.getFileDownloadInfo(zipFileName);
        assertNotNull(fi);
        log.debug("File download Info = {}", fi);
        assertEquals(
                "c8d3a0c3d323366c711e4d05d5706db27da385a0181ae7584013b641c2b97221",
                fi.fileInfo.hash);
        assertEquals(zipFileName, fi.fileInfo.fileId);
    }

    @Test
    void getMissingResource() {
        String zipFileName = "MISSING-archive.zip";

        FileDownloadInfo fi = filesManager.getFileDownloadInfo(zipFileName);
        assertNull(fi);
    }
}