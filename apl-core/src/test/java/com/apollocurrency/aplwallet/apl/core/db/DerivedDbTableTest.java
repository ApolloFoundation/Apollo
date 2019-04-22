/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public abstract class DerivedDbTableTest<T extends DerivedEntity> {
    @RegisterExtension
    DbExtension extension = new DbExtension();

    DerivedDbTable<T> derivedDbTable;
    Class<T> clazz;

    public DerivedDbTableTest(DerivedDbTable<T> derivedDbTable, Class<T> clazz) {
        this.derivedDbTable = derivedDbTable;
        this.clazz = clazz;
        assertTrue(getHeights(getAllExpectedData()).size() >= 3, "Expected >= 3 data entries with different heights");
    }

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
        List<T> all = sortByHeight(getAllExpectedData());
        List<Integer> heights = getHeights(all);
        DbUtils.inTransaction(extension, (con) -> derivedDbTable.rollback(heights.get(0)));

        assertEquals(all, derivedDbTable.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE));
    }


    @Test
    public void testRollbackToFirstEntry() throws SQLException {
        List<T> all = getAllExpectedData();
        List<Integer> heights = getHeights(all);
        Integer rollbackHeight = heights.get(heights.size() - 1);
        DbUtils.inTransaction(extension, (con) -> derivedDbTable.rollback(rollbackHeight));
        assertEquals(sortByHeight(getAllExpectedData(), rollbackHeight), derivedDbTable.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE));
    }

    public List<T> sortByHeight(List<T> list, int maxHeight) {
        return list
                .stream()
                .filter(d-> d.getHeight() < maxHeight)
                .sorted(Comparator.comparing(DerivedEntity::getHeight).thenComparing(DerivedEntity::getDbId).reversed())
                .collect(Collectors.toList());
    }

    public List<T> sortByHeight(List<T> list) {
        return sortByHeight(list, Integer.MAX_VALUE);
    }

    public List<Integer> getHeights(List<T> list) {
        return list
                .stream()
                .map(DerivedEntity::getHeight)
                .sorted(Comparator.reverseOrder())
                .distinct()
                .collect(Collectors.toList());
    }

    protected abstract List<T> getAllExpectedData();
}
