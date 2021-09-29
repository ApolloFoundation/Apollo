/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.derived;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class BasicDbTableTest<T extends DerivedEntity> extends DerivedDbTableTest<T> {
    BasicDbTable<T> table;

    public BasicDbTableTest(Class<T> clazz) {
        super(clazz);
    }

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        table = (BasicDbTable<T>) getDerivedDbTable();
    }

    public List<T> getDeletedMultiversionRecord() {
        throw new UnsupportedOperationException("deleted multiversion record is not provided");
    }

    @Test
    public void testTrimForMaxHeight() throws SQLException {
        int maxHeight = sortByHeightDesc(getAll()).get(0).getHeight();
        testOrdinaryOrMultiversionTrim(maxHeight);
    }

    @Override
    @Test
    public void testTrimForZeroHeight() throws SQLException {
        testOrdinaryOrMultiversionTrim(0);
    }

    @Test
    public void testTrimForMaxHeightInclusive() throws SQLException {
        int maxHeight = sortByHeightDesc(getAll()).get(0).getHeight() + 1;
        testOrdinaryOrMultiversionTrim(maxHeight);
    }

    public void testOrdinaryOrMultiversionTrim(int height) throws SQLException {
        if (table.isMultiversion()) {
            testMultiversionTrim(height, Integer.MAX_VALUE);
        } else {
            testTrim(height, Integer.MAX_VALUE);
        }
    }

    public void testOrdinaryOrMultiversionRollback(int height) throws SQLException {
        if (table.isMultiversion()) {
            testMultiversionRollback(height);
        } else {
            testRollback(height);
        }
    }

    @Test
    public void testTrimForMaxHeightExclusive() throws SQLException {
        int maxHeight = sortByHeightDesc(getAll()).get(0).getHeight() - 1;
        testOrdinaryOrMultiversionTrim(maxHeight);
    }

    @Test
    public void testTrimAllDeleted() throws SQLException {
        if (table.isMultiversion() && table.supportDelete()) {
            List<T> deleted = getDeletedMultiversionRecord();
            int height = deleted.get(deleted.size() - 1).getHeight() + 1;
            testMultiversionTrim(height, Integer.MAX_VALUE);
        }
    }

    @Test
    public void testTrimDeletedEqualAtLastDeletedHeight() throws SQLException {
        if (table.isMultiversion() && table.supportDelete()) {
            List<T> deleted = getDeletedMultiversionRecord();
            int height = deleted.get(deleted.size() - 1).getHeight();
            testMultiversionTrim(height, Integer.MAX_VALUE);
        }
    }

    @Test
    public void testTrimDeletedAtHeightLessThanLastDeletedRecord() throws SQLException {
        if (table.isMultiversion() && table.supportDelete()) {
            List<T> deleted = getDeletedMultiversionRecord();
            int height = deleted.get(deleted.size() - 1).getHeight() - 1;
            testMultiversionTrim(height, Integer.MAX_VALUE);
        }
    }

    @Test
    public void testTrimMiddleHeight() throws SQLException {
        List<Integer> heights = getHeights();
        int middleHeight = (heights.get(0) + heights.get(heights.size() - 1)) / 2;
        testOrdinaryOrMultiversionTrim(middleHeight);

    }

    @Test
    public void testTrimForMiddleRecord() throws SQLException {
        List<T> all = getAll();

        int middleHeight = all.get(all.size() / 2).getHeight();
        testOrdinaryOrMultiversionTrim(middleHeight);
    }

    @Test
    public void testTrimForThreeUpdatedRecords() throws SQLException {
        if (table.isMultiversion()) {
            List<T> list = sortByHeightDesc(getEntryWithListOfSize(getAll(), table.getDbKeyFactory(), 3, true).getValue());
            testOrdinaryOrMultiversionTrim(list.get(0).getHeight());
        }
    }

    @Test
    public void testTrimNothingForThreeUpdatedRecords() throws SQLException {
        if (table.isMultiversion()) {
            List<T> list = sortByHeightDesc(getEntryWithListOfSize(getAll(), table.getDbKeyFactory(), 3, true).getValue());
            testOrdinaryOrMultiversionTrim(list.get(1).getHeight());
        }
    }

    @Test
    public void testTrimOutsideTransaction() {
        if (table.isMultiversion()) {
            Assertions.assertThrows(IllegalStateException.class, () -> table.trim(0));
        }
    }

    @Test
    public void testRollbackOutsideTransaction() {
        Assertions.assertThrows(IllegalStateException.class, () -> table.rollback(0));
    }

    @Test
    public void testRollbackDeletedEntries() throws SQLException {
        if (table.isMultiversion() && table.supportDelete()) {
            List<T> deleted = getDeletedMultiversionRecord();
            int height = deleted.get(deleted.size() - 1).getHeight() - 1;
            testOrdinaryOrMultiversionRollback(height);
        }
    }

    @Test
    public void testRollbackForThreeUpdatedRecords() throws SQLException {
        // fired FTS event(s) : fullTextOperationDataEvent.select(new AnnotationLiteral<TrimEvent>() {}).fire(operationData);
        if (table.isMultiversion()) {
            int height = sortByHeightDesc(getEntryWithListOfSize(getAll(), table.getDbKeyFactory(), 3, true).getValue()).get(0).getHeight() - 1;
            testOrdinaryOrMultiversionRollback(height);
        }
    }

    @Test
    public void testRollbackNothingForThreeUpdatedRecords() throws SQLException {
        if (table.isMultiversion()) {
            int height = sortByHeightDesc(getEntryWithListOfSize(getAll(), table.getDbKeyFactory(), 3, true).getValue()).get(0).getHeight();
            testOrdinaryOrMultiversionRollback(height);
        }
    }

    @Test
    public void testRollbackEntirelyForTwoRecords() throws SQLException {
        // fired FTS event(s) : fullTextOperationDataEvent.select(new AnnotationLiteral<TrimEvent>() {}).fire(operationData);
        if (table.isMultiversion()) {
            int height = sortByHeightDesc(getEntryWithListOfSize(getAll(), table.getDbKeyFactory(), 2, true).getValue()).get(1).getHeight() - 1;
            testOrdinaryOrMultiversionRollback(height);
        }
    }

    @Override
    @Test
    public void testRollbackToFirstEntry() throws SQLException {
        List<T> all = getAll();
        T first = all.get(all.size() - 1);
        testOrdinaryOrMultiversionRollback(first.getHeight());
    }

    public void testMultiversionRollback(int height) throws SQLException {
        List<T> all = getAll();
        List<T> rollbacked = all.stream().filter(t -> t.getHeight() > height).collect(Collectors.toList());
        Map<DbKey, List<T>> dbKeyListMapRollbacked = groupByDbKey(rollbacked, table.getDbKeyFactory());

        List<T> expected = all.stream().filter(t -> t.getHeight() <= height).collect(Collectors.toList());
        Map<DbKey, List<T>> expectedDbKeyListMap = groupByDbKey(expected, table.getDbKeyFactory());
        dbKeyListMapRollbacked.entrySet()
            .stream()
            .filter(e -> expectedDbKeyListMap.containsKey(e.getKey()) && expectedDbKeyListMap.get(e.getKey()).size() != 0)
            .map(Map.Entry::getKey)
            .map(expectedDbKeyListMap::get)
            .forEach((e) -> {
                int maxHeight = e.stream().map(DerivedEntity::getHeight).max(Comparator.naturalOrder()).get();
                e.stream().filter(el -> el.getHeight() == maxHeight).forEach(el -> ((VersionedDerivedEntity) el).setLatest(true));
            });
        expected = sortByHeightAsc(expected);

        DbUtils.inTransaction(extension, (con) -> table.rollback(height));
        List<T> values = table.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        assertEquals(expected, values);
    }

    public void testMultiversionTrim(int height, int blockchainHeight) throws SQLException {
        List<T> all = getAll();
        Map<DbKey, List<T>> dbKeyListMap = groupByDbKey();
        List<T> trimmed = new ArrayList<>();
        dbKeyListMap.forEach(((key, value) -> {
            List<T> list = value.stream().filter(t -> t.getHeight() < height).collect(Collectors.toList());
            if (list.size() != 0) {
                if (table.supportDelete() && list.stream().noneMatch(e -> ((VersionedDerivedEntity) e).isLatest()) && value.size() == list.size()) { //delete deleted
                    trimmed.addAll(list);
                } else {
                    Integer maxHeight = list.stream().map(DerivedEntity::getHeight).max(Comparator.naturalOrder()).get(); //delete all not latest duplicates
                    List<T> toTrim = list.stream().filter(el -> el.getHeight() < maxHeight).collect(Collectors.toList());
                    trimmed.addAll(toTrim);
                }
            }
        }));
        List<T> expected = new ArrayList<>(all);
        for (T t : trimmed) {
            expected.remove(t);
        }
        expected = sortByHeightAsc(expected);
        DbUtils.inTransaction(extension, (con) -> table.trim(height));
        List<T> values = table.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        assertEquals(expected, values);
    }

    Map<DbKey, List<T>> groupByDbKey() {
        return groupByDbKey(table.getDbKeyFactory());
    }

}
