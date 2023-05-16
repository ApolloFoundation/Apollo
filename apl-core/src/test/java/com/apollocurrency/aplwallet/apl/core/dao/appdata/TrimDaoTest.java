/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.appdata;

import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.TrimEntry;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Slf4j

@Tag("slow")
class TrimDaoTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer);
    TrimDao dao = new TrimDao(extension.getDatabaseManager());

    @AfterEach
    void tearDown() {
        extension.cleanAndPopulateDb();
    }

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