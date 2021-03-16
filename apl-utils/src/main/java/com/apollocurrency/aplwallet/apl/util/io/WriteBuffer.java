/*
 * Copyright (c) 2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.io;

import java.math.BigInteger;
import java.nio.ByteOrder;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface WriteBuffer {
    ByteOrder order();

    ByteOrder setOrder(ByteOrder order);

    default boolean isBigEndian() {
        return ByteOrder.BIG_ENDIAN.equals(order());
    }

    default int size() {
        return toByteArray().length;
    }

    byte[] toByteArray();

    default WriteBuffer write(byte value) {
        writeByte(value);
        return this;
    }

    default WriteBuffer write(byte[] value) {
        for (byte b : value) {
            writeByte(b);
        }
        return this;
    }

    default WriteBuffer write(boolean value) {
        writeByte((byte) (value ? 1 : 0));
        return this;
    }

    void writeByte(byte value);

    default WriteBuffer write(short value) {
        writeShort(value);
        return this;
    }

    void writeShort(short value);

    default WriteBuffer write(int value) {
        writeInt(value);
        return this;
    }

    void writeInt(int value);

    default WriteBuffer write(long value) {
        writeLong(value);
        return this;
    }

    void writeLong(long value);

    WriteBuffer write(String hex);

    WriteBuffer write(BigInteger value);

    /**
     * Copies bytes to the output buffer without any encoding
     *
     * @param bytes the byte array
     * @return the output buffer
     */
    default WriteBuffer concat(byte[] bytes) {
        return write(bytes);
    }
}
