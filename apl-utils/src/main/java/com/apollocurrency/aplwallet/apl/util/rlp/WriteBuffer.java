/*
 * Copyright (c) 2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.rlp;

import org.web3j.rlp.RlpType;

import java.math.BigInteger;
import java.util.List;

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

    WriteBuffer write(List<RlpType> list);

    WriteBuffer concat(byte[] bytes);
}
