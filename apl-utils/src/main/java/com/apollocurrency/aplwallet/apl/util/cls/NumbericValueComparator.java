package com.apollocurrency.aplwallet.apl.util.cls;

import java.math.BigInteger;

/**
 * Non-parseable numbers give 0. So 2 non-parseables are equival
 * Supported radix is 10, 16, 8
 * @author alukin@gmail.com
 */
public class NumbericValueComparator implements ItemValueComparator {

    private BigInteger parse(String vv) {
        String v=vv.replaceAll("\"", "");
        BigInteger res = BigInteger.ZERO;
        try {
            res = new BigInteger(v);
        } catch (NumberFormatException e10) {
            try {
                res = new BigInteger(v, 16);
            } catch (NumberFormatException e16) {
                try {
                    res = new BigInteger(v, 8);
                } catch (NumberFormatException e8) {
                }
            }
        }
        return res;
    }

    @Override
    public int compare(String one, String two) {
        BigInteger oneB = parse(one);
        BigInteger twoB = parse(two);
        return oneB.compareTo(twoB);
    }

}
