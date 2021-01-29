/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.common;

import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class TxBytesSerializer {

    private final TxSerializer txSerializer;
    private WriteBuffer buffer;

    public TxBytesSerializer(TxSerializer txSerializer) {
        this.txSerializer = txSerializer;
    }

/*    public final void writeLong(long v) throws IOException {
        byte[] writeBuffer = new byte[8];
        writeBuffer[0] = (byte)(v >>> 56);
        writeBuffer[1] = (byte)(v >>> 48);
        writeBuffer[2] = (byte)(v >>> 40);
        writeBuffer[3] = (byte)(v >>> 32);
        writeBuffer[4] = (byte)(v >>> 24);
        writeBuffer[5] = (byte)(v >>> 16);
        writeBuffer[6] = (byte)(v >>>  8);
        writeBuffer[7] = (byte)(v >>>  0);
        out.write(writeBuffer, 0, 8);
        incCount(8);
    }*/

    public void startTx(WriteBuffer buffer) {
        this.buffer = buffer;
    }

    public void endTx() {

    }

    public void element(byte data) {
        buffer.write(data);

    }

    public void element(byte[] data) {

    }

    public void element(short data) {

    }

    public void element(int data) {

    }

    public void element(long data) {

    }

    public void element(String hexData) {

    }

    public void element(BigInteger data) {

    }

    public void startList() {

    }

    public void endList() {

    }
}
