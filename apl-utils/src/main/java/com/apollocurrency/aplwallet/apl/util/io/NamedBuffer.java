/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.io;

import java.math.BigInteger;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface NamedBuffer extends WriteBuffer {

    WriteBuffer put(String tag, byte value);

    WriteBuffer put(String tag, byte[] value);

    WriteBuffer put(String tag, boolean value);

    WriteBuffer put(String tag, short value);

    WriteBuffer put(String tag, int value);

    WriteBuffer put(String tag, long value);

    WriteBuffer put(String tag, String hex);

    WriteBuffer put(String tag, BigInteger value);

    @Override
    default void writeByte(byte value) {
        put("", value);
    }

    @Override
    default WriteBuffer write(byte[] value) {
        return put("", value);
    }

    @Override
    default WriteBuffer write(boolean value) {
        return put("", value);
    }

    @Override
    default void writeShort(short value) {
        put("", value);
    }

    @Override
    default void writeInt(int value) {
        put("", value);
    }

    @Override
    default void writeLong(long value) {
        put("", value);
    }

    @Override
    default WriteBuffer write(String hex) {
        return put("", hex);
    }

    @Override
    default WriteBuffer write(BigInteger value) {
        return put("", value);
    }
}
