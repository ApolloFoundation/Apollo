/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.db.DbIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class CollectionUtil {
    public static <T> List<T> toList(DbIterator<T> dbIterator) {
        List<T> list = new ArrayList<>();
        try (dbIterator) {
            while (dbIterator.hasNext()) {
                T element = dbIterator.next();
                list.add(element);
            }
            return list;
        }
    }

    public static <T> Stream<T> limitStream(Stream<T> stream, int from, int to) {
        int limit = to >=0 && to >= from && to < Integer.MAX_VALUE ? to - from + 1 : 0;
        if (from > 0) {
            stream = stream.skip(from);
        }
        if (limit > 0) {
            stream = stream.limit(limit);
        }
        return stream;
    }
}
