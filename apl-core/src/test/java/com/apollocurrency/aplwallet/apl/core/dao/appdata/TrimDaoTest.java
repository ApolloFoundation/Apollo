/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.appdata;

import com.apollocurrency.aplwallet.apl.core.dao.appdata.TrimDao;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.TrimEntry;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


class TrimDaoTest {
    @RegisterExtension
    DbExtension extension = new DbExtension();
    TrimDao dao = new TrimDao(extension.getDatabaseManager());

    @Test
    void testInsert() {
        TrimEntry saved = dao.save(new TrimEntry(null, 10, false));
        int count = dao.count();
        assertEquals(2, count);
        assertEquals(2, saved.getId());
    }

    @Test
    void testUpdate() {
        TrimEntry trimEntry = new TrimEntry(1L, 3, true);
        dao.save(trimEntry);
        int count = dao.count();
        assertEquals(1, count);
        assertEquals(trimEntry, dao.get());
    }

    @Test
    void testGet() {
        TrimEntry entry = dao.get();
        assertEquals(new TrimEntry(1L, 1000, true), entry);
    }

    @Test
    void testClear() {
        dao.clear();
        int count = dao.count();
        assertEquals(0, count);
        assertNull(dao.get());
    }
}