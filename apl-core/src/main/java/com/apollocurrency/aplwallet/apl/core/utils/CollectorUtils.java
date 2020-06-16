package com.apollocurrency.aplwallet.apl.core.utils;

import org.json.simple.JSONArray;

import javax.enterprise.inject.Vetoed;
import java.util.List;
import java.util.stream.Collector;

/**
 * @author silaev-firstbridge on 4/14/2020
 */
@Vetoed
public class CollectorUtils {
    private CollectorUtils() {
    }

    public static <T> Collector<T, JSONArray, JSONArray> jsonCollector() {
        return Collector.of(
            JSONArray::new,
            List::add,
            (r1, r2) -> {
                r1.addAll(r2);
                return r1;
            },
            Collector.Characteristics.IDENTITY_FINISH
        );
    }
}
