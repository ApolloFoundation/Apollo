package com.apollocurrency.aplwallet.apl.core.utils;

import com.apollocurrency.aplwallet.apl.core.db.DbIterator;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author silaev-firstbridge on 3/20/2020
 */
public class StreamUtils {
    private StreamUtils() {
    }

    public static <T> Stream<T> getStreamFromIterator(DbIterator<T> dbIterator) {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(dbIterator, Spliterator.ORDERED),
            false
        );
    }
}
