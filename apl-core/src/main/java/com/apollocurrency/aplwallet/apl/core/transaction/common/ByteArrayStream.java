/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.common;

import lombok.SneakyThrows;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class ByteArrayStream implements WriteBuffer {
    private final ByteArrayOutputStream byteArrayOutputStream;
    private final DataOutputStream out;

    public ByteArrayStream() {
        this(32);
    }

    public ByteArrayStream(int capacity) {
        this.byteArrayOutputStream = new ByteArrayOutputStream(capacity);
        this.out = new DataOutputStream(byteArrayOutputStream);
    }

    @Override
    public byte[] toByteArray() {
        return byteArrayOutputStream.toByteArray();
    }

    @SneakyThrows
    @Override
    public WriteBuffer write(byte value) {
        out.writeByte(value);
        return this;
    }

    @SneakyThrows
    @Override
    public WriteBuffer write(byte[] value) {
        out.write(value);
        return this;
    }

    @SneakyThrows
    @Override
    public WriteBuffer write(boolean value) {
        out.writeBoolean(value);
        return this;
    }

    @SneakyThrows
    @Override
    public WriteBuffer write(long value) {
        out.writeLong(value);
        return this;
    }

    @SneakyThrows
    @Override
    public WriteBuffer write(String hex) {
        out.write(hex.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    @SneakyThrows
    @Override
    public WriteBuffer write(BigInteger value) {
        out.write(value.toByteArray());
        return this;
    }

    @SneakyThrows
    @Override
    public WriteBuffer concat(byte[] bytes) {
        out.write(bytes);
        return this;
    }
}
