/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.p2p;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author alukin@gmail.com
 */
public class FileDownloadInfo {
    public FileInfo fileInfo=new FileInfo();    
    public List<FileChunkInfo> chunks = new ArrayList<>();
    @JsonIgnore
    /** record creation date, needed by cache */
    public Instant created;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileDownloadInfo that = (FileDownloadInfo) o;
        return Objects.equals(fileInfo, that.fileInfo) &&
                Objects.equals(chunks, that.chunks) &&
                Objects.equals(created, that.created);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileInfo, chunks, created);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("FileDownloadInfo{");
        sb.append("fileInfo=").append(fileInfo);
        sb.append(", chunks=").append(chunks);
        sb.append(", created=").append(created);
        sb.append('}');
        return sb.toString();
    }
}
