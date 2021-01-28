/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.bind;

import java.math.BigInteger;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface WriteBuffer {
    int size();

    byte[] toByteArray();

    WriteBuffer write(byte value);

    WriteBuffer write(byte[] value);

    WriteBuffer write(boolean value);

    WriteBuffer write(long value);

    WriteBuffer write(String hex);

    WriteBuffer write(BigInteger value);

    WriteBuffer concat(byte[] bytes);
}
