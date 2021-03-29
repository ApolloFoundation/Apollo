/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Read/Write file by chunks
 *
 * @author alukin@gmail.com
 */
@Slf4j
public class ChunkedFileOps {
    public static final int FILE_CHUNK_SIZE = 32768;
    public static final String DIGESTER = "SHA-256";
    private final List<ChunkInfo> fileCRCs = new ArrayList<>();
    @Getter
    @Setter
    private String fileId;
    @Getter
    private Path absPath;
    private Long lastRDChunkCrc;
    private Long lastWRChunkCrc;

    private byte[] fileHash = null;
    public ChunkedFileOps(String absPath) {
        this(Paths.get(absPath));
    }

    public ChunkedFileOps(Path absPath) {
        this.absPath = absPath;
    }

    public boolean isHashedOK() {
        return fileHash != null;
    }

    public void moveFile(Path target) throws IOException {
        absPath = Files.move(absPath, target, StandardCopyOption.REPLACE_EXISTING);
    }

    public byte[] getFileHash() {
        if (fileHash == null) {
            getFileHashSums(FILE_CHUNK_SIZE);
        }
        return fileHash;
    }

    public synchronized void writeChunk(Long offset, byte[] data, long crc) throws IOException {
        int res = 0;
        CheckSum cs = new CheckSum();
        cs.update(data);
        lastWRChunkCrc = cs.finish();
        if (lastWRChunkCrc != crc) {
            throw new BadCheckSumException(absPath.toString());
        }
        if (!absPath.getParent().toFile().exists()) {
            absPath.getParent().toFile().mkdirs();
        }
        try (RandomAccessFile rf = new RandomAccessFile(absPath.toFile(), "rw")) {
            rf.seek(offset);
            rf.write(data);
        } catch (IOException e) {
            log.error("Can not write file: {}", absPath.toAbsolutePath().toString());
            throw e;
        }
    }

    public int readChunk(Long offset, Long size, byte[] dataBuf) throws IOException {
        int res;
        if (!absPath.toFile().exists()) {
            res = -2;
            return res;
        }
        try (RandomAccessFile rf = new RandomAccessFile(absPath.toFile(), "r")) {
            rf.skipBytes(offset.intValue());
            res = rf.read(dataBuf, 0, size.intValue());
        }
        CheckSum cs = new CheckSum();
        cs.update(dataBuf, size.intValue());
        lastRDChunkCrc = cs.finish();
        return res;
    }

    public long getLastWRChunkCrc() {
        return lastWRChunkCrc;
    }

    public long getLastRDChunkCrc() {
        return lastRDChunkCrc;
    }

    public long getFileSize() {
        long res = -1L;
        try {
            BasicFileAttributes attrs = Files.readAttributes(absPath, BasicFileAttributes.class);
            res = attrs.size();
        } catch (IOException ignored) {
        }
        return res;
    }

    public byte[] getFileHashSums() {
        return getFileHashSums(FILE_CHUNK_SIZE);
    }

    /**
     * Calculates file hash and partial CRCss
     * Should depends on crypto settings, but at the time it is SHA-256
     *
     * @param chunkSize size of file chunks to calculate partial CRCs
     * @return Crypto hash sum of entire file
     */
    public byte[] getFileHashSums(int chunkSize) {
        byte[] hash = {}; //do not return null
        byte[] buf = new byte[chunkSize];
        fileCRCs.clear();
        if (absPath == null) {
            return hash;
        }
        try (RandomAccessFile rf = new RandomAccessFile(absPath.toFile(), "r")) {
            //TODO: use FBCryptoDigest after FBCrypto update for stream operations
            MessageDigest dgst = MessageDigest.getInstance(DIGESTER);
            int rd;
            long offset = 0;
            while ((rd = rf.read(buf)) > 0) {
                dgst.update(buf, 0, rd);
                CheckSum cs = new CheckSum();
                cs.update(buf, rd);
                ChunkInfo ci = new ChunkInfo(offset, rd, cs.finish());
                fileCRCs.add(ci);
                offset += rd;
            }
            fileHash = dgst.digest();
        } catch (IOException | NoSuchAlgorithmException ignored) {
        }
        return fileHash;
    }

    public List<ChunkInfo> getChunksCRC() {
        if (fileCRCs.size() == 0) {
            getFileHashSums();
        }
        return fileCRCs;
    }

    public Date getFileDate() {
        long res = 1L;
        try {
            BasicFileAttributes attrs = Files.readAttributes(absPath, BasicFileAttributes.class);
            res = attrs.lastModifiedTime().toMillis();
        } catch (IOException ignored) {
        }
        return new Date(res);
    }

    public static class ChunkInfo {
        public long offset;
        public long size;
        public long crc;

        ChunkInfo(long offset, long size, long crc) {
            this.offset = offset;
            this.size = size;
            this.crc = crc;
        }
    }
}
