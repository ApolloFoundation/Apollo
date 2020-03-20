/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.alias.converter;

import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.rest.converter.Converter;

import javax.enterprise.inject.Vetoed;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Converts an iterator to a Stream.
 *
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
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(dbIterator, Spliterator.ORDERED),
            false
        );
    }
}
