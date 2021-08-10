/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.utils;

import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionUtilTest {
    @Test
    void toDbIterator() {
        //GIVEN
        //WHEN
        DbIterator<String> dbIterator = CollectionUtil.toDbIterator(List.of("str1", "str2", "str3"));

        //THEN
        assertTrue(dbIterator.hasNext());
        assertEquals("str1", dbIterator.next());
        assertEquals("str2", dbIterator.next());
        assertEquals("str3", dbIterator.next());
        assertFalse(dbIterator.hasNext());
    }

    @Test
    void toList() {
        //GIVEN
        //WHEN
        List<String> list = CollectionUtil.toList(CollectionUtil.toDbIterator(List.of("str1", "str2", "str3")));

        //THEN
        assertEquals(3, list.size());
        assertEquals("str1", list.get(0));
        assertEquals("str2", list.get(1));
        assertEquals("str3", list.get(2));
    }

}