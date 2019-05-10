/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.p2p;

import java.util.Date;

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
}
