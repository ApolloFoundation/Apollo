/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.p2p.response;

import com.apollocurrency.aplwallet.api.p2p.FileChunk;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author alukin@gmail.com
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileChunkResponse extends BaseP2PResponse {
    public FileChunk chunk;
}
