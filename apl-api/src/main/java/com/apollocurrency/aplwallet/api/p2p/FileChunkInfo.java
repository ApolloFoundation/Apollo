/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.p2p;

import java.util.Objects;

/**
 *
 * @author alukin@gmail.com
 */
public class FileChunkInfo {
    public String fileId;
    public Integer chunkId;
    public Long offset;
    public Long size;
    /**
     * 0 - not present
     * 1 - download in progress;
     * 2 - present;
     * 3 - saved;
     */
    public FileChunkState present;
    public long crc;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileChunkInfo that = (FileChunkInfo) o;
        return chunkId == that.chunkId &&
                offset == that.offset &&
                size == that.size &&
                present == that.present &&
                crc == that.crc &&
                Objects.equals(fileId, that.fileId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId, chunkId, offset, size, present, crc);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("FileChunkInfo{");
        sb.append("fileId='").append(fileId).append('\'');
        sb.append(", chunkId=").append(chunkId);
        sb.append(", offset=").append(offset);
        sb.append(", size=").append(size);
        sb.append(", present=").append(present);
        sb.append(", crc=").append(crc);
        sb.append('}');
        return sb.toString();
    }
}
