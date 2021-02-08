/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.rlp;

import org.bouncycastle.util.Arrays;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;

import java.math.BigInteger;
import java.util.List;

/**
 * Simple writer, to encode values in RLP format.
 * Not thread-safe.
 *
 * @author andrew.zinchenko@gmail.com
 */
public class RlpWriteBuffer implements WriteBuffer {
    private byte[] output;

    public RlpWriteBuffer() {
        output = new byte[]{};
    }

    @Override
    public int size() {
        return output.length;
    }

    @Override
    public byte[] toByteArray() {
        return output;
    }

    @Override
    public WriteBuffer write(byte value) {
        output = Arrays.concatenate(output, RlpEncoder.encode(RlpString.create(value)));
        return this;
    }

    @Override
    public WriteBuffer write(byte[] value) {
        output = Arrays.concatenate(output, RlpEncoder.encode(RlpString.create(value)));
        return this;
    }

    @Override
    public WriteBuffer write(boolean value) {
        write((byte) (value ? 1 : 0));
        return this;
    }

    @Override
    public WriteBuffer write(long value) {
        output = Arrays.concatenate(output, RlpEncoder.encode(RlpString.create(value)));
        return this;
    }

    @Override
    public WriteBuffer write(String hex) {
        output = Arrays.concatenate(output, RlpEncoder.encode(RlpString.create(hex)));
        return this;
    }

    @Override
    public WriteBuffer write(BigInteger value) {
        output = Arrays.concatenate(output, RlpEncoder.encode(RlpString.create(value)));
        return this;
    }

    @Override
    public WriteBuffer write(List<RlpType> list) {
        output = Arrays.concatenate(output, RlpEncoder.encode(new RlpList(list)));
        return this;
    }

    @Override
    public WriteBuffer concat(byte[] bytes) {
        output = Arrays.concatenate(output, bytes);
        return this;
    }
}
