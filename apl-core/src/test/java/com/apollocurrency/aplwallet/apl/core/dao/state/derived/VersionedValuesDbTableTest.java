/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.derived;


import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDeletableEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public abstract class VersionedValuesDbTableTest<T extends VersionedDeletableEntity> extends ValuesDbTableTest<T> {

    VersionedDeletableValuesDbTable<T> table;

    public VersionedValuesDbTableTest(Class<T> clazz) {
        super(clazz);
    }

    @BeforeEach
    public void setUp() {
        super.setUp();
        table = (VersionedDeletableValuesDbTable<T>) getTable();
    }

    @Override
    protected List<T> getAllLatest() {
        return sortByHeightDesc(getAll().stream().filter(VersionedDerivedEntity::isLatest).collect(Collectors.toList()));
    }

    @Override
    @Test
    public void testDelete() {
        DbUtils.inTransaction(getDatabaseManager(), (con) -> {

            List<T> allLatest = getAllLatest();
            Map.Entry<DbKey, List<T>> valuesToDelete = getEntryWithListOfSize(allLatest, table.getDbKeyFactory(), 3);
            int deleteHeight = sortByHeightDesc(valuesToDelete.getValue()).get(0).getHeight() + 1;
            boolean deleted = table.delete(valuesToDelete.getValue().get(0), deleteHeight);
            assertTrue(deleted);
            List<T> values = table.get(valuesToDelete.getKey());
            assertTrue(values.isEmpty());
            List<T> all;
            try {
                all = table.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
            assertFalse(all.containsAll(valuesToDelete.getValue()));
            List<T> expectedDeleted = valuesToDelete.getValue().stream().peek(v -> v.setLatest(false)).collect(Collectors.toList());
            assertTrue(all.containsAll(expectedDeleted));
            long lastDbId = sortByHeightDesc(getAll()).get(0).getDbId();
            expectedDeleted = sortByHeightAsc(valuesToDelete.getValue());
            for (T t : expectedDeleted) {
                t.setHeight(deleteHeight);
                t.setDbId(++lastDbId);
            }
            assertTrue(all.containsAll(expectedDeleted));
        });
    }

    @Test
    public void testDeleteOutsideTransaction() {
        assertThrows(IllegalStateException.class, () -> table.delete(mock(clazz), 0));
    }

    @Test
    public void testDeleteForNull() {
        assertFalse(table.delete(null, 0));
    }

    @Test
    public void testDeleteForSameHeight() {
        DbUtils.inTransaction(getDatabaseManager(), (con) -> {

            List<T> allLatest = getAllLatest();
            Map.Entry<DbKey, List<T>> valuesToDelete = getEntryWithListOfSize(allLatest, table.getDbKeyFactory(), 2);
            int deleteHeight = sortByHeightDesc(valuesToDelete.getValue()).get(0).getHeight();
            boolean deleted = table.delete(valuesToDelete.getValue().get(0), deleteHeight);
            assertTrue(deleted);
            List<T> values = table.get(valuesToDelete.getKey());
            assertTrue(values.isEmpty());
            List<T> all;
            try {
                all = table.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
            assertEquals(getAll().size(), all.size());
            assertFalse(all.containsAll(valuesToDelete.getValue()));
            List<T> expectedDeleted = valuesToDelete.getValue().stream().peek(v -> v.setLatest(false)).collect(Collectors.toList());
            assertTrue(all.containsAll(expectedDeleted));
            assertEquals(all.size(), getAll().size());
        });
    }

    @Test
    public void testDeleteValuesAtHeightBelowLastValueToDelete() {
        DbUtils.inTransaction(getDatabaseManager(), (con) -> {
            Map.Entry<DbKey, List<T>> valuesToDelete = getEntryWithListOfSize(getAll(), table.getDbKeyFactory(), 6);
            int deleteHeight = sortByHeightAsc(valuesToDelete.getValue()).get(0).getHeight();
            boolean deleted = table.delete(valuesToDelete.getValue().get(0), deleteHeight);
            assertTrue(deleted);
            List<T> values = table.get(valuesToDelete.getKey());
            assertTrue(values.isEmpty());
            List<T> all;
            try {
                all = table.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
            assertEquals(getAll().size() - 6, all.size());
            assertFalse(all.containsAll(valuesToDelete.getValue()));
            List<T> expectedDeleted = valuesToDelete.getValue().stream().peek(v -> v.setLatest(false)).collect(Collectors.toList());
            assertFalse(all.containsAll(expectedDeleted));
        });
    }

    @Test
    public void testDeleteForIncorrectDbKey() {
        DbUtils.inTransaction(getDatabaseManager(), (con) -> {
            T valueToDelete = getAllLatest().get(0);
            valueToDelete.setDbKey(mock(DbKey.class));
            assertThrows(RuntimeException.class, () -> table.delete(valueToDelete, valueToDelete.getHeight() + 1));
        });
    }

    @Override
    public List<T> getDeletedMultiversionRecord() {
        return sortByHeightAsc(groupByDbKey()
            .entrySet()
            .stream()
            .filter(e -> e.getValue().size() >= 2
                && !e.getValue()
                .stream()
                .allMatch(VersionedDerivedEntity::isLatest))
            .map(e -> e.getValue().get(0))
            .collect(Collectors.toList()));
    }

    @Test
    public void testInsertWithSameKey() throws SQLException {
        List<T> toInsert = sortByHeightAsc(getEntryWithListOfSize(getAllLatest(), table.getDbKeyFactory(), 3).getValue());
        List<Long> dbIds = toInsert.stream().map(T::getDbId).collect(Collectors.toList());

        DbUtils.inTransaction(getDatabaseManager(), (con) -> {
            List<T> values = table.get(table.getDbKeyFactory().newKey(toInsert.get(0)));

            assertEquals(toInsert, values);
            toInsert.forEach(t -> t.setHeight(t.getHeight() + 1));
            table.insert(toInsert);
            //check cache in transaction

        });
        toInsert.forEach(t -> t.setHeight(t.getHeight() - 1));
        //check db
        List<T> retrievedData = table.get(table.getDbKeyFactory().newKey(toInsert.get(0)));
        long lastDbId = sortByHeightDesc(getAll()).get(0).getDbId();
        for (T t : toInsert) {
            t.setHeight(t.getHeight() + 1);
            t.setDbId(++lastDbId);
        }
        assertEquals(toInsert, retrievedData);
        List<T> allValues = table.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();

        assertTrue(allValues.containsAll(toInsert));
        for (int i = 0; i < toInsert.size(); i++) {
            T t = toInsert.get(i);
            t.setHeight(t.getHeight() - 1);
            t.setLatest(false);
            t.setDbId(dbIds.get(i));
        }
        assertTrue(allValues.containsAll(toInsert));
    }
}
