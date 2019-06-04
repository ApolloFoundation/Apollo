 /*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * zip-unziop directorys
 * @author alukin@gmail.com
 */
public class Zip {
    private static final int BUF_SIZE=8192;
    
    private static final Logger log = LoggerFactory.getLogger(Zip.class);

    /**
    * Extract zip file into directory
    * @param zipFile zip file
    * @param outputFolder output directory
    * @return true if success
    */ 
    public boolean extract(String zipFile, String outputFolder) {
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
 * @return true if success
 */
    public boolean compress(String zipFile, String inputFolder, long filesTimeFromEpoch) {
        boolean res = true;
        File directory = new File(inputFolder);
        List<String> fileList = getFileList(directory);

        try (FileOutputStream fos = new FileOutputStream(zipFile);
                ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (String filePath : fileList) {

                String name = filePath.substring(
                        directory.getAbsolutePath().length() + 1,
                        filePath.length());

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
                } catch (Exception e) {
                    res = false;
                    log.error("Error creating zip file: {}", zipFile, e);
                }
            }
        } catch (IOException e) {
            res = false;
            log.error("Error creating zip file: {}", zipFile, e);
        }
        return res;
    }

    private List<String> getFileList(File directory) {
        List<String> fileList = new ArrayList<>();

        File[] files = directory.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isFile()) {
                    fileList.add(file.getAbsolutePath());
                } else {
                    getFileList(file);
                }
            }
        }
        return fileList;
    }

}
