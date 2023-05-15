/*
 * Copyright (c) 2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.io;

import lombok.Getter;

import java.nio.ByteBuffer;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class WriteByteBuffer extends AbstractWriteBuffer {
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
}
