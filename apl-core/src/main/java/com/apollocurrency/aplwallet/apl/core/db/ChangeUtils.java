package com.apollocurrency.aplwallet.apl.core.db;

import java.util.Arrays;
import java.util.function.BiFunction;

public class ChangeUtils {
    public static InMemoryVersionedDerivedEntityRepository.ChangedValue getChange(Object value, Object prevValue, BiFunction<Object, Object,Boolean> equalFunction) {
        if (prevValue == null) {
            if (value == null) {
                return new InMemoryVersionedDerivedEntityRepository.ChangedValue();
            }
            return new InMemoryVersionedDerivedEntityRepository.ChangedValue(value);
        } else {
            if (value == null) {
                return new InMemoryVersionedDerivedEntityRepository.ChangedValue(null);
            }
            if (equalFunction.apply(value, prevValue)) {
                return new InMemoryVersionedDerivedEntityRepository.ChangedValue();
            } else {
                return new InMemoryVersionedDerivedEntityRepository.ChangedValue(value);
            }
        }
    }
    public static InMemoryVersionedDerivedEntityRepository.ChangedValue getChange(Object value, Object prevValue) {
        return getChange(value, prevValue, (v1, v2) -> value.equals(prevValue));
    }

    public static InMemoryVersionedDerivedEntityRepository.ChangedValue getDoubleByteArrayChange(Object value, Object prevValue) {
        return getChange(value, prevValue, (arr1, arr2) -> Arrays.deepEquals((byte[][]) arr1, (byte[][]) arr2));
    }

    private ChangeUtils() {}
}
