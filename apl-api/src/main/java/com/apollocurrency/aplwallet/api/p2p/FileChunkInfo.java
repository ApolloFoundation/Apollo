/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.p2p;

/**
 *
 * @author alukin@gmail.com
 */
public class FileChunkInfo {
    public String fileId;
    public int chunkId;
    public long offset;
    public int size;
    /**
     * 0 - not present
     * 1 - download in progress;
     * 2 - present;
     * 3 - saved;
     */
    public int present;
    public long crc;
}
