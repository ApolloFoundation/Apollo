/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.appdata.impl;

import com.apollocurrency.aplwallet.apl.core.entity.appdata.ScanEntity;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("slow")
class ScanDaoImplTest {
    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getInMemDbProps(), "db/scan-data.sql", null);
    ScanDaoImpl scanDao;

    @BeforeEach
    void setUp() {
        scanDao = new ScanDaoImpl(extension.getDatabaseManager());
    }

    @Test
    void saveOrUpdate() {
        ScanEntity toSave = new ScanEntity(false, true, 900, 901, true, true);

        scanDao.saveOrUpdate(toSave); //should replace existing

        assertEquals(toSave, scanDao.get());
    }

    @Test
    void testDelete() {
        int removed = scanDao.delete();

        assertEquals(1, removed);
        assertNull(scanDao.get());
    }

    @Test
    void get() {
        ScanEntity existing = new ScanEntity(true, false, 1000, 1000, true, false);

        ScanEntity scanEntity = scanDao.get();

        assertEquals(existing, scanEntity);
    }
}