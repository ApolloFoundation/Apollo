/*
 * Copyright (c) 2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.io;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class PayloadResult implements Result {
    @Setter
    private int payloadSize = -1;
    @Getter
    private final WriteBuffer buffer;

    public PayloadResult(WriteBuffer buffer) {
        Objects.requireNonNull(buffer);
        this.buffer = buffer;
    }

    public static PayloadResult createLittleEndianByteArrayResult() {
        ByteArrayStream buffer = new ByteArrayStream();
        buffer.setOrder(ByteOrder.LITTLE_ENDIAN);

        return new PayloadResult(buffer);
    }

    public static PayloadResult createByteArrayResult() {
        return new PayloadResult(new ByteArrayStream());
    }

    public static PayloadResult createByteBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        return new PayloadResult(new WriteByteBuffer(ByteBuffer.allocate(capacity)));
    }

    public static PayloadResult createJsonResult() {
        return new PayloadResult(new JsonBuffer());
    }

    public static PayloadResult createJsonResult(JsonBuffer buffer) {
        return new PayloadResult(buffer);
    }

    @Override
    public byte[] array() {
        return buffer.toByteArray();
    }

    @Override
    public int payloadSize() {
        if (payloadSize < 0) {
            payloadSize = buffer.toByteArray().length;
        }
        return payloadSize;
    }
}
