/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.p2p;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * @author alukin@gmail.com
 */
@Getter
@Setter
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileDownloadInfo {
    public FileInfo fileInfo = new FileInfo();
    public List<FileChunkInfo> chunks = new ArrayList<>();
    @JsonIgnore
    /** record creation date, needed by cache */
    public Instant created;

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("FileDownloadInfo{");
        sb.append("fileInfo=").append(fileInfo);
        sb.append(", chunks=[").append(chunks != null ? chunks.size() : -1).append("]");
        sb.append(", created=").append(created);
        sb.append('}');
        return sb.toString();
    }
}
