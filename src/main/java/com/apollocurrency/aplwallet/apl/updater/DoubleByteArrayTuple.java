package com.apollocurrency.aplwallet.apl.updater;

import java.util.Arrays;

public class DoubleByteArrayTuple {
    private final byte[] first;
    private final byte[] second;

    public DoubleByteArrayTuple(byte[] first, byte[] second) {
        this.first = first;
        this.second = second;
    }

    public byte[] getFirst() {
        return first;
    }

    public byte[] getSecond() {
        return second;
    }

    @Override
    public String toString() {
        return "DoubleByteArrayTuple{" +
                "first=" + Arrays.toString(first) +
                ", second=" + Arrays.toString(second) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DoubleByteArrayTuple)) return false;
        DoubleByteArrayTuple that = (DoubleByteArrayTuple) o;
        return Arrays.equals(first, that.first) &&
                Arrays.equals(second, that.second);
    }

    @Override
    public int hashCode() {

        int result = Arrays.hashCode(first);
        result = 31 * result + Arrays.hashCode(second);
        return result;
    }
}
