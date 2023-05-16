/*
 *  Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util;

import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;

public class FileUtils {

    private static final Logger log = getLogger(FileUtils.class);
    private static final int READ_BUFFER_SIZE = 16384;

    private FileUtils() {
    }

    public static boolean deleteFileIfExistsQuietly(Path file) {
        try {
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            log.error("Unable to delete file {}, cause: {}", file, e.getMessage());
        }
        return false;
    }

    public static boolean deleteFileIfExistsAndHandleException(Path file, Consumer<IOException> handler) {
        try {
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            handler.accept(e);
        }
        return false;
    }

    public static boolean deleteFileIfExists(Path file) {
        try {
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static void clearDirectorySilently(Path directory) {
        if (!Files.isDirectory(directory) || !Files.exists(directory)) {
            return;
        }
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        if (!dir.equals(directory)) {
                            Files.delete(dir);
                        }
                        return super.postVisitDirectory(dir, exc);
                    }
                }
            );
        } catch (IOException e) {
            log.error("Unable to delete dir {}", directory);
        }
    }

    public static void deleteFilesByPattern(Path directory, String[] suffixes, String[] names) {
        if (!Files.isDirectory(directory) || !Files.exists(directory)) {
            return;
        }
        try (Stream<Path> pathStream = Files.list(directory)) {
            pathStream.filter(p -> {
                boolean match = names == null;
                if (!match) {
                    for (String name : names) {
                        if (p.toAbsolutePath().toString().contains(name)) {
                            match = true;
                            break;
                        }
                    }
                }
                if (!match) {
                    return false;
                }
                match = suffixes == null;
                if (!match) {

                    for (String suffix : suffixes) {
                        if (p.toAbsolutePath().toString().endsWith(suffix)) {
                            return true;
                        }
                    }
                } else {
                    return true;
                }
                return false;
            }).forEach(FileUtils::deleteFileIfExistsQuietly);
        } catch (IOException e) {
            log.error("Unable to delete dir {}", directory);
        }
    }

    public static void deleteFilesByFilter(Path directory, Predicate<Path> predicate) {
        if (!Files.isDirectory(directory) || !Files.exists(directory)) {
            return;
        }
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(predicate).forEach(FileUtils::deleteFileIfExistsQuietly);
        } catch (IOException e) {
            log.error("Unable to delete dir {}", directory);
        }
    }

    public static long countElementsOfDirectory(Path directory) throws IOException {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.count();
        }
    }

    public static long freeSpace() {
        return new File("/").getFreeSpace();
    }

    public static long webFileSize(String url) throws IOException {
        return new URL(url).openConnection().getContentLengthLong();
    }

    public static byte[] hashFile(Path file, MessageDigest digest) throws IOException {
        try (DigestInputStream in = new DigestInputStream(Files.newInputStream(file), digest)) {
            byte[] block = new byte[READ_BUFFER_SIZE];
            while (in.read(block) > 0) {
            }
            return in.getMessageDigest().digest();
        }
    }

    public static long countElementsOfDirectory(Path directory, Predicate<Path> predicate) throws IOException {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.filter(predicate).count();
        }
    }

    /**
     * Search directory for entries that match part of name and is config entity
     * (directory, zip or jar file)
     *
     * @param location directory to search
     * @param namePart name to search for
     * @return entity found. Usually should be empty or 1 entry. In there are
     * more then 1 entries, it means that namePart is too short or there are
     * several entities that match. Firs entity is tken and warning emitted
     * Empty string means nothing found
     */
    public static List<String> searchByNamePart(String location, String namePart) {
        List<String> res = new ArrayList<>();
        //TODO: implement
        return null;
    }
}
