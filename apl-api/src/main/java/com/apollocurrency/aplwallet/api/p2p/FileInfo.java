/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.p2p;

import java.util.Date;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Info about file in P2P downloading process
 * @author alukin@gmail.com
 */
@ToString
@EqualsAndHashCode
@Getter @Setter
public class FileInfo {
    public String fileId;
    public boolean isPresent=false;
    public Date fileDate;
    public String hash=null;
    public Long size=-1L;
    public Integer chunkSize;
    public String originHostSignature;
}
