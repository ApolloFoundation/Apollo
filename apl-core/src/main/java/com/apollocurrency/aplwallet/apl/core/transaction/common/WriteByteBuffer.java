/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.common;

import lombok.Getter;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class WriteByteBuffer implements WriteBuffer {
    @Getter
    private final ByteBuffer buffer;

    public WriteByteBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public byte[] toByteArray() {
        return buffer.array();
    }

    @Override
    public WriteBuffer write(byte value) {
        buffer.put(value);
        return this;
    }

    @Override
    public WriteBuffer write(byte[] value) {
        buffer.put(value);
        return this;
    }

    @Override
    public WriteBuffer write(boolean value) {
        buffer.put((byte) (value ? 1 : 0));
        return this;
    }

    @Override
    public WriteBuffer write(long value) {
        buffer.putLong(value);
        return this;
    }

    @Override
    public WriteBuffer write(String hex) {
        buffer.put(hex.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    @Override
    public WriteBuffer write(BigInteger value) {
        buffer.put(value.toByteArray());
        return this;
    }

    @Override
    public WriteBuffer concat(byte[] bytes) {
        buffer.put(bytes);
        return this;
    }
}
