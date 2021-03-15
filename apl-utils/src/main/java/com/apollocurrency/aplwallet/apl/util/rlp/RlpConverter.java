/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.rlp;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;

import java.math.BigInteger;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author andrew.zinchenko@gmail.com
 */
@NoArgsConstructor(access = AccessLevel.NONE)
public class RlpConverter {

    public static byte[] toByteArray(RlpType value) {
        return ((RlpString) value).getBytes();
    }

    public static String toString(RlpType value) {
        return new String(((RlpString) value).getBytes());
    }

    public static long toLong(RlpType value) {
        String unsignedLong = ((RlpString) value).asPositiveBigInteger().toString();
        return Long.parseUnsignedLong(unsignedLong);
    }

    public static int toInt(RlpType value) {
        return ((RlpString) value).asPositiveBigInteger().intValueExact();
    }

    public static short toShort(RlpType value) {
        return ((RlpString) value).asPositiveBigInteger().shortValueExact();
    }

    public static byte toByte(RlpType value) {
        return ((RlpString) value).asPositiveBigInteger().byteValueExact();
    }

    public static BigInteger toBigInteger(RlpType value) {
        return ((RlpString) value).asPositiveBigInteger();
    }

    public static <T> List<T> toList(List<RlpType> list, Function<RlpType, T> mapper) {
        return list.stream().map(mapper).collect(Collectors.toList());
    }

    public static List<String> toStringList(List<RlpType> list) {
        return toList(list, RlpConverter::toString);
    }

    public static List<Long> toLongList(List<RlpType> list) {
        return toList(list, RlpConverter::toLong);
    }

    public static List<BigInteger> toBigIntegerList(List<RlpType> list) {
        return toList(list, RlpConverter::toBigInteger);
    }
}
