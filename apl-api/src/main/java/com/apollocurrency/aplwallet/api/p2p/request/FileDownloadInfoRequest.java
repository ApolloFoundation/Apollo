/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.p2p.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/**
 * @author alukin@gmail.com
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileDownloadInfoRequest extends BaseP2PRequest {
    public String fileId;
    public boolean full;

    public FileDownloadInfoRequest(UUID chainId) {
        super("getFileDownloadInfo", chainId);
    }
}
