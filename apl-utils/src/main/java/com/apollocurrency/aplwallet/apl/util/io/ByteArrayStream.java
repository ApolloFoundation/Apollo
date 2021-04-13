/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.io;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class ByteArrayStream extends AbstractWriteBuffer {
    private static final AtomicLong counter = new AtomicLong();
    private final ByteArrayOutputStream out;
    private volatile long parentIndex;
    @Getter
    private final long index;

    public ByteArrayStream() {
        this(32);
    }

    public void setParentIndex(long parentIndex) {
        this.parentIndex = parentIndex;
        log.trace("Init BAS: {}-{}", parentIndex, index);
    }

    public ByteArrayStream(int capacity) {
        this.out = new ByteArrayOutputStream(capacity);
        this.index = counter.incrementAndGet();
        log.trace("Create ByteArrayStream {}", index);
    }

    @Override
    public byte[] toByteArray() {
        return out.toByteArray();
    }

    @Override
    public WriteBuffer write(byte value) {
        int length = out.toByteArray().length;
        out.write(value);
        log.trace("Write {} into {}-{} trace - {}", value, parentIndex, index, ThreadUtils.lastNStacktrace(20));
        if (out.toByteArray().length - length != 1) {
            log.error("Byte array inconsistency: {}, expected {}", Convert.toHexString(out.toByteArray()), length + 1);
        }
        return this;
    }

    @SneakyThrows
    @Override
    public WriteBuffer write(byte[] value) {
        out.write(value);
        return this;
    }

    @Override
    public String toString() {
        return "BAS{" +
            "out=" + Convert.toHexString(out.toByteArray()) +
            ", parentIndex=" + parentIndex +
            ", index=" + index +
            '}';
    }
}
