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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
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

            log.debug("Created archive '{}', CRC/hash = [{}] within {} sec",
                    zipFile, zipCrcHash.length, (System.currentTimeMillis() - start) / 1000);
            return chunkedFileOps;
        } else {
            return new ChunkedFileOps("");
        }
    }

    @Override
    public long uncompressedSize(String zipFile) throws IOException {
        ZipFile file = new ZipFile(Paths.get(zipFile).toFile());
        Enumeration<? extends ZipEntry> e = file.entries();
        long uncompressedSize = 0;
        while (e.hasMoreElements()) {
            ZipEntry ze = e.nextElement();
            uncompressedSize += ze.getSize();
        }
        return uncompressedSize;
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

        // assign permanent zip file entry info
        FileTime ft;
        if (filesTimeFromEpoch != null) {
            ft = FileTime.fromMillis(filesTimeFromEpoch); // it's possible, but not quite correct
        } else {
            ft = FileTime.from(DEFAULT_BACK_TO_1970); // assign default, PREFERRED value!
        }
        Path inputDirPath = Paths.get(inputFolder);
        try {
            List<Path> filesToZip = collectFiles(inputDirPath, filenameFilter, recursive);
            if (filesToZip.isEmpty()) {
                return false;
            }
            try (FileOutputStream fos = new FileOutputStream(zipFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                zos.setComment("");
                zos.setLevel(ZIP_COMPRESSION_LEVEL);
                zos.setMethod(ZipOutputStream.DEFLATED);
                for (Path file : filesToZip) {
                    if (Files.isDirectory(file)) {
                        zipDir(zos, ft, inputDirPath, file);
                    } else {
                        zipFile(zos, ft, inputDirPath, file);
                    }
                }
                zos.finish();
                return true;
            }
        } catch (IOException e) {
            log.error("Error creating zip file: {}", zipFile, e);
            return false;
        }
    }

    List<Path> collectFiles(Path input,FilenameFilter filter, boolean recursive) throws IOException {
        List<Path> files = new ArrayList<>();
        if (recursive) {
            Files.walkFileTree(input, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (dir.equals(input)) {
                        return super.preVisitDirectory(dir, attrs);
                    }
                    files.add(dir);
                    return super.preVisitDirectory(dir, attrs);
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (filter != null && !filter.accept(file.getParent().toFile(), file.getFileName().toString())) {
                        return super.visitFile(file, attrs);
                    }
                    files.add(file);
                    return super.visitFile(file, attrs);
                }
            });
        } else {
            files.addAll(
                Files.list(input)
                    .filter(f -> !Files.isDirectory(f) && (filter == null || filter.accept(f.getParent().toFile(), f.getFileName().toString())))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .collect(Collectors.toList()));
        }
        return files;
    }

    private void zipDir(ZipOutputStream zos, FileTime ft, Path inputDir, Path dir) throws IOException {
        ZipEntry zipEntry = makeZipEntry(inputDir.relativize(dir).toString() + "/", ft);
        zos.putNextEntry(zipEntry);
        zos.closeEntry();
    }

    private void zipFile(ZipOutputStream zos, FileTime ft, Path inputDir, Path file) throws IOException {
        ZipEntry zipEntry = makeZipEntry(inputDir.relativize(file).toString(), ft);
        zos.putNextEntry(zipEntry);

        try (FileInputStream fis = new FileInputStream(file.toFile())) {
            byte[] buffer = new byte[BUF_SIZE];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
            zos.closeEntry();
        }
    }

    private ZipEntry makeZipEntry(String name, FileTime ft) {
        ZipEntry zipEntry = new ZipEntry(name);
        zipEntry.setCreationTime(ft);
        zipEntry.setLastAccessTime(ft);
        zipEntry.setLastModifiedTime(ft);
        zipEntry.setTime(ft.toMillis());
        zipEntry.setComment("");
        zipEntry.setMethod(ZipOutputStream.DEFLATED);
        return zipEntry;
    }

}
