/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.p2p;

import java.util.Date;
import java.util.Objects;

/**
 * Info about file in P2P downloading process
 * @author alukin@gmail.com
 */
public class FileInfo {
    public String fileId;
    public boolean isPresent;
    public Date fileDate;
    public String hash;
    public Long size;
    public Integer chunkSize;
    public String originHostSignature;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileInfo fileInfo = (FileInfo) o;
        return isPresent == fileInfo.isPresent &&
                Objects.equals(fileId, fileInfo.fileId) &&
                Objects.equals(fileDate, fileInfo.fileDate) &&
                Objects.equals(hash, fileInfo.hash) &&
                Objects.equals(size, fileInfo.size) &&
                Objects.equals(chunkSize, fileInfo.chunkSize) &&
                Objects.equals(originHostSignature, fileInfo.originHostSignature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId, isPresent, fileDate, hash, size, chunkSize, originHostSignature);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("FileInfo{");
        sb.append("fileId='").append(fileId).append('\'');
        sb.append(", isPresent=").append(isPresent);
        sb.append(", fileDate=").append(fileDate);
        sb.append(", hash='").append(hash).append('\'');
        sb.append(", size=").append(size);
        sb.append(", chunkSize=").append(chunkSize);
        sb.append(", originHostSignature='").append(originHostSignature).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
