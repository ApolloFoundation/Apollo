/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class ConvertTest {
    private static final byte[] EMPTY = new byte[0];
    private static final byte[] REVERSED_1 = new byte[] {3, 2, 1};
    private static final byte[] REVERSED_3 = new byte[] {-3, -1};
    private static final byte[] ARR_1 = new byte[] {1, 2, 3};
    private static final byte[] ARR_2 = new byte[] {4, 5};
    private static final byte[] ARR_3 = new byte[] {-1, -3};
    private static final byte[] ARR_4 = new byte[] {-2};
    private static final byte[] ALL_BYTES = new byte[] {1, 2, 3, 4, 5, -1, -3, -2};
    private static final byte[] LONG_BYTES = Convert.parseHexString("ff1ed78a9cc314e0");
    private static final long LONG_VALUE = Long.parseUnsignedLong("18383367719308432608");
    private static final long ID = -2899336147900037206L;
    private static final byte[] HASH = Convert.parseHexString("aa07f883227dc3d7343211ea8e84386f17d14a8e23d6f471cd3cd6607b037d52");
    private static final byte[] PARTIAL_HASH = Convert.parseHexString("343211ea8e84386f17d14a8e23d6f471cd3cd6607b037d52");

    @Test
    void testConcatArrays() {
        byte[] actual = Convert.concat(ARR_1, ARR_2, ARR_3, ARR_4);
        assertArrayEquals(ALL_BYTES, actual);
    }

    @Test
    void testConcatOneArray() {
        byte[] actual = Convert.concat(ARR_1);
        assertArrayEquals(ARR_1, actual);
    }

    @Test
    void testConcatEmptyArray() {
        byte[] actual = Convert.concat(EMPTY);
        assertArrayEquals(actual, EMPTY);
    }

    @Test
    void testConcatNull() {
        assertThrows(NullPointerException.class, () -> Convert.concat(null));
    }

    @Test
    void testReverse() {
        byte[] reversed = Convert.reverse(ARR_1);
        assertArrayEquals(REVERSED_1, reversed);
        reversed = Convert.reverse(reversed);
        assertArrayEquals(ARR_1, reversed);
    }

    @Test
    void testSingleElementArray() {
        byte[] reversed = Convert.reverse(ARR_4);
        assertArrayEquals(ARR_4, reversed);
    }

    @Test
    void testReverseTwoElementArray() {
        byte[] reversed = Convert.reverse(ARR_3);
        assertArrayEquals(REVERSED_3, reversed);
        reversed = Convert.reverse(reversed);
        assertArrayEquals(ARR_3, reversed);
    }

    @Test
    void testReverseSelf() {
        byte[] copyArr1 = Arrays.copyOf(ARR_1, ARR_1.length);
        Convert.reverseSelf(copyArr1);
        assertArrayEquals(REVERSED_1, copyArr1);
    }

    @Test
    void testReverseSelfToSelf() {
        byte[] copyArr1 = Arrays.copyOf(ARR_1, ARR_1.length);
        Convert.reverseSelf(copyArr1);
        assertArrayEquals(REVERSED_1, copyArr1);
        Convert.reverseSelf(copyArr1);
        assertArrayEquals(ARR_1, copyArr1);
    }

    @Test
    void testLongToBytes() {
        byte[] bytes = Convert.longToBytes(LONG_VALUE);
        assertArrayEquals(LONG_BYTES, bytes);
    }

    @Test
    void testBytesToLong() {
        long value = Convert.bytesToLong(LONG_BYTES);
        assertEquals(LONG_VALUE, value);
    }

    @Test
    void testToFullHash() {
        byte[] hash = Convert.toFullHash(ID, PARTIAL_HASH);

        assertArrayEquals(HASH, hash);
    }

    @Test
    void testToPartialHash() {
        byte[] partialHash = Convert.toPartialHash(HASH);

        assertArrayEquals(PARTIAL_HASH, partialHash);
    }

    @Test
    void testFullHashToId() {
        long id = Convert.fullHashToId(HASH);

        assertEquals(ID, id);
    }
}
