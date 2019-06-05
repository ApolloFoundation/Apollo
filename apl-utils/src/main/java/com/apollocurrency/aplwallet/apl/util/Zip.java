 /*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Class is used for zip-unzip directories
 *
 * @author alukin@gmail.com
 */
public class Zip {
    private static final int BUF_SIZE= 1042 * 2; // 2 Mb
    
    private static final Logger log = LoggerFactory.getLogger(Zip.class);

    /**
    * Extract zip file into directory
    * @param zipFile zip file
    * @param outputFolder output directory
    * @return true if success
    */ 
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
 * zip directory into file, change file times. It time is the same zip will be
 * exactly the same
 * @param zipFile result zip file path
 * @param inputFolder folder to zip
 * @param filesTimeFromEpoch time in ms from Epoch for all file times
 * @param filenameFilter NULL or implemented instance
 * @return true if success
 */
    public boolean compress(String zipFile, String inputFolder, long filesTimeFromEpoch,
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
        boolean result = true;
        File directory = new File(inputFolder);
//        List<String> fileList = getFileList(directory, filenameFilter);
        File[] fileList = directory.listFiles(filenameFilter);
//        log.trace("Prepared [{}]={} files in in folder '{}', filenameFilter = {}", fileList.size(), Arrays.toString(fileList.toArray()) ,
        log.trace("Prepared [{}]={} files in in folder '{}', filenameFilter = {}",
                fileList != null ? fileList.length : -1, Arrays.toString(fileList) ,
                inputFolder, filenameFilter);

        try (FileOutputStream fos = new FileOutputStream(zipFile);
                ZipOutputStream zos = new ZipOutputStream(fos)) {

//            for (String filePath : fileList) {
            for (int i = 0; i < fileList.length; i++) {
                File file = fileList[i];
                String filePath = file.getAbsolutePath();

                String name = filePath.substring(directory.getAbsolutePath().length() + 1);
                log.trace("processing zip entry '{}' as file in '{}'...", name, filePath);
                ZipEntry zipEntry = new ZipEntry(name);
                FileTime ft = FileTime.fromMillis(filesTimeFromEpoch);
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
                    result = false;
                    log.error("Error creating zip file: {}", zipFile, e);
                }
            }
        } catch (IOException e) {
            result = false;
            log.error("Error creating zip file: {}", zipFile, e);
        }
//        log.debug("Created archive '{}' with [{}] file(s) within {} sec", zipFile, fileList.size(),
        log.debug("Created archive '{}' with [{}] file(s) within {} sec", zipFile,
                fileList != null ? fileList.length : -1,
                (System.currentTimeMillis() - start) / 1000 );
        return result;
    }

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
