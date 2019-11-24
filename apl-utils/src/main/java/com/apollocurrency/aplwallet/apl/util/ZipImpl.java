 /*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;

 import javax.inject.Singleton;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileOutputStream;
 import java.io.FilenameFilter;
 import java.io.IOException;
 import java.nio.file.attribute.FileTime;
 import java.time.Instant;
 import java.util.ArrayList;
 import java.util.Comparator;
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

    // magic constant copied from DownloadableFilesManager class
    private final static int BUF_SIZE= 1024 * 16; // 16 Kb
    public static Instant DEFAULT_BACK_TO_1970 = Instant.EPOCH; // in past
    private List<File> fileList = new ArrayList<>();
    private static final int ZIP_COMPRESSION_LEVEL=9;
    
    public ZipImpl() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean extract(String zipFile, String outputFolder) {
        Objects.requireNonNull(zipFile, "zipFile is NULL");
        Objects.requireNonNull(outputFolder, "outputFolder is NULL");
        StringValidator.requireNonBlank(zipFile);
        StringValidator.requireNonBlank(outputFolder);

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
            log.error("Error extracting zip file: {}", zipFile, ex);
            res = false;
        }
        return res;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ChunkedFileOps compressAndHash(String zipFile, String inputFolder, Long filesTimeFromEpoch,
                            FilenameFilter filenameFilter, boolean recursive) {
        long start = System.currentTimeMillis();
        boolean compressed = compress(zipFile, inputFolder, filesTimeFromEpoch, filenameFilter, recursive);
        if (compressed) {
            // compute CRC/hash sum on zip file
            ChunkedFileOps chunkedFileOps = new ChunkedFileOps(zipFile);
            byte[] zipCrcHash = chunkedFileOps.getFileHashSums();

            log.debug("Created archive '{}' with [{}] file(s), CRC/hash = [{}] within {} sec",
                    zipFile, zipCrcHash.length,
                    fileList.size(), (System.currentTimeMillis() - start) / 1000);
            return chunkedFileOps;
        } else {
            ChunkedFileOps res = new ChunkedFileOps("");
            return res;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean compress(String zipFile, String inputFolder, Long filesTimeFromEpoch,
                            FilenameFilter filenameFilter, boolean recursive) {
        Objects.requireNonNull(zipFile, "zipFile is NULL");
        Objects.requireNonNull(inputFolder, "inputFolder is NULL");
        StringValidator.requireNonBlank(zipFile);
        StringValidator.requireNonBlank(inputFolder);

        log.trace("Creating file '{}' in folder '{}', filesTimestamp = {}", zipFile, inputFolder, filesTimeFromEpoch);
        File directory = new File(inputFolder);
        // get file(s) listing from folder by filter (no recursion for subfolders(s) !)
        List<File> fl = getFileList(directory, filenameFilter, recursive);

        // throw exception because it's a error/failure in case sharding process
        if (fl.size() == 0) {
            log.warn("Zip will not created, no files found");
            return false;
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
                zos.setComment("");
                zos.setLevel(ZIP_COMPRESSION_LEVEL);
                zos.setMethod(ZipOutputStream.DEFLATED);
                
            for (File file: fl) {

                String name = file.getName();
                log.trace("processing zip entry '{}' as file ...", name);
                ZipEntry zipEntry = new ZipEntry(name);
                zipEntry.setCreationTime(ft);
                zipEntry.setLastAccessTime(ft);
                zipEntry.setLastModifiedTime(ft);
                zipEntry.setTime(ft.toMillis());
                zipEntry.setComment("");
                zipEntry.setMethod(ZipOutputStream.DEFLATED);
                zos.putNextEntry(zipEntry);

                try (FileInputStream fis = new FileInputStream(file)) {
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
            zos.finish();
            return true;
        } catch (IOException e) {
            log.error("Error creating zip file: {}", zipFile, e);
            return false;
        }
    }
    
    private List<File> getFileList(File directory, FilenameFilter filenameFilter, boolean recursive) {

        fileList.clear();
        File[] files;
        if (filenameFilter == null) {
            files = directory.listFiles();
        } else {
            files = directory.listFiles(filenameFilter);
        }
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isFile()) {
                    fileList.add(file);
                } else if(recursive){
                    getFileList(file, filenameFilter, recursive);
                }
            }
        }
        //sort by simple name to avoid different order in zip
        fileList.sort(Comparator.comparing(File::getName));

        log.debug("Gathered [{}] files with filter = {}", files != null?  files.length : -1, filenameFilter);
        return fileList;
    }

}
