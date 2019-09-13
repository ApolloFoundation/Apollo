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
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// at least 8 data records required to launch this test
// 2 deleted record, 1 latest not updated, 2 - 1 latest 1 not latest, 3 (1 latest, 1 not latest, 1 not latest)
public abstract class VersionedEntityDbTableTest<T extends VersionedDerivedEntity> extends EntityDbTableTest<T> {
    public VersionedEntityDbTableTest(Class<T> clazz) {
        super(clazz);
    }

    private VersionedDeletableEntityDbTable<T> table;
    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        Map<DbKey, List<T>> dbKeyListMap = groupByDbKey();
        Assertions.assertTrue(dbKeyListMap.entrySet().stream().anyMatch(e-> e.getValue().size() >= 2 && e.getValue().stream().noneMatch(VersionedDerivedEntity::isLatest)), "At least two blockchain deleted record should exist");
        Assertions.assertTrue(dbKeyListMap.entrySet().stream().anyMatch(e-> e.getValue().size() == 1 && e.getValue().get(0).isLatest()), "At least one not updated record should exist");
        Assertions.assertTrue(dbKeyListMap.entrySet().stream().anyMatch(e-> e.getValue().size() == 2 && e.getValue().get(0).isLatest() && !e.getValue().get(1).isLatest()), "At least one updated record should exist");
        Assertions.assertTrue(dbKeyListMap.entrySet().stream().anyMatch(e-> e.getValue().size() == 3 && e.getValue().get(0).isLatest() && !e.getValue().get(1).isLatest() && !e.getValue().get(2).isLatest()), "At least one updated twice record should exist");
        table = (VersionedDeletableEntityDbTable<T>) getDerivedDbTable();
    }

    @Override
    public List<T> getAllLatest() {
        return sortByHeightDesc(getAll().stream().filter(VersionedDerivedEntity::isLatest).collect(Collectors.toList()));
    }

    @Override
    public List<T> getDeletedMultiversionRecord() {
        return sortByHeightAsc(groupByDbKey().entrySet().stream().filter(e -> e.getValue().size() >= 2 && e.getValue().stream().noneMatch(VersionedDerivedEntity::isLatest)).map(e -> e.getValue().get(0)).collect(Collectors.toList()));
    }

    @Test
    public void testInsertNewEntityWithExistingDbKey() {
        List<T> allLatest = getAllLatest();
        T t = allLatest.get(0);
        t.setHeight(t.getHeight() + 1);
        DbUtils.inTransaction(extension, (con)-> {
            table.insert(t);
            assertInCache(t);
            assertEquals(t, table.get(table.getDbKeyFactory().newKey(t)));
            List<T> all = CollectionUtil.toList(table.getAll(0, Integer.MAX_VALUE));
            assertEquals(allLatest.stream().sorted(getDefaultComparator()).collect(Collectors.toList()), all);
            assertListInCache(allLatest);
        });
        assertListNotInCache(allLatest);
        assertNotInCache(t);
    }

    @Test
    public void testInsertNewEntityWithFakeDbKey() {
        List<T> allLatest = getAllLatest();
        T t = valueToInsert();
        t.setHeight(allLatest.get(0).getHeight() + 1);
        t.setDbKey(allLatest.get(0).getDbKey());
        DbUtils.inTransaction(extension, (con)-> {
            table.insert(t);
            assertInCache(t);
            assertEquals(t, table.get(table.getDbKeyFactory().newKey(t)));
            List<T> all = CollectionUtil.toList(table.getAll(0, Integer.MAX_VALUE));
            allLatest.set(0, t);
            List<T> expected = allLatest.stream().sorted(getDefaultComparator()).collect(Collectors.toList());
            assertEquals(expected, all);
            assertListInCache(allLatest);
        });
        assertListNotInCache(allLatest);
        assertNotInCache(t);
    }

    @Test
    @Override
    public void testDelete() throws SQLException {
        List<T> allLatest = getAllLatest();
        T valueToDelete = allLatest.get(2);
        int oldHeight = valueToDelete.getHeight();
        long oldDbId = valueToDelete.getDbId();
        DbUtils.inTransaction(extension, (con)-> {
            List<T> sortedByDbId = sortByHeightDesc(getAll());
            valueToDelete.setHeight(sortedByDbId.get(0).getHeight() + 200);
            valueToDelete.setDbId(sortedByDbId.get(0).getDbId() + 1);
            boolean deleted = table.delete(valueToDelete, false, valueToDelete.getHeight());
            assertTrue(deleted, "Value should be deleted");
            T deletedValue = table.get(table.getDbKeyFactory().newKey(valueToDelete));
            assertNull(deletedValue, "Deleted value should not be returned by get call");
            try {
                List<T> values = table.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
                valueToDelete.setLatest(false);
                assertTrue(values.contains(valueToDelete), "All values should contain new deleted value");
                valueToDelete.setDbId(oldDbId);
                valueToDelete.setHeight(oldHeight);
                assertTrue(values.contains(valueToDelete), "All values should contain old deleted value");
                assertEquals(getAll().size() + 1, values.size());
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testNotDeletedForNullEntity() {
        DbUtils.inTransaction(getDatabaseManager(), (con)-> {
            boolean deleted = table.delete(null, false, 0);
            assertFalse(deleted);
        });
    }

    @Test
    public void testDeleteNotInTransaction() {
        assertThrows(IllegalStateException.class, () -> table.delete(getAllLatest().get(1), false, 0));
    }

    @Test
    public void testDeleteNothingForNonexistentEntity() {
        DbUtils.inTransaction(getDatabaseManager(), (con)-> {
            boolean deleted = table.delete(valueToInsert(), false, Integer.MAX_VALUE);
            assertFalse(deleted);
        });
    }

    @Test
    public void testDeleteAllForHeightLessThanEntityHeight() {
        DbUtils.inTransaction(getDatabaseManager(), (con)-> {
            List<T> valuesToDelete = sortByHeightAsc(groupByDbKey().values().stream().findAny().get());
            table.delete(valuesToDelete.get(0), false, valuesToDelete.get(0).getHeight());
            try {
                List<T> all = table.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
                List<T> expected = new ArrayList<>(getAll());
                expected.removeAll(valuesToDelete);
                expected = sortByHeightAsc(expected);
                assertEquals(expected, all);
            }
            catch (SQLException e) {
                e.printStackTrace();
            }

        });
    }

    @Test
    public void testDeleteAndKeepInCache() {
        T valueToDelete = getAllLatest().get(1);
        DbUtils.inTransaction(extension, (con)-> {
            table.get(table.getDbKeyFactory().newKey(valueToDelete));
            valueToDelete.setHeight(valueToDelete.getHeight() + 1);
            boolean deleted = table.delete(valueToDelete, true, valueToDelete.getHeight());
            assertTrue(deleted, "Value should be deleted");
            T deletedValue = table.get(table.getDbKeyFactory().newKey(valueToDelete));
            valueToDelete.setHeight(valueToDelete.getHeight() - 1);
            assertInCache(valueToDelete);
            assertEquals(valueToDelete, deletedValue);
        });
        T deletedValue = table.get(table.getDbKeyFactory().newKey(valueToDelete));
        assertNull(deletedValue, "Deleted value should not be returned by get call after cache clear");
    }

}
