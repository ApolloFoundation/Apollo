/*
 * Copyright (c) 2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.io;

import java.math.BigInteger;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * @author andrew.zinchenko@gmail.com
 */
public abstract class AbstractWriteBuffer implements WriteBuffer {

    private ByteOrder order;

    public AbstractWriteBuffer() {
        this(ByteOrder.BIG_ENDIAN);
    }

    public AbstractWriteBuffer(ByteOrder order) {
        this.order = order;
    }

    @Override
    public ByteOrder setOrder(ByteOrder order) {
        ByteOrder oldOrder = this.order;
        this.order = order;
        return oldOrder;
    }

    @Override
    public ByteOrder order() {
        return order;
    }

    @Override
    public WriteBuffer write(short value) {
        writeShort(value);
        return this;
    }

    @Override
    public WriteBuffer write(int value) {
        writeInt(value);
        return this;
    }

    @Override
    public WriteBuffer write(long value) {
        writeLong(value);
        return this;
    }

    @Override
    public WriteBuffer write(String hex) {
        write(hex.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    @Override
    public WriteBuffer write(BigInteger value) {
        write(value.toByteArray());
        return this;
    }

    @Override
    public void writeShort(short v) {
        putShortParts((byte) (0xFF & v)
            , (byte) (0xFF & (v >> 8)));

    }

    @Override
    public void writeInt(int v) {
        putIntParts((byte) (0xFF & v)
            , (byte) (0xFF & (v >> 8))
            , (byte) (0xFF & (v >> 16))
            , (byte) (0xFF & (v >> 24)));

    }

    @Override
    public void writeLong(long v) {
        putLongParts((byte) (0xFF & v)
            , (byte) (0xFF & (v >> 8))
            , (byte) (0xFF & (v >> 16))
            , (byte) (0xFF & (v >> 24))
            , (byte) (0xFF & (v >> 32))
            , (byte) (0xFF & (v >> 40))
            , (byte) (0xFF & (v >> 48))
            , (byte) (0xFF & (v >> 56)));

    }

    private void putLongParts(byte i0, byte i1, byte i2, byte i3, byte i4, byte i5, byte i6, byte i7) {
        write(pick(i0, i7));
        write(pick(i1, i6));
        write(pick(i2, i5));
        write(pick(i3, i4));
        write(pick(i4, i3));
        write(pick(i5, i2));
        write(pick(i6, i1));
        write(pick(i7, i0));
    }

    private void putIntParts(byte i0, byte i1, byte i2, byte i3) {
        write(pick(i0, i3));
        write(pick(i1, i2));
        write(pick(i2, i1));
        write(pick(i3, i0));
    }

    private void putShortParts(byte i0, byte i1) {
        write(pick(i0, i1));
        write(pick(i1, i0));
    }

    private byte pick(byte le, byte be) {
        return isBigEndian() ? be : le;
    }

}
