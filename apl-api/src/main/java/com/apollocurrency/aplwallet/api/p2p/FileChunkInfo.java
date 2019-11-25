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
import lombok.ToString;

/**
 *
 * @author alukin@gmail.com
 */
@Getter @Setter
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
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
    @JsonIgnore
    public int failedAttempts = 0;
}
