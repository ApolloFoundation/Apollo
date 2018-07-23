/*
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.util.Convert;

import java.util.Arrays;

public class DoubleByteArrayTuple {
    private final byte[] first;
    private final byte[] second;

    public DoubleByteArrayTuple(byte[] first, byte[] second) {
        this.first = first;
        this.second = second;
    }
    public DoubleByteArrayTuple(String firstUrlPart, String secondUrlPart) {
        this.first = Convert.parseHexString(firstUrlPart);
        this.second = Convert.parseHexString(secondUrlPart);
    }

    public byte[] getFirst() {
        return first;
    }

    public byte[] getSecond() {
        return second;
    }

    @Override
    public String toString() {
        return Convert.toHexString(first) + System.lineSeparator() + Convert.toHexString(second);
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
