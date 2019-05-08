/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.p2p;

/**
 *
 * @author alukin@gmail.com
 */
public class FileChunkInfo {
    public int id;
    public int offset;
    public int size;
    public boolean present;
    public int crc32;
}
