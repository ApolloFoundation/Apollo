/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.converter.rest;

import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;

import jakarta.enterprise.inject.Vetoed;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Converts an iterator to a Stream.
 * <p>
 * Note that it cannot be a singleton cdi bean because
 * every injection requires a properly set type T in
 * a IteratorToStreamConverter instance.
 *
 * @author silaev-firstbridge on 3/20/2020
 */
@Vetoed
public class IteratorToStreamConverter<T> implements Converter<DbIterator<T>, Stream<T>> {
    @Override
    public Stream<T> apply(DbIterator<T> dbIterator) {
        Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(dbIterator, Spliterator.ORDERED);
        return StreamSupport.stream(
            spliterator,
            false
        ).onClose(dbIterator::close);
    }
}
