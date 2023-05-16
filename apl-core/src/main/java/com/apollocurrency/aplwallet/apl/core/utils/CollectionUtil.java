/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.utils;


import com.apollocurrency.aplwallet.apl.util.db.DbIterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
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

    public static <T> List<T> toList(Stream<T> stream) {
        try (stream) {
            return stream.collect(Collectors.toList());
        }
    }
    public static <T> void forEach(Stream<T> stream, Consumer<T> c) {
        try (stream) {
            stream.forEach(c);
        }
    }
    public static <T> long count(Stream<T> stream) {
        try (stream) {
            return stream.count();
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

    public static boolean isEmpty(Collection collection) {
        return collection == null || collection.size() == 0;
    }

    public static boolean isEmpty(Map map) {
        return map == null || map.isEmpty();
    }

    public static <T> DbIterator<T> toDbIterator(final Collection<T> data) {
        final Iterator<T> iterator = data.iterator();
        return new DbIterator<>(null, null, null) {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                return iterator.next();
            }

            @Override
            public void close() {

            }

            @Override
            public Iterator<T> iterator() {
                return iterator;
            }
        };
    }
}
