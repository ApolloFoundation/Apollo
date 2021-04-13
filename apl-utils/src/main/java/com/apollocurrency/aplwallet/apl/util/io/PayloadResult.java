/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.io;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class PayloadResult implements Result {
    private static final AtomicLong counter = new AtomicLong();
    @Setter
    private int payloadSize = -1;
    @Getter
    private final WriteBuffer buffer;
    @Getter
    private final long index;

    public PayloadResult(WriteBuffer buffer) {
        Objects.requireNonNull(buffer);
        this.buffer = buffer;

        this.index = counter.incrementAndGet();
        if (buffer instanceof ByteArrayStream) {
            ((ByteArrayStream) buffer).setParentIndex(index);
        }
        log.trace("Create PR with id: {}", index);
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

    @Override
    public String toString() {
        return "PR{" +
            "buffer=" + buffer +
            "index=" + index +
            '}';
    }
}
