/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableValuesDbTable;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedChildDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.db.table.VersionedValuesDbTableImpl;
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
import java.util.stream.Collectors;
@EnableWeld
class VersionedValuesDbTableTest {
    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getInMemDbProps(), "db/derived-data.sql", null);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            FullTextConfigImpl.class,
            DerivedDbTablesRegistryImpl.class)
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .build();

    VersionedDeletableValuesDbTable<VersionedChildDerivedEntity> table;
    DerivedTestData data;
    CacheUtils<VersionedChildDerivedEntity> cacheUtils;


    @BeforeEach
    void setUp() {
        data = new DerivedTestData();
        table = new VersionedValuesDbTableImpl();
        cacheUtils = new CacheUtils<>(table, extension);
    }

    @Test
    void testDelete() {
        DbUtils.inTransaction(extension, (con) -> {

            List<VersionedChildDerivedEntity> valuesToDelete = List.of(data.VCE_2_1_3, data.VCE_2_2_3, data.VCE_2_3_3);
            DbKey dbKey = table.getDbKeyFactory().newKey(valuesToDelete.get(0));
            int deleteHeight = data.VCE_2_3_3.getHeight() + 1;
            boolean deleted = table.delete(valuesToDelete.get(0), deleteHeight); // should delete all values linked to same dbKey
            assertTrue(deleted);
            List<VersionedChildDerivedEntity>  values = table.get(dbKey);
            assertTrue(values.isEmpty());
            List<VersionedChildDerivedEntity>  all;
            try {
                all = table.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
            }
            catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
            assertFalse(all.containsAll(valuesToDelete));
            valuesToDelete.forEach(e-> e.setLatest(false));
            assertTrue(all.containsAll(valuesToDelete));
            long lastDbId = data.VCE_4_1_1.getDbId();
            for (VersionedChildDerivedEntity  t : values) {
                t.setHeight(deleteHeight);
                t.setDbId(++lastDbId);
            }
            assertTrue(all.containsAll(valuesToDelete));
        });
    }

    @Test
    void testDeleteOutsideTransaction() {
        assertThrows(IllegalStateException.class, () -> table.delete(data.VCE_4_1_1, 0));
    }

    @Test
    void testDeleteForNull() {
        assertFalse(table.delete(null, 0));
    }

    @Test
    void testDeleteForSameHeight() {
        DbUtils.inTransaction(extension, (con) -> {

            List<VersionedChildDerivedEntity> valuesToDelete = List.of(data.VCE_1_1_2, data.VCE_1_2_2);
            DbKey dbKey = table.getDbKeyFactory().newKey(valuesToDelete.get(0));
            int deleteHeight = data.VCE_1_1_2.getHeight();
            boolean deleted = table.delete(valuesToDelete.get(0), deleteHeight);
            assertTrue(deleted);
            List<VersionedChildDerivedEntity>  values = table.get(dbKey);
            assertTrue(values.isEmpty());
            List<VersionedChildDerivedEntity>  all;
            try {
                all = table.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
            }
            catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
            assertEquals(12, all.size());
            assertFalse(all.containsAll(valuesToDelete));
            valuesToDelete.forEach(v -> v.setLatest(false));
            assertTrue(all.containsAll(valuesToDelete));
        });
    }

    @Test
    void testDeleteValuesAtHeightBelowLastValueToDelete() {
        DbUtils.inTransaction(extension, (con) -> {
            List<VersionedChildDerivedEntity> valuesToDelete = List.of(data.VCE_2_1_1, data.VCE_2_1_2, data.VCE_2_1_2, data.VCE_2_1_3, data.VCE_2_2_3, data.VCE_2_3_3);
            DbKey dbKey = table.getDbKeyFactory().newKey(valuesToDelete.get(0));
            int deleteHeight = data.VCE_2_1_1.getHeight();
            boolean deleted = table.delete(valuesToDelete.get(0), deleteHeight);
            assertTrue(deleted);
            List<VersionedChildDerivedEntity>  values = table.get(dbKey);
            assertTrue(values.isEmpty());
            List<VersionedChildDerivedEntity>  all;
            try {
                all = table.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
            }
            catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
            assertEquals(6, all.size());
            assertFalse(all.containsAll(valuesToDelete));
            valuesToDelete.forEach(v -> v.setLatest(false)); // no effect, all values were deleted
            assertFalse(all.containsAll(valuesToDelete));
        });
    }

    @Test
    void testDeleteForIncorrectDbKey() {
        DbUtils.inTransaction(extension, (con) -> {
            VersionedChildDerivedEntity valueToDelete = data.VCE_3_1_1;
            valueToDelete.setDbKey(mock(DbKey.class));
            assertThrows(RuntimeException.class, () -> table.delete(valueToDelete, valueToDelete.getHeight() + 1));
        });
    }

    @Test
    void testInsertWithSameKey() throws SQLException {
        List<VersionedChildDerivedEntity> toInsert = List.of(data.VCE_2_1_3, data.VCE_2_2_3, data.VCE_2_3_3);
        List<Long> dbIds = toInsert.stream().map(VersionedChildDerivedEntity::getDbId).collect(Collectors.toList());

        DbUtils.inTransaction(extension, (con) -> {
            List<VersionedChildDerivedEntity>  values = table.get(table.getDbKeyFactory().newKey(toInsert.get(0)));

            assertEquals(toInsert, values);
            cacheUtils.assertInCache(toInsert);
            toInsert.forEach(t-> t.setHeight(t.getHeight() + 1));
            table.insert(toInsert);
            //check cache in transaction
            cacheUtils.assertInCache(toInsert);

        });
        toInsert.forEach(t -> t.setHeight(t.getHeight() - 1));
        //check db
        cacheUtils.assertNotInCache(toInsert);
        List<VersionedChildDerivedEntity>  retrievedData = table.get(table.getDbKeyFactory().newKey(toInsert.get(0)));
        long lastDbId = data.VCE_4_1_1.getDbId();
        for (VersionedChildDerivedEntity  t : toInsert) {
            t.setHeight(t.getHeight() + 1);
            t.setDbId(++lastDbId);
        }
        assertEquals(toInsert, retrievedData);
        List<VersionedChildDerivedEntity>  allValues = table.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();

        assertTrue(allValues.containsAll(toInsert));
        for (int i = 0; i < toInsert.size(); i++) {
            VersionedChildDerivedEntity  t = toInsert.get(i);
            t.setHeight(t.getHeight() - 1);
            t.setLatest(false);
            t.setDbId(dbIds.get(i));
        }
        assertTrue(allValues.containsAll(toInsert));
    }
}
