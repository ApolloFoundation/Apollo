package com.apollocurrency.aplwallet.apl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.inject.Inject;

@EnableWeld
class ZipTest {
    private static final Logger log = getLogger(ZipTest.class);
    private static final String APL_BLOCKCHAIN_ARCH_1_ZIP_HASH = "f3b51cb318c7de39c345ba6344f2bb0068a2627e92a8d6466a7a98bf3fd3e1a2";
    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(ZipImpl.class)
            .build();

    @Inject
    private Zip zipComponent;
    private Path csvResourcesPath;

    @BeforeEach
    void setUp() {
        ResourceFileLoader resourceFileLoader = new ResourceFileLoader();
        csvResourcesPath = resourceFileLoader.getResourcePath();
        assertNotNull(csvResourcesPath);
    }

    @Test
    void compressAndExtractFiles() throws Exception {
        // create ZIP in temp folder for unit test
        String folderWithZip = temporaryFolderExtension.getRoot().toPath().toFile().getAbsolutePath();
        log.debug("Create zip into = '{}'", folderWithZip);
        String zipFileName = "test-archive-csv-1.zip";
        String zipFileInPath = folderWithZip + File.separator + zipFileName;
        log.debug("Create zip full name = '{}'", zipFileInPath);
        // start creating zip for all CSV
        FilenameFilter CSV_FILE_FILTER = new SuffixFileFilter(".csv"); // CSV files only
        ChunkedFileOps chunkedFileOps = zipComponent.compressAndHash(zipFileInPath, csvResourcesPath.toAbsolutePath().toString(),
                null, CSV_FILE_FILTER, false);
        byte[] zipCrc = chunkedFileOps.getFileHash();
        assertTrue(zipCrc != null && zipCrc.length > 0, "CSV files were NOT compressed into ZIP!!");

        String[] zipExtension = new String[]{"zip"};
        // check ZIP is created
        Collection filesInFolder = FileUtils.listFiles(temporaryFolderExtension.getRoot().toPath().toFile(), zipExtension, false);
        assertNotNull(filesInFolder);
        assertEquals(1, filesInFolder.size());// first zip is created, the second is


        // extract all csv files from ZIP and check CSV content
        String anotherFolderExtract = temporaryFolderExtension.getRoot().toPath().toFile().getAbsolutePath()
                + File.separator + "EXTRACT_TO";
        log.debug("Extract zip into another folder = '{}'", anotherFolderExtract);

        log.debug("Copy zip '{}' into another folder '{}'...", zipFileInPath, anotherFolderExtract);
        FileUtils.copyFileToDirectory(new File( zipFileInPath), new File( anotherFolderExtract));

        // zip file was copied into another folder
        String anotherZipToExtract = anotherFolderExtract + File.separator + zipFileName;

        boolean isExtracted = zipComponent.extract(anotherZipToExtract, anotherFolderExtract);
        assertTrue(isExtracted, "CSV files were NOT extracted from ZIP!!");

        int csvFilesNumber = 0;
        Iterator iterator = FileUtils.iterateFiles(new File(anotherFolderExtract), null, false);
        while (iterator.hasNext()) {
            Object csvFile = iterator.next();
            log.debug("Reading extracted CSV '{}'", csvFile);
            assertNotNull(csvFile);
            File file = new File(csvFile.toString());
            if (csvFile.toString().endsWith(".zip")) {
                // that will trigger if there is 'another-archive-1.zip' or something else like that is in folder
                assertTrue(csvFile.toString().contains(zipFileName), // only source zip file name
                        "Another file ZIP should not be present inside ZIP, but there is file = " + csvFile.toString());
            }
            // check CSV content
            List lines = FileUtils.readLines(file);
            assertTrue(lines.size() > 1, "CSV file" + csvFile + " is EMPTY !");
            csvFilesNumber++; // count all files in folder
        }
        assertEquals(8, csvFilesNumber); // count CSV files + original ZIP
    }

    @Test
    void tryToCompressEmptyFolder() {
        File folderNoCsvInside = temporaryFolderExtension.getRoot().toPath().toFile();
        String zipFileName = "test-archive-csv-1.zip";
        String zipFileInPath = folderNoCsvInside + File.separator + zipFileName;
        assertFalse(zipComponent.compress(zipFileInPath, folderNoCsvInside.getAbsolutePath(), null, null,false));
    }

    @Test
    void testCalculateHash() throws URISyntaxException {

        URL zipUrl = getClass().getClassLoader().getResource("another-archive-1.zip");
        ChunkedFileOps ops = new ChunkedFileOps(Paths.get(zipUrl.toURI()).toAbsolutePath().toString());
        byte[] hash = ops.getFileHash();

        String hexHash = Convert.toHexString(hash);

        assertEquals(APL_BLOCKCHAIN_ARCH_1_ZIP_HASH, hexHash);
    }

    @Test
    void incorrectParamsCall() {
        assertThrows(NullPointerException.class, () -> zipComponent.compress(
                null, "", -1L, null,false));

        assertThrows(NullPointerException.class, () -> zipComponent.compress(
                "", null, -1L, null,false));

        assertThrows(IllegalArgumentException.class, () -> zipComponent.compress(
                "", csvResourcesPath.toAbsolutePath().toString(), null, null, false));

        assertThrows(IllegalArgumentException.class, () -> zipComponent.compress(
                csvResourcesPath.toAbsolutePath().toString(), "", null, null,false));

        assertThrows(NullPointerException.class, () -> zipComponent.extract(null, ""));

        assertThrows(NullPointerException.class, () -> zipComponent.extract("", null));

        assertThrows(IllegalArgumentException.class, () -> zipComponent.extract("", csvResourcesPath.toAbsolutePath().toString()));

        assertThrows(IllegalArgumentException.class, () -> zipComponent.extract(csvResourcesPath.toAbsolutePath().toString(), ""));
    }
}