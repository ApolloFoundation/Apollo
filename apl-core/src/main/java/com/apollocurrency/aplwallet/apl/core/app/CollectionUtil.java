/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.db.DbIterator;

import java.util.ArrayList;
import java.util.List;

public class CollectionUtil {
    public static <T> List<T> toList(DbIterator<T> dbIterator) {
        List<T> list = new ArrayList<>();
        try {
            while (dbIterator.hasNext()) {
                T element = dbIterator.next();
                list.add(element);
            }
            return list;
        }
        finally {
            dbIterator.close();
        }
    }
}
