/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.rlp;

import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Simple reader, to read items from RLP encoded input.
 * Not thread-safe.
 *
 * @author andrew.zinchenko@gmail.com
 */
public class RlpReader implements Iterable<RlpType> {
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

    @Override
    public Iterator<RlpType> iterator(){
        return iterator;
    }

    public int size(){
        return size;
    }

    public boolean hasNext(){
        return iterator.hasNext();
    }

    public byte[] read() {
        return RlpConverter.toByteArray(iterator.next());
    }

    public String readString() {
        return RlpConverter.toString(iterator.next());
    }

    public long readLong() {
        return RlpConverter.toLong(iterator.next());
    }

    public int readInt() {
        return RlpConverter.toInt(iterator.next());
    }

    public BigInteger readBigInteger() {
        return RlpConverter.toBigInteger(iterator.next());
    }

    public <T> List<T> readList(Function<RlpType, T> mapper) {
        RlpList list = (RlpList) iterator.next();
        return list.getValues().stream().map(mapper).collect(Collectors.toList());
    }

    public List<RlpType> readList() {
        RlpList list = (RlpList) iterator.next();
        return list.getValues();
    }

    public RlpReader readListReader() {
        RlpList list = (RlpList) iterator.next();
        return new RlpReader(list);
    }
}
