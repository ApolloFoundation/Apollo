/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class DerivedDbTableTest<T extends DerivedEntity> {
    @RegisterExtension
    DbExtension extension = new DbExtension();

    DerivedDbTable<T> derivedDbTable;
    Class<T> clazz;

    public DerivedDbTableTest(Class<T> clazz) {
        this.clazz = clazz;

    }

    @BeforeEach
    public void setUp() {
        derivedDbTable = getDerivedDbTable();
        assertTrue(getHeights().size() >= 3, "Expected >= 3 data entries with different heights");
    }

    public abstract DerivedDbTable<T> getDerivedDbTable();

    public DatabaseManager getDatabaseManager() {
        return extension.getDatabaseManger();
    }

    @Test
    public void testGetAll() throws SQLException {
        List<T> all = derivedDbTable.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();

        assertEquals(getAllExpectedData(), all);
    }

    @Test
    public void testTrim() throws SQLException {
        DbUtils.inTransaction(extension, (con) -> derivedDbTable.trim(0));

        List<T> all = derivedDbTable.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        assertEquals(getAllExpectedData(), all);
    }

    @Test
    public void testDelete() {
        assertThrows(UnsupportedOperationException.class, () -> derivedDbTable.delete(mock(clazz)));
    }

    @Test
    public void testTruncate() throws SQLException {
        DbUtils.inTransaction(extension, (con) -> derivedDbTable.truncate());

        List<T> all = derivedDbTable.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();

        assertTrue(all.isEmpty(), "Table should not contain any records after 'truncate' operation");
    }

    @Test
    public void testInsert() {
        assertThrows(UnsupportedOperationException.class, ()-> DbUtils.inTransaction(extension, (con) -> derivedDbTable.insert(mock(clazz))));
    }

    @Test
    public void testRollbackToNegativeHeight() throws SQLException {
        DbUtils.inTransaction(extension, (con) -> derivedDbTable.rollback(-1));
        List<T> all = derivedDbTable.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        assertTrue(all.isEmpty(), "Derived table " + derivedDbTable.toString() + " should not have any entries after rollback to -1 height");
    }

    @Test
    public void testRollbackToLastEntry() throws SQLException {
        List<Integer> heights = getHeights();
        DbUtils.inTransaction(extension, (con) -> derivedDbTable.rollback(heights.get(0)));

        List<T> all = getAllExpectedData();
        List<T> actualValues = derivedDbTable.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        assertEquals(all, actualValues);
    }

    @Test
    public void testRollbackToFirstEntry() throws SQLException {
        List<Integer> heights = getHeights();
        Integer rollbackHeight = heights.get(heights.size() - 1);
        DbUtils.inTransaction(extension, (con) -> derivedDbTable.rollback(rollbackHeight));
        assertEquals(sublistByHeight(getAllExpectedData(), rollbackHeight), derivedDbTable.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues());
    }

    public List<T> sublistByHeightDesc(List<T> list, int maxHeight) {
        return list
                .stream()
                .filter(d-> d.getHeight() <= maxHeight)
                .sorted(Comparator.comparing(DerivedEntity::getHeight).thenComparing(DerivedEntity::getDbId).reversed())
                .collect(Collectors.toList());
    }


    public List<T> sublistByHeight(List<T> list, int maxHeight) {
        return list
                .stream()
                .filter(d-> d.getHeight() <= maxHeight)
                .sorted(Comparator.comparing(DerivedEntity::getHeight).thenComparing(DerivedEntity::getDbId))
                .collect(Collectors.toList());
    }

    public List<T> sortByHeightDesc(List<T> list) {
        return sublistByHeightDesc(list, Integer.MAX_VALUE);
    }

    public List<T> sortByHeightAsc(List<T> list) {
        return list
                .stream()
                .sorted(Comparator.comparing(DerivedEntity::getHeight).thenComparing(DerivedEntity::getDbId))
                .collect(Collectors.toList());
    }

    public List<Integer> getHeights() {
        return getHeights(getAllExpectedData());
    }
    public List<Integer> getHeights(List<T> l) {
        return l
                .stream()
                .map(DerivedEntity::getHeight)
                .sorted(Comparator.reverseOrder())
                .distinct()
                .collect(Collectors.toList());
    }

    public void assertInCache(KeyFactory<T> keyFactory, List<T> values) {
        List<T> cachedValues = getCache(keyFactory.newKey(values.get(0)));
        assertEquals(values, cachedValues);
    }

    public void assertNotInCache(KeyFactory<T> keyFactory, List<T> values) {
        List<T> cachedValues = getCache(keyFactory.newKey(values.get(0)));
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


    public void removeFromCache(KeyFactory<T> keyFactory, List<T> values) {
        DbKey dbKey = keyFactory.newKey(values.get(0));
        Map<DbKey, Object> cache = extension.getDatabaseManger().getDataSource().getCache(derivedDbTable.getTableName());
        cache.remove(dbKey);
    }
    public Map<DbKey, List<T>> groupByDbKey(KeyFactory<T> keyFactory) {
        List<T> allExpectedData = getAllExpectedData();
        return allExpectedData
                .stream()
                .collect(Collectors.groupingBy(keyFactory::newKey));
    }
    protected Map.Entry<DbKey, List<T>> getEntryWithListOfSize(KeyFactory<T> keyFactory, int size) {
        return groupByDbKey(keyFactory)
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() == size)
                .findFirst()
                .get();
    }

    protected abstract List<T> getAllExpectedData();
}
