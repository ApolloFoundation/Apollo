package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.dao.state.InMemoryVersionedDerivedEntityRepository;

import java.util.Arrays;
import java.util.function.BiFunction;

public class ChangeUtils {
    public static InMemoryVersionedDerivedEntityRepository.Value getChange(Object value, Object prevValue, BiFunction<Object, Object,Boolean> equalFunction) {
        if (prevValue == null) {
            if (value == null) {
                return new InMemoryVersionedDerivedEntityRepository.Value();
            }
            return new InMemoryVersionedDerivedEntityRepository.Value(value);
        } else {
            if (value == null) {
                return new InMemoryVersionedDerivedEntityRepository.Value(null);
            }
            if (equalFunction.apply(value, prevValue)) {
                return new InMemoryVersionedDerivedEntityRepository.Value();
            } else {
                return new InMemoryVersionedDerivedEntityRepository.Value(value);
            }
        }
    }
    public static InMemoryVersionedDerivedEntityRepository.Value getChange(Object value, Object prevValue) {
        return getChange(value, prevValue, (v1, v2) -> value.equals(prevValue));
    }

    public static InMemoryVersionedDerivedEntityRepository.Value getDoubleByteArrayChange(Object value, Object prevValue) {
        return getChange(value, prevValue, (arr1, arr2) -> Arrays.deepEquals((byte[][]) arr1, (byte[][]) arr2));
    }

    private ChangeUtils() {}
}
