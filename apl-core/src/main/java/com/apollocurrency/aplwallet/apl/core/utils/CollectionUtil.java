/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.utils;

import com.apollocurrency.aplwallet.apl.core.db.DbIterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CollectionUtil {
    public static <T> List<T> toList(DbIterator<T> dbIterator) {
        List<T> list = new ArrayList<>();
        try {
            while (dbIterator.hasNext()) {
                T element = dbIterator.next();
                list.add(element);
            }
            return list;
        } finally {
            dbIterator.close();
        }
    }

    public static boolean isEmpty(Collection collection) {
        return collection == null || collection.size() == 0;
    }

    public static boolean isEmpty(Map map) {
        return map == null || map.isEmpty();
    }
}
