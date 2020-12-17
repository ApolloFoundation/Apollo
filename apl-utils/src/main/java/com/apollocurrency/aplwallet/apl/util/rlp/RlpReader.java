/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.rlp;

import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Iterator;

/**
 * Simple reader, to read items from RLP encoded input.
 * Not thread safe.
 *
 * @author andrew.zinchenko@gmail.com
 */
public class RlpReader {
    private final Iterator<RlpType> iterator;
    private final int size;

    public RlpReader(String hexInput) {
        this(Numeric.hexStringToByteArray(hexInput));
    }

    public RlpReader(byte[] input) {
        this(RlpDecoder.decode(input));
    }

    public RlpReader(RlpList listInput) {
        this.size = listInput.getValues().size();
        this.iterator = listInput.getValues().iterator();
    }

    public int size(){
        return size;
    }

    public boolean hasNext(){
        return iterator.hasNext();
    }

    public byte[] read() {
        RlpType value = iterator.next();
        return ((RlpString) value).getBytes();
    }

    public String readString() {
        RlpType value = iterator.next();
        return new String(((RlpString) value).getBytes());
    }

    public long readLong() {
        RlpType value = iterator.next();
        return ((RlpString) value).asPositiveBigInteger().longValueExact();
    }

    public int readInt() {
        RlpType value = iterator.next();
        return ((RlpString) value).asPositiveBigInteger().intValueExact();
    }

    public BigInteger readBigInteger() {
        RlpType value = iterator.next();
        return ((RlpString) value).asPositiveBigInteger();
    }

    public RlpReader readList() {
        RlpList list = (RlpList) iterator.next();
        return new RlpReader(list);
    }
}
