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
    public boolean present;
    public long crc;
}
