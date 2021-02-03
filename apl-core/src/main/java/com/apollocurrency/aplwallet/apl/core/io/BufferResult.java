/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.io;

import lombok.Getter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class BufferResult implements Result {

    @Getter
    private final WriteBuffer buffer;

    public BufferResult(WriteBuffer buffer) {
        Objects.requireNonNull(buffer);
        this.buffer = buffer;
    }

    public static BufferResult createLittleEndianByteArrayResult() {
        ByteArrayStream buffer = new ByteArrayStream();
        buffer.setOrder(ByteOrder.LITTLE_ENDIAN);
        return new BufferResult(buffer);
    }

    public static BufferResult createByteArrayResult() {
        return new BufferResult(new ByteArrayStream());
    }

    public static BufferResult createByteBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        return new BufferResult(new WriteByteBuffer(ByteBuffer.allocate(capacity)));
    }

    public static BufferResult createJsonResult() {
        return new BufferResult(new JsonBuffer());
    }

    public static BufferResult createJsonResult(JsonBuffer buffer) {
        return new BufferResult(buffer);
    }

    @Override
    public byte[] array() {
        return buffer.toByteArray();
    }
}
