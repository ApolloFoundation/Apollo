/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

        assertEquals(getAll(), all);
    }

    @Test
    public void testTrim() throws SQLException {
        DbUtils.inTransaction(extension, (con) -> derivedDbTable.trim(0));

        List<T> all = derivedDbTable.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        assertEquals(getAll(), all);
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

        List<T> all = getAll();
        List<T> actualValues = derivedDbTable.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        assertEquals(all, actualValues);
    }

    @Test
    public void testRollbackToFirstEntry() throws SQLException {
        List<Integer> heights = getHeights();
        Integer rollbackHeight = heights.get(heights.size() - 1);
        DbUtils.inTransaction(extension, (con) -> derivedDbTable.rollback(rollbackHeight));
        assertEquals(sublistByHeight(getAll(), rollbackHeight), derivedDbTable.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues());
    }



    public List<T> sublistByHeightDesc(List<T> list, int maxHeight) {
        return list
                .stream()
                .filter(d-> d.getHeight() <= maxHeight)
                .sorted(Comparator.comparing(DerivedEntity::getHeight).thenComparing(DerivedEntity::getDbId).reversed())
                .collect(toList());
    }


    public List<T> sublistByHeight(List<T> list, int maxHeight) {
        return list
                .stream()
                .filter(d-> d.getHeight() <= maxHeight)
                .sorted(Comparator.comparing(DerivedEntity::getHeight).thenComparing(DerivedEntity::getDbId))
                .collect(toList());
    }

    public List<T> sortByHeightDesc(List<T> list) {
        return sublistByHeightDesc(list, Integer.MAX_VALUE);
    }

    public List<T> sortByHeightAsc(List<T> list) {
        return list
                .stream()
                .sorted(Comparator.comparing(DerivedEntity::getHeight).thenComparing(DerivedEntity::getDbId))
                .collect(toList());
    }

    public List<Integer> getHeights() {
        return getHeights(getAll());
    }
    public List<Integer> getHeights(List<T> l) {
        return l
                .stream()
                .map(DerivedEntity::getHeight)
                .sorted(Comparator.reverseOrder())
                .distinct()
                .collect(toList());
    }

    public Map<DbKey, List<T>> groupByDbKey(List<T> data, KeyFactory<T> keyFactory) {
        return data
                .stream()
                .collect(Collectors.groupingBy(keyFactory::newKey,
                        Collectors.collectingAndThen(toList(),
                                l-> l.stream().sorted(
                                        Comparator.comparing(DerivedEntity::getHeight)
                                                .thenComparing(DerivedEntity::getDbId)
                                                .reversed()).collect(toList()))));
    }

    public Map<DbKey, List<T>> groupByDbKey(KeyFactory<T> keyFactory) {
        return groupByDbKey(getAll(), keyFactory);
    }

    protected Map.Entry<DbKey, List<T>> getEntryWithListOfSize(KeyFactory<T> keyFactory, int size) {
        return getEntryWithListOfSize(getAll(), keyFactory, size);
    }
    protected Map.Entry<DbKey, List<T>> getEntryWithListOfSize(List<T> data, KeyFactory<T> keyFactory, int size) {
        return groupByDbKey(data, keyFactory)
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() == size)
                .findFirst()
                .get();
    }

    protected abstract List<T> getAll();
}
