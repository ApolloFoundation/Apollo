package com.apollocurrency.aplwallet.apl.util;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.slf4j.LoggerFactory.getLogger;

@EnableWeld
class ZipTest {
    private static final Logger log = getLogger(ZipTest.class);
    private static final String APL_BLOCKCHAIN_ARCH_1_ZIP_HASH = "f3b51cb318c7de39c345ba6344f2bb0068a2627e92a8d6466a7a98bf3fd3e1a2";

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
    void compressAndExtractFiles(@TempDir Path dir) throws Exception {
        // create ZIP in temp folder for unit test
        String folderWithZip = dir.toAbsolutePath().toString();
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
        Collection filesInFolder = FileUtils.listFiles(dir.toFile(), zipExtension, false);
        assertNotNull(filesInFolder);
        assertEquals(1, filesInFolder.size());// first zip is created, the second is


        // extract all csv files from ZIP and check CSV content
        String anotherFolderExtract = dir.toFile().getAbsolutePath()
                + File.separator + "EXTRACT_TO";
        log.debug("Extract zip into another folder = '{}'", anotherFolderExtract);

        log.debug("Copy zip '{}' into another folder '{}'...", zipFileInPath, anotherFolderExtract);
        FileUtils.copyFileToDirectory(new File( zipFileInPath), new File( anotherFolderExtract));

        // zip file was copied into another folder
        String anotherZipToExtract = anotherFolderExtract + File.separator + zipFileName;

        boolean isExtracted = zipComponent.extract(anotherZipToExtract, anotherFolderExtract, true);
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
    void testCalculateArchiveSize() throws IOException, URISyntaxException {
        long uncompressedSize = zipComponent.uncompressedSize(new File(getClass().getClassLoader().getResource("another-archive-1.zip").toURI()).toString());

        assertEquals(78_900, uncompressedSize);
    }

    @Test
    void tryToCompressEmptyFolder() throws IOException {
        Path folderNoCsvInside = Files.createTempDirectory("csvFolder");
        Path zipDir = Files.createTempDirectory("tempZipDir");
        try {
            String zipFileName = "test-archive-csv-1.zip";
            String zipFileInPath = zipDir + File.separator + zipFileName;

            boolean compressed = zipComponent.compress(zipFileInPath, folderNoCsvInside.toAbsolutePath().toString(), null, null, false);

            assertFalse(compressed);
            assertFalse(Files.exists(Paths.get(zipFileInPath)));
        } finally {
            Files.deleteIfExists(zipDir);
            Files.deleteIfExists(folderNoCsvInside);
        }
    }

    @Test
    void testCompressExtractRecursive(@TempDir Path tempDir) throws IOException {
        doTestCompressExtractRecursive(tempDir, true);
    }

    @Test
    void testCompressExtractRecursive_delete_zip(@TempDir Path tempDir) throws IOException {
        doTestCompressExtractRecursive(tempDir, false);
        Path zip = tempDir.resolve("target.zip");
        assertFalse(Files.exists(zip));
    }

    void doTestCompressExtractRecursive(Path tempDir, boolean keepZip) throws IOException {
        Path dir = tempDir.resolve("dirToZip");
        List<Path> filesToCompress = prepareFiles(dir);

        Path extractDir = tempDir.resolve("extractDir");
        Path zipFile = tempDir.resolve("target.zip");

        boolean compress = zipComponent.compress(zipFile.toAbsolutePath().toString(), dir.toAbsolutePath().toString(), null, null, true);

        assertTrue(compress);
        assertTrue(Files.exists(zipFile));

        boolean extracted = zipComponent.extract(zipFile.toAbsolutePath().toString(), extractDir.toAbsolutePath().toString(), keepZip);

        verifyExtraction(extracted, filesToCompress, extractDir);
    }

    private void verifyExtraction(boolean extracted, List<Path> filesToCompress, Path extractDir) {
        assertTrue(extracted);
        filesToCompress.forEach(f -> {
            Path extractedFile = extractDir.resolve(extractDir.relativize(f));
            assertTrue(Files.exists(extractedFile));
            try {
                assertEquals(Files.readAllLines(f), Files.readAllLines(extractedFile));
            } catch (IOException e) {
                fail(e);
            }
        });
    }

    private List<Path> prepareFiles(Path dir) throws IOException {
        Path file1 = dir.resolve("dir/1.txt");
        Files.createDirectories(file1.getParent());
        Files.write(file1, "Content 1".getBytes());
        Path file2 = dir.resolve("2.txt");
        Files.write(file2, "Content 2".getBytes());
        Path file3 = dir.resolve("dd/dd/dd/3.txt");
        Files.createDirectories(file3.getParent());
        Files.write(file3, "Content 3".getBytes());
        Path file4 = dir.resolve("dd/dd/dd/4.txt");
        Files.write(file4, "Content 4".getBytes());
        Path file5 = dir.resolve("dd/dd1/5.txt");
        Files.createDirectories(file5.getParent());
        Files.write(file5, "Content 5".getBytes());
        return List.of(file1, file2, file3, file4, file5);
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

        assertThrows(NullPointerException.class, () -> zipComponent.extract(null, "", true));

        assertThrows(NullPointerException.class, () -> zipComponent.extract("", null, false));

        assertThrows(IllegalArgumentException.class, () -> zipComponent.extract("", csvResourcesPath.toAbsolutePath().toString(), true));

        assertThrows(IllegalArgumentException.class, () -> zipComponent.extract(csvResourcesPath.toAbsolutePath().toString(), "", true));
    }
}