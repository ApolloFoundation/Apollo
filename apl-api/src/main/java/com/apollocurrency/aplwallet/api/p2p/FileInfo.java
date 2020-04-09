/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.p2p;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * Info about file in P2P downloading process
 *
 * @author alukin@gmail.com
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileInfo {
    public String fileId;
    public boolean isPresent = false;
    public Date fileDate;
    public String hash = null;
    public Long size = -1L;
    public Integer chunkSize;
    public String originHostSignature;
}
