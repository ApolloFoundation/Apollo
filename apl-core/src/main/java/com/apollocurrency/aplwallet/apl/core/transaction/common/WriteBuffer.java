/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.common;

import java.io.DataOutput;
import java.io.IOException;
import java.math.BigInteger;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface WriteBuffer {
    default int size() {
        return toByteArray().length;
    }

    byte[] toByteArray();

    WriteBuffer write(byte value);

    WriteBuffer write(byte[] value);

    WriteBuffer write(boolean value);

    WriteBuffer write(long value);

    WriteBuffer write(String hex);

    WriteBuffer write(BigInteger value);

    WriteBuffer concat(byte[] bytes);
    /*    public final void writeLong(long v) throws IOException {
        //BIG_ENDIAN
        byte[] writeBuffer = new byte[8];
        writeBuffer[0] = (byte)(v >>> 56);
        writeBuffer[1] = (byte)(v >>> 48);
        writeBuffer[2] = (byte)(v >>> 40);
        writeBuffer[3] = (byte)(v >>> 32);
        writeBuffer[4] = (byte)(v >>> 24);
        writeBuffer[5] = (byte)(v >>> 16);
        writeBuffer[6] = (byte)(v >>>  8);
        writeBuffer[7] = (byte)(v >>>  0);
        out.write(writeBuffer, 0, 8);
        incCount(8);
    }*/

    static void toLittleEndian(DataOutput out, long value, int length) throws IOException {
        long num = value;

        for (int i = 0; i < length; ++i) {
            out.write((int) (num & 255L));
            num >>= 8;
        }

    }
}
