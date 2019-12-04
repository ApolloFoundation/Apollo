/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * SImple wrapper to CRC32 checksum
 * @author alukin@gmail.com
 */
public class CheckSum {
    private Checksum cs = new CRC32();

    public void update(byte[] bytes){
        cs.update(bytes);
    }

    public long finish(){
        return cs.getValue();
    }

    void update(byte[] dataBuf, int size) {
        cs.update(dataBuf, 0, size);
    }
}
