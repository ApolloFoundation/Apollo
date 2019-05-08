/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.p2p;

/**
 * Info about file in P2P downloading process
 * @author alukin@gmail.com
 */
public class FileInfo {
    public String name;
    public String hash;
    public Long size;
    public Integer chunkSize;
    public String originHostSignature;
}
