package com.apollocurrency.smc.rlp;

import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Iterator;

/**
 * Not thread safe.
 *
 * @author andrew.zinchenko@gmail.com
 */
public class RlpReader {
    private final Iterator<RlpType> iterator;

    public RlpReader(String hexInput) {
        this(Numeric.hexStringToByteArray(hexInput));
    }

    public RlpReader(byte[] input) {
        this(RlpDecoder.decode(input));
    }

    public RlpReader(RlpList listInput) {
        this.iterator = listInput.getValues().iterator();
    }

    public byte[] read() {
        RlpType value = iterator.next();
        return ((RlpString) value).getBytes();
    }

    public String readString() {
        RlpType value = iterator.next();
        return new String(((RlpString) value).getBytes());
    }

    public Long readLong() {
        RlpType value = iterator.next();
        return ((RlpString) value).asPositiveBigInteger().longValueExact();
    }

    public BigInteger readBigInteger() {
        RlpType value = iterator.next();
        return ((RlpString) value).asPositiveBigInteger();
    }
}
