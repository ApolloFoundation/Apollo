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
    Checksum cs = new CRC32();
    public void update(byte[] bytes){
        cs.update(bytes);
    }
    public Long finish(){
        return cs.getValue();
    }
}
