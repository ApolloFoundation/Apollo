/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.io;

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
    public WriteBuffer write(byte value) {
        out.write(value);
        return this;
    }

    @SneakyThrows
    @Override
    public WriteBuffer write(byte[] value) {
        out.write(value);
        return this;
    }
}
