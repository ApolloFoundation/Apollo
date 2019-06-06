 /*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

import javax.inject.Singleton;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Class is used for zip-unzip filtered files in specified directory
 *
 * @author alukin@gmail.com
 */
@Singleton
public class ZipImpl implements Zip {
    private static final Logger log = LoggerFactory.getLogger(ZipImpl.class);

    public final static int FILE_CHUNK_SIZE = 32768; // magic constant copied from DownloadableFilesManager class
    private final static int BUF_SIZE= 1042 * 2; // 2 Mb
    public static Instant DEFAULT_BACK_TO_1970 = Instant.EPOCH; // in past
    public static FilenameFilter DEFAULT_CSV_FILE_FILTER = new SuffixFileFilter(".csv"); // CSV files only

    public ZipImpl() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean extract(String zipFile, String outputFolder) {
        Objects.requireNonNull(zipFile, "zipFile is NULL");
        Objects.requireNonNull(outputFolder, "outputFolder is NULL");
        if (zipFile.isEmpty()) {
            throw new IllegalArgumentException("'zipFile' value is empty");
        }
        if (outputFolder.isEmpty()) {
            throw new IllegalArgumentException("'outputFolder' value is empty");
        }
        log.trace("Extracting file '{}' from outputFolder '{}'", zipFile, outputFolder);
        boolean res = true;
        byte[] buffer = new byte[BUF_SIZE];

        try {

            //create output directory is not exists
            File folder = new File(outputFolder);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            //get the zipped file list entry
            try ( //get the zip file content
                ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
                //get the zipped file list entry
                ZipEntry ze = zis.getNextEntry();

                while (ze != null) {

                    String fileName = ze.getName();
                    File newFile = new File(outputFolder + File.separator + fileName);

                    //create all non exists folders
                    //else you will hit FileNotFoundException for compressed folder
                    new File(newFile.getParent()).mkdirs();

                    if (ze.isDirectory()) {
                        newFile.mkdirs();
                    } else {
                        try (FileOutputStream fos = new FileOutputStream(newFile)) {
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                    }
                    ze = zis.getNextEntry();
                }

                zis.closeEntry();
            }

        } catch (IOException ex) {
            log.error("Error extractiong zip file: {}", zipFile, ex);
            res = false;
        }
        return res;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] compress(String zipFile, String inputFolder, Long filesTimeFromEpoch,
                            FilenameFilter filenameFilter) {
        Objects.requireNonNull(zipFile, "zipFile is NULL");
        Objects.requireNonNull(inputFolder, "inputFolder is NULL");
        if (zipFile.isEmpty()) {
            throw new IllegalArgumentException("'zipFile' value is empty");
        }
        if (inputFolder.isEmpty()) {
            throw new IllegalArgumentException("'inputFolder' value is empty");
        }
        long start = System.currentTimeMillis();
        log.trace("Creating file '{}' in folder '{}', filesTimestamp = {}", zipFile, inputFolder, filesTimeFromEpoch);
        byte[] zipCrcHash;
        File directory = new File(inputFolder);
        if (filenameFilter == null) {
            filenameFilter = DEFAULT_CSV_FILE_FILTER;
        }
        // get file(s) listing from folder by filter (no recursion for subfolders(s) !)
        File[] fileList = directory.listFiles(filenameFilter);
        log.trace("Prepared [{}]={} files in in folder '{}', filenameFilter = {}",
                fileList != null ? fileList.length : -1, Arrays.toString(fileList) ,
                inputFolder, filenameFilter);
        // throw exception because it's a error/failure in case sharding process
        if (fileList == null || fileList.length <= 0) {
            String error = String.format(
                    "Error on creating CSV zip archive, no csv file(s) were found in folder '%s' !", inputFolder);
            log.error(error);
            throw new RuntimeException(error);
        }

        // assign permanent zip file entry info
        FileTime ft;
        if (filesTimeFromEpoch != null) {
            ft = FileTime.fromMillis(filesTimeFromEpoch); // it's possible, but not quite correct
        } else {
            ft = FileTime.from(DEFAULT_BACK_TO_1970); // assign default, PREFERRED value!
        }

        try (FileOutputStream fos = new FileOutputStream(zipFile);
                ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (int i = 0; i < fileList.length; i++) {
                File file = fileList[i];
                String filePath = file.getAbsolutePath();

                String name = filePath.substring(directory.getAbsolutePath().length() + 1);
                log.trace("processing zip entry '{}' as file in '{}'...", name, filePath);
                ZipEntry zipEntry = new ZipEntry(name);
                zipEntry.setCreationTime(ft);
                zipEntry.setLastAccessTime(ft);
                zipEntry.setLastModifiedTime(ft);
                zos.putNextEntry(zipEntry);

                try (FileInputStream fis = new FileInputStream(filePath)) {
                    byte[] buffer = new byte[BUF_SIZE];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                    log.trace("closed zip entry '{}'", name);
                } catch (Exception e) {
                    log.error("Error creating zip file: {}", zipFile, e);
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException e) {
            log.error("Error creating zip file: {}", zipFile, e);
            throw new RuntimeException(e);
        }

        // compute CRC/hash sum on zip file
        ChunkedFileOps chunkedFileOps = new ChunkedFileOps(zipFile);
        zipCrcHash = chunkedFileOps.getFileHashSums(FILE_CHUNK_SIZE);

        log.debug("Created archive '{}' with [{}] file(s), CRC/hash = [{}] within {} sec",
                zipFile, zipCrcHash.length,
                fileList.length, (System.currentTimeMillis() - start) / 1000 );
        return zipCrcHash;
    }

    // Incorrect recursive listing, not used, can be removed later
    private List<String> getFileList(File directory, FilenameFilter filenameFilter) {
        List<String> fileList = new ArrayList<>();

        File[] files;
        if (filenameFilter != null) {
            files = directory.listFiles();
        } else {
            files = directory.listFiles(filenameFilter);
        }
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isFile()) {
                    fileList.add(file.getAbsolutePath());
                } else {
                    getFileList(file, filenameFilter);
                }
            }
        }
        log.debug("Gathered [{}] files with filter = {}", files.length, filenameFilter);
        return fileList;
    }

}
