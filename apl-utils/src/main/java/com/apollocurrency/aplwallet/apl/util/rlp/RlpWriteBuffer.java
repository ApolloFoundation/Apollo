package com.apollocurrency.aplwallet.apl.util.rlp;

import org.bouncycastle.util.Arrays;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpString;

import java.math.BigInteger;

/**
 * Simple writer, to encode values in RLP format.
 * Not thread safe.
 *
 * @author andrew.zinchenko@gmail.com
 */
public class RlpWriteBuffer {
    private byte[] output;

    public RlpWriteBuffer() {
        output = new byte[]{};
    }

    public byte[] toByteArray(){
        return output;
    }

    public RlpWriteBuffer write(byte value) {
        output = Arrays.concatenate(output, RlpEncoder.encode(RlpString.create(value)));
        return this;
    }

    public RlpWriteBuffer write(byte[] value) {
        output = Arrays.concatenate(output, RlpEncoder.encode(RlpString.create(value)));
        return this;
    }

    public RlpWriteBuffer write(long value) {
        output = Arrays.concatenate(output, RlpEncoder.encode(RlpString.create(value)));
        return this;
    }

    public RlpWriteBuffer write(String hex) {
        output = Arrays.concatenate(output, RlpEncoder.encode(RlpString.create(hex)));
        return this;
    }

    public RlpWriteBuffer write(BigInteger value) {
        output = Arrays.concatenate(output, RlpEncoder.encode(RlpString.create(value)));
        return this;
    }
}
