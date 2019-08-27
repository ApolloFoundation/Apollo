package com.apollocurrency.aplwallet.apl.core.db;

import java.util.Arrays;
import java.util.function.BiFunction;

public class ChangeUtils {
    public static Object getChange(Object value, Object prevValue, BiFunction<Object, Object,Boolean> equalFunction) {
        return prevValue == null ? value : value == null || equalFunction.apply(value, prevValue) ? null : value;
    }
    public static Object getChange(Object value, Object prevValue) {
        return getChange(value, prevValue, (v1, v2) -> value.equals(prevValue));
    }

    public static Object getDoubleByteArrayChange(Object value, Object prevValue) {
        return getChange(value, prevValue, (arr1, arr2) -> Arrays.deepEquals((byte[][]) arr1, (byte[][]) arr2));
    }

    private ChangeUtils() {}
}
