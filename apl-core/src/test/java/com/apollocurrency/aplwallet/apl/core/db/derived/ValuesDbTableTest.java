/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.derived;

import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public abstract class ValuesDbTableTest<T extends DerivedEntity> extends DerivedDbTableTest<T> {
    private DbKey INCORRECT_DB_KEY = new DbKey() {
        @Override
        public int setPK(PreparedStatement pstmt) throws SQLException {
            return setPK(pstmt, 1);
        }

        @Override
        public int setPK(PreparedStatement pstmt, int index) throws SQLException {
            pstmt.setLong(index, Long.MIN_VALUE);
            return index + 1;
        }
    };

    public ValuesDbTableTest(Class<T> clazz) {
        super(clazz);

    }

    public ValuesDbTable<T> getTable() {
        return (ValuesDbTable<T>) getDerivedDbTable();
    }

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        table = getTable();
        assertNotNull(getEntryWithListOfSize(table.getDbKeyFactory(),2));
        assertNotNull(getEntryWithListOfSize(table.getDbKeyFactory(),3));
    }

    ValuesDbTable<T> table ;

    @Test
    public void testGetByDbKey() {
        Map.Entry<DbKey, List<T>> entry = getEntryWithListOfSize(table.getDbKeyFactory(), 3);
        List<T> values = sortByHeightAsc(entry.getValue());
        DbKey dbKey = entry.getKey();
        List<T> result = table.get(dbKey);
        assertEquals(values, result);
    }

    @Test
    public void testGetByUnknownDbKey() {
        List<T> actual = table.get(INCORRECT_DB_KEY);
        assertTrue(actual.isEmpty(), "No records should be found at dbkey -1");

    }

    @Override
    public void testInsert() {
        List<T> toInsert = dataToInsert();
        DbUtils.inTransaction(extension, (con) -> {
            table.insert(toInsert);
            //check cache in transaction
            assertInCache(toInsert);
        });
        //check db
        assertNotInCache(toInsert);
        List<T> retrievedData = table.get(table.getDbKeyFactory().newKey(toInsert.get(0)));
        assertEquals(toInsert, retrievedData);
    }

    @Test
    public void testGetInCached() {
        Map.Entry<DbKey, List<T>> entry = getEntryWithListOfSize(table.getDbKeyFactory(), 2);
        List<T> values = sortByHeightAsc(entry.getValue());
        DbKey dbKey = entry.getKey();
        DbUtils.inTransaction(extension, con -> {
            List<T> actual = table.get(dbKey);
            assertInCache(values);
            assertEquals(values, actual);
        });
        assertNotInCache(values);
    }

    @Test
    public void testGetFromDeletedCache() {
        Map.Entry<DbKey, List<T>> entry = getEntryWithListOfSize(table.getDbKeyFactory(), 2);
        List<T> values = sortByHeightAsc(entry.getValue());
        DbKey dbKey = entry.getKey();
        DbUtils.inTransaction(extension, con -> {
            List<T> actual = table.get(dbKey);
            assertInCache(values);
            assertEquals(values, actual);
            removeFromCache(values);
            assertNotInCache(values);
        });
        assertNotInCache(values);
    }


    @Test
    public void testInsertNotInTransaction() {
        assertThrows(IllegalStateException.class, () -> table.insert(Collections.emptyList()));
    }

    @Test
    public void testInsertWithDifferentDbKeys() {
        List<T> dataToInsert = dataToInsert();
        T t = dataToInsert.get(0);
        t.setDbKey(INCORRECT_DB_KEY);
        assertThrows(IllegalArgumentException.class, () -> DbUtils.inTransaction(extension, (con) -> table.insert(dataToInsert)));
    }

    @Test
    public void testTrimNothing() throws SQLException {
        DbUtils.inTransaction(extension, (con) -> table.trim(0));
        List<T> allValues = table.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        assertEquals(getAll(), allValues);
    }

    @Override
    @Test
    public void testTrim() throws SQLException {
        if (table.isMultiversion()) {
            int trimHeight = getTrimHeight();
            DbUtils.inTransaction(extension, con-> table.trim(trimHeight));
            List<T> allValues = table.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
            assertEquals(getAllAfterTrim(), allValues);
        } else {
            super.testTrim();
        }
    }


    protected Map<DbKey, List<T>> getDuplicates() {
        return groupByDbKey(table.getDbKeyFactory())
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue()
                        .stream()
                        .map(DerivedEntity::getHeight)
                        .distinct()
                        .count() > 1)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    protected int getTrimHeight() {
        Map<DbKey, List<T>> duplicates = getDuplicates();
        Integer maxDuplicatesHeight = duplicates.values()
                .stream()
                .map(l -> l
                        .stream()
                        .map(DerivedEntity::getHeight)
                        .max(Comparator.naturalOrder())
                        .get())
                .max(Comparator.naturalOrder())
                .get();

        return maxDuplicatesHeight + 1;
    }

    protected List<T> getAllAfterTrim() {
        Map<DbKey, List<T>> duplicates = getDuplicates();
        List<T> removed = new ArrayList<>();
                duplicates.values()
                        .forEach(l ->
                        {
                            Integer maxHeight = l.stream().map(DerivedEntity::getHeight).max(Comparator.naturalOrder()).get();
                            l.stream().filter(e-> e.getHeight() < maxHeight).forEach(removed::add);
                        });
        List<T> allExpectedData = new ArrayList<>(getAll());
        removed.forEach(allExpectedData::remove);
        return allExpectedData;
    }

    public void assertInCache(List<T> values) {
        List<T> cachedValues = getCache(table.getDbKeyFactory().newKey(values.get(0)));
        assertEquals(values, cachedValues);
    }

    public void assertNotInCache(List<T> values) {
        List<T> cachedValues = getCache(table.getDbKeyFactory().newKey(values.get(0)));
        assertNotEquals(values, cachedValues);
    }

    public  List<T> getCache(DbKey dbKey) {
        if (!extension.getDatabaseManger().getDataSource().isInTransaction()) {
            return DbUtils.getInTransaction(extension, (con) -> getCacheInTransaction(dbKey));
        } else {
            return getCacheInTransaction(dbKey);
        }
    }

    public List<T> getCacheInTransaction(DbKey dbKey) {
        Map<DbKey, Object> cache = extension.getDatabaseManger().getDataSource().getCache(derivedDbTable.getTableName());
        return (List<T>) cache.get(dbKey);
    }


    public void removeFromCache(List<T> values) {
        DbKey dbKey = table.getDbKeyFactory().newKey(values.get(0));
        Map<DbKey, Object> cache = extension.getDatabaseManger().getDataSource().getCache(derivedDbTable.getTableName());
        cache.remove(dbKey);
    }

    protected abstract List<T> dataToInsert();

}
