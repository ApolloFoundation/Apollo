/*
 * Copyright (c) 2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.io;

import lombok.SneakyThrows;

import java.io.ByteArrayOutputStream;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class ByteArrayStream extends AbstractWriteBuffer {
    private final ByteArrayOutputStream out;

    public ByteArrayStream() {
        this(32);
    }

    public ByteArrayStream(int capacity) {
        this.out = new ByteArrayOutputStream(capacity);
    }

    @Override
    public byte[] toByteArray() {
        return out.toByteArray();
    }

    @Override
    public void writeByte(byte value) {
        out.write(value);
    }

    @SneakyThrows
    @Override
    public WriteBuffer write(byte[] value) {
        out.write(value);
        return this;
    }
}
