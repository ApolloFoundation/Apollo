/*
 * Copyright (c)  2018-2021. Apollo Foundation.
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

    WriteBuffer write(byte value);

    WriteBuffer write(byte[] value);

    WriteBuffer write(boolean value);

    WriteBuffer write(short value);

    WriteBuffer write(int value);

    WriteBuffer write(long value);

    WriteBuffer write(String hex);

    WriteBuffer write(BigInteger value);

    default WriteBuffer concat(byte[] bytes) {
        return write(bytes);
    }
}
