/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.rlp;

import com.apollocurrency.aplwallet.apl.util.io.WriteBuffer;
import org.bouncycastle.util.Arrays;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;

import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.List;

/**
 * Simple writer, to encode values in RLP format.
 * RLP encoding only supports POSITIVE integer values, any negative value encoded as a zero.
 * So every negative numeric is converted to the unsigned numeric before encoding except BigInteger value, it's encoded as is.
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
    public ByteOrder order() {
        return ByteOrder.BIG_ENDIAN;
    }

    @Override
    public ByteOrder setOrder(ByteOrder order) {
        throw new UnsupportedOperationException();
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
        return write((byte) (value ? 1 : 0));
    }

    @Override
    public WriteBuffer write(short value) {
        return write((long) value);
    }

    @Override
    public WriteBuffer write(int value) {
        return write((long) value);
    }

    @Override
    public WriteBuffer write(long value) {
        RlpString rlpStr = value >= 0 ?
            RlpString.create(value) :
            RlpString.create(new BigInteger(Long.toUnsignedString(value)));
        output = Arrays.concatenate(output, RlpEncoder.encode(rlpStr));
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
