/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apollocurrency.aplwallet.apl.core.app.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedIdEntity;
import com.apollocurrency.aplwallet.apl.core.db.table.VersionedDeletableEntityDbTableImpl;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.DerivedTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.SQLException;
import java.util.List;

// at least 8 data records required to launch this test
// 2 deleted record, 1 latest not updated, 2 - 1 latest 1 not latest, 3 (1 latest, 1 not latest, 1 not latest)
@EnableWeld
class VersionedEntityDbTableTest {
    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getInMemDbProps(), "db/derived-data.sql", null);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            FullTextConfigImpl.class,
            DerivedDbTablesRegistryImpl.class)
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .build();

    private VersionedDeletableEntityDbTable<VersionedDerivedIdEntity> table;
    private DerivedTestData data;
    private CacheUtils<VersionedDerivedIdEntity> cacheUtils;

    @BeforeEach
    void setUp() {
        data = new DerivedTestData();
        table = new VersionedDeletableEntityDbTableImpl();
        cacheUtils = new CacheUtils<>(table, extension);
    }

    @Test
    void testInsertNewEntityWithExistingDbKey() {
        VersionedDerivedIdEntity t = data.VERSIONED_ENTITY_3_2;
        t.setHeight(t.getHeight() + 1);
        t.setDbId(data.VERSIONED_ENTITY_3_2.getDbId() + 1);
        List<VersionedDerivedIdEntity> expected = List.of(data.VERSIONED_ENTITY_1_1, data.VERSIONED_ENTITY_2_3, t);
        DbUtils.inTransaction(extension, (con)-> {
            table.insert(t);
            cacheUtils.assertInCache(t);
            assertEquals(t, table.get(table.getDbKeyFactory().newKey(t)));
            List<VersionedDerivedIdEntity> all = CollectionUtil.toList(table.getAll(0, Integer.MAX_VALUE));

            assertEquals(expected, all);
            cacheUtils.assertListInCache(expected);
        });
        cacheUtils.assertListNotInCache(expected);
        cacheUtils.assertNotInCache(t);
    }

    @Test
    public void testInsertNewEntityWithFakeDbKey() {
        VersionedDerivedIdEntity t = data.NEW_VERSIONED_ENTITY;
        VersionedDerivedIdEntity lastValue = data.VERSIONED_ENTITY_3_2;
        t.setHeight(lastValue.getHeight() + 1);
        t.setDbKey(table.getDbKeyFactory().newKey(lastValue));
        t.setDbId(lastValue.getDbId() + 1);
        List<VersionedDerivedIdEntity> expected = List.of(data.VERSIONED_ENTITY_1_1, data.VERSIONED_ENTITY_2_3, t);
        DbUtils.inTransaction(extension, (con)-> {
            table.insert(t);
            cacheUtils.assertInCache(t);
            assertEquals(t, table.get(table.getDbKeyFactory().newKey(t)));
            List<VersionedDerivedIdEntity> all = CollectionUtil.toList(table.getAll(0, Integer.MAX_VALUE));
            t.setDbKey(null);
            assertEquals(expected, all);
            cacheUtils.assertListInCache(expected);
        });
        cacheUtils.assertListNotInCache(expected);
        cacheUtils.assertNotInCache(t);
    }

    @Test
    public void testDelete() throws SQLException {
        VersionedDerivedIdEntity valueToDelete = data.VERSIONED_ENTITY_2_3;
        int oldHeight = valueToDelete.getHeight();
        long oldDbId = valueToDelete.getDbId();
        DbUtils.inTransaction(extension, (con)-> {
            valueToDelete.setHeight(data.VERSIONED_ENTITY_3_2.getHeight() + 200);
            valueToDelete.setDbId(data.VERSIONED_ENTITY_3_2.getDbId() + 1);
            boolean deleted = table.delete(valueToDelete, false, valueToDelete.getHeight());
            assertTrue(deleted, "Value should be deleted");
            VersionedDerivedIdEntity deletedValue = table.get(table.getDbKeyFactory().newKey(valueToDelete));
            assertNull(deletedValue, "Deleted value should not be returned by get call");
            try {
                List<VersionedDerivedIdEntity> values = table.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
                valueToDelete.setLatest(false);
                assertTrue(values.contains(valueToDelete), "All values should contain new deleted value");
                valueToDelete.setDbId(oldDbId);
                valueToDelete.setHeight(oldHeight);
                assertTrue(values.contains(valueToDelete), "All values should contain old deleted value");
                assertEquals(9, values.size());
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testNotDeletedForNullEntity() {
        DbUtils.inTransaction(extension, (con)-> {
            boolean deleted = table.delete(null, false, 0);
            assertFalse(deleted);
        });
    }

    @Test
    public void testDeleteNotInTransaction() {
        assertThrows(IllegalStateException.class, () -> table.delete(data.VERSIONED_ENTITY_1_1, false, 0));
    }

    @Test
    public void testDeleteNothingForNonexistentEntity() {
        DbUtils.inTransaction(extension, (con)-> {
            boolean deleted = table.delete(data.NEW_VERSIONED_ENTITY, false, Integer.MAX_VALUE);
            assertFalse(deleted);
        });
    }

    @Test
    public void testDeleteAllForHeightLessThanEntityHeight() {
        DbUtils.inTransaction(extension, (con)-> {
            VersionedDerivedIdEntity valueToDelete = data.VERSIONED_ENTITY_2_1;
            table.delete(valueToDelete, false, valueToDelete.getHeight());
            try {
                List<VersionedDerivedIdEntity> all = table.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
                List<VersionedDerivedIdEntity> expected = List.of(data.VERSIONED_ENTITY_1_1, data.VERSIONED_ENTITY_3_1, data.VERSIONED_ENTITY_4_1, data.VERSIONED_ENTITY_4_2, data.VERSIONED_ENTITY_3_2);
                assertEquals(expected, all);
            }
            catch (SQLException e) {
                e.printStackTrace();
            }

        });
    }

    @Test
    public void testDeleteAndKeepInCache() {
        VersionedDerivedIdEntity valueToDelete = data.VERSIONED_ENTITY_1_1;
        DbUtils.inTransaction(extension, (con)-> {
            table.get(table.getDbKeyFactory().newKey(valueToDelete));
            valueToDelete.setHeight(valueToDelete.getHeight() + 1);
            boolean deleted = table.delete(valueToDelete, true, valueToDelete.getHeight());
            assertTrue(deleted, "Value should be deleted");
            VersionedDerivedIdEntity deletedValue = table.get(table.getDbKeyFactory().newKey(valueToDelete));
            valueToDelete.setHeight(valueToDelete.getHeight() - 1);
            cacheUtils.assertInCache(valueToDelete);
            assertEquals(valueToDelete, deletedValue);
        });
        VersionedDerivedIdEntity deletedValue = table.get(table.getDbKeyFactory().newKey(valueToDelete));
        assertNull(deletedValue, "Deleted value should not be returned by get call after cache clear");
    }

}
