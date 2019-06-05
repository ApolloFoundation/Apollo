package com.apollocurrency.aplwallet.apl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;

@EnableWeld
class ZipTest {
    private static final Logger log = getLogger(ZipTest.class);

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
        String zipFileName = "test-archive-csv-1.zip";
        String zipFileInPath = folderWithZip + File.separator + zipFileName;
        // start creating zip for all CSV
        boolean isCompressed = zipComponent.compress(zipFileInPath, csvResourcesPath.toAbsolutePath().toString(),
                null, null);
        assertTrue(isCompressed, "CSV files were NOT compressed into ZIP!!");

        String[] extensions = new String[]{"zip"};
        // check ZIP is created
        Collection filesInFolder = FileUtils.listFiles(temporaryFolderExtension.getRoot().toPath().toFile(), extensions, false);
        assertNotNull(filesInFolder);
        assertEquals(1, filesInFolder.size());

        // extract all csv files from ZIP and check CSV content
        boolean isExtracted = zipComponent.extract(zipFileInPath, folderWithZip);
        assertTrue(isExtracted, "CSV files were NOT extracted from ZIP!!");
        extensions = new String[]{"csv"};
        int csvFilesNumber = 0;
        Iterator iterator = FileUtils.iterateFiles(temporaryFolderExtension.getRoot().toPath().toFile(), extensions, false);
        while (iterator.hasNext()) {
            Object csvFile = iterator.next();
            log.debug("Reading extracted CSV '{}'", csvFile);
            File file = new File(csvFile.toString());
            // check CSV content
            List lines = FileUtils.readLines(file);
            assertTrue(lines.size() > 1, "CSV file" + csvFile + " is EMPTY !");
            csvFilesNumber++; // count CSV files
        }
        assertEquals(7, csvFilesNumber);
    }

    @Test
    void tryToCompressEmptyFolder() {
        File folderNoCsvInside = temporaryFolderExtension.getRoot().toPath().toFile();
        String zipFileName = "test-archive-csv-1.zip";
        String zipFileInPath = folderNoCsvInside + File.separator + zipFileName;
        assertThrows(RuntimeException.class, () ->
                zipComponent.compress(zipFileInPath, folderNoCsvInside.getAbsolutePath(), null, null)
        );
    }

    @Test
    void incorrectParamsCall() {
        assertThrows(NullPointerException.class, () -> zipComponent.compress(
                null, "", -1L, null));

        assertThrows(NullPointerException.class, () -> zipComponent.compress(
                "", null, -1L, null));

        assertThrows(IllegalArgumentException.class, () -> zipComponent.compress(
                "", csvResourcesPath.toAbsolutePath().toString(), null, null));

        assertThrows(IllegalArgumentException.class, () -> zipComponent.compress(
                csvResourcesPath.toAbsolutePath().toString(), "", null, null));

        assertThrows(NullPointerException.class, () -> zipComponent.extract(null, ""));

        assertThrows(NullPointerException.class, () -> zipComponent.extract("", null));

        assertThrows(IllegalArgumentException.class, () -> zipComponent.extract("", csvResourcesPath.toAbsolutePath().toString()));

        assertThrows(IllegalArgumentException.class, () -> zipComponent.extract(csvResourcesPath.toAbsolutePath().toString(), ""));
    }
}