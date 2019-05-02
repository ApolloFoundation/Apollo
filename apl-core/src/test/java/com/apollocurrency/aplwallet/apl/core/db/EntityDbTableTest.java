/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.app.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class EntityDbTableTest<T extends DerivedEntity> extends DerivedDbTableTest<T> {
    private DbKey UNKNOWN_DB_KEY = new DbKey() {
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
    private DbKey THROWING_DB_KEY = createThrowingKey();

    private DbKey createThrowingKey() {
        DbKey throwingKey = mock(DbKey.class);
        try {
            doThrow(SQLException.class).when(throwingKey).setPK(any(PreparedStatement.class));
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return throwingKey;
    }

    public EntityDbTableTest(Class<T> clazz) {
        super(clazz);
    }

    EntityDbTable<T> table;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        table = (EntityDbTable<T>) getDerivedDbTable();
    }

    @Test
    public void testGetByDbKey() {
        List<T> all = getAllLatest();
        T expected = all.get(1);
        DbKey dbKey = table.getDbKeyFactory().newKey(expected);
        T actual = table.get(dbKey);
        assertEquals(expected, actual);
    }

    @Test
    public void testByUnknownDbKey() {
        T unknownValue = table.get(UNKNOWN_DB_KEY);

        assertNull(unknownValue, "value with unknown db key should be null");
    }

    @Test
    public void testGetFromCache() {
        List<T> all = getAllLatest();
        T expected = all.get(2);
        DbKey dbKey = table.getDbKeyFactory().newKey(expected);
        DbUtils.inTransaction(extension, (con) -> {
            T actual = table.get(dbKey, true);
            assertEquals(expected, actual);
            assertInCache(expected);
        });
        assertNotInCache(expected);
        T actual = table.get(dbKey, true);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetWithoutCache() {
        List<T> all = getAllLatest();
        T expected = all.get(0);
        DbKey dbKey = table.getDbKeyFactory().newKey(expected);
        DbUtils.inTransaction(extension, (con) -> {
            T actual = table.get(dbKey, false);
            assertEquals(expected, actual);
            assertNotInCache(expected);
        });
        assertNotInCache(expected);
        T actual = table.get(dbKey, false);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetFromCacheWithoutTransaction() {
        List<T> all = getAllLatest();
        T expected = all.get(1);
        DbKey dbKey = table.getDbKeyFactory().newKey(expected);
        T actual = table.get(dbKey, true);
        assertEquals(expected, actual);
        assertNotInCache(expected);
    }

    @Test
    public void testGetWithSqlException() throws SQLException {

        assertThrows(RuntimeException.class, () -> table.get(THROWING_DB_KEY));
    }

    @Test
    public void testGetByHeight() {

        if (table.isMultiversion()) {
            Map.Entry<DbKey, List<T>> entries = getEntryWithListOfSize(getAllLatest(), table.getDbKeyFactory(), 3);
            List<T> sorted = sortByHeightDesc(entries.getValue());
            T latest = sorted.get(0);
            T notLatest = sorted.get(1);
            T first = sorted.get(2);

            assertNotEquals(latest.getHeight(), notLatest.getHeight());
            assertNotEquals(notLatest.getHeight(), first.getHeight());

            T actual = table.get(entries.getKey(), latest.getHeight() + 1);
            assertEquals(latest, actual);

            actual = table.get(entries.getKey(), latest.getHeight());
            assertEquals(latest, actual);

            actual = table.get(entries.getKey(), latest.getHeight() - 1);
            assertEquals(notLatest, actual);

            actual = table.get(entries.getKey(), notLatest.getHeight());
            assertEquals(latest, actual);

            actual = table.get(entries.getKey(), notLatest.getHeight() - 1);
            assertEquals(first, actual);

            actual = table.get(entries.getKey(), first.getHeight());
            assertEquals(first, actual);

            T deleted = getDeletedMultiversionRecord();

            actual = table.get(table.getDbKeyFactory().newKey(deleted), deleted.getHeight());

            assertNull(actual);

            entries = getEntryWithListOfSize(table.getDbKeyFactory(), 1);

            T expected = entries.getValue().get(0);

            actual = table.get(entries.getKey(), expected.getHeight());
            assertEquals(expected, actual);

            actual = table.get(entries.getKey(), expected.getHeight() + 1);
            assertEquals(expected, actual);
        } else {
            T expected = getAll().get(1);
            T actual = table.get(table.getDbKeyFactory().newKey(expected), expected.getHeight());
            assertEquals(expected, actual);

            actual = table.get(table.getDbKeyFactory().newKey(expected), expected.getHeight() + 1);
            assertEquals(expected, actual);
        }
    }

    @Test
    public void testGetByHeightForUnknownDbKey() {
        T unknownEntity = table.get(UNKNOWN_DB_KEY, Integer.MAX_VALUE);
        assertNull(unknownEntity, "Entity with unknown db key should not exist");
    }

    @Test
    public void testGetByHeightWithException() {
        assertThrows(RuntimeException.class, () -> table.get(THROWING_DB_KEY, Integer.MAX_VALUE));
    }

    @Test
    public void testGetByDbClause() {
        T expected;
        if (table.isMultiversion()) {

            expected = getAllLatest().get(0);
        } else {
            expected = getAll().get(2);
        }
        T actual = table.getBy(new DbClause.IntClause("height", DbClause.Op.EQ, expected.getHeight()).and(new DbClause.LongClause("db_id", DbClause.Op.EQ, expected.getDbId())));
        assertEquals(expected, actual);
    }

    @Test
    public void testGetByDbClauseUsingIncorrectDbId() {
        T uknownEntity = table.getBy(new DbClause.LongClause("db_id", DbClause.Op.EQ, Long.MAX_VALUE));
        assertNull(uknownEntity, "Entity with unknown db_id should not exist");
    }

    @Test
    public void testGetByDbClauseWithHeight() {
        T expected;
        if (table.isMultiversion()) {

            expected = getAllLatest().get(1);
        } else {
            expected = getAll().get(2);
        }
        T actual = table.getBy((new DbClause.LongClause("db_id", DbClause.Op.EQ, expected.getDbId())), expected.getHeight());
        assertEquals(expected, actual);
    }

    @Test
    public void testGetByDbClauseWithHeightUsingIncorrectDbId() {
        long incorrectDbId = getIncorrectDbId();

        T uknownEntity = table.getBy(new DbClause.LongClause("db_id", DbClause.Op.EQ, incorrectDbId));
        assertNull(uknownEntity, "Entity with unknown db_id should not exist");
    }

    public long getIncorrectDbId() {
        long incorrectDbId;
        if (table.isMultiversion()) {
            incorrectDbId = getDeletedMultiversionRecord().getDbId();
        } else {
            incorrectDbId = Long.MAX_VALUE;
        }
        return incorrectDbId;
    }

//    public List<T> getAllLatest() {
//        return sortByHeightDesc(groupByDbKey(table.getDbKeyFactory()).values().stream().map(l -> sortByHeightDesc(l).get(0)).collect(Collectors.toList()));
//    }

    @Test
    public void testGetManyByEmptyClause() {
        List<T> all = CollectionUtil.toList(table.getManyBy(DbClause.EMPTY_CLAUSE, 0, Integer.MAX_VALUE));
        List<T> expected = getAllLatest();
        assertEquals(expected, all);
    }


    @Test
    public void testGetManyByEmptyClauseWithOffset() {
        List<T> all = CollectionUtil.toList(table.getManyBy(DbClause.EMPTY_CLAUSE, 2, Integer.MAX_VALUE));
        List<T> allExpectedData = getAllLatest();
        List<T> expected = allExpectedData.subList(2, allExpectedData.size());
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyByEmptyClauseWithLimit() {
        List<T> allExpectedData = getAllLatest();
        List<T> all = CollectionUtil.toList(table.getManyBy(DbClause.EMPTY_CLAUSE, 0, allExpectedData.size() - 2));
        List<T> expected = allExpectedData.subList(0, allExpectedData.size() - 1);
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyByEmptyClauseWithLimitAndOffset() {
        List<T> allExpectedData = getAllLatest();
        List<T> all = CollectionUtil.toList(table.getManyBy(DbClause.EMPTY_CLAUSE, 1, allExpectedData.size() - 2));
        List<T> expected = allExpectedData.subList(1, allExpectedData.size() - 1);
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyByIncorrectClause() {
        List<T> all = CollectionUtil.toList(table.getManyBy(new DbClause.LongClause("db_id", getIncorrectDbId()), 0, Integer.MAX_VALUE));
        assertEquals(0, all.size());
    }

    @Test
    public void testGetManyByHeightAllClause() {
        List<T> allExpectedData = getAllLatest();
        List<T> all = CollectionUtil.toList(table.getManyBy(new DbClause.IntClause("height", DbClause.Op.GTE, 0), 0, allExpectedData.size()));
        List<T> expected = allExpectedData;
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyByHeightWithUpperBound() {
        List<T> allExpectedData = getAllLatest();
        List<Integer> heights = getHeights(allExpectedData);
        int height = heights.get(1);
        List<T> expected = allExpectedData.stream().filter(t -> t.getHeight() < height).collect(Collectors.toList());
        List<T> all = CollectionUtil.toList(table.getManyBy(new DbClause.IntClause("height", DbClause.Op.GTE, 0).and(new DbClause.IntClause("height", DbClause.Op.LT, height)), 0, expected.size() - 1));
        assertEquals(expected, all);
    }


    @Test
    public void testGetManyByHeightInRange() {
        List<T> allExpectedData = getAllLatest();
        List<Integer> heights = getHeights(allExpectedData);
        int upperHeight = heights.get(1);
        int lowerHeight = heights.get(2);
        List<T> expected = allExpectedData.stream().filter(t -> t.getHeight() < upperHeight && t.getHeight() >= lowerHeight).collect(Collectors.toList());
        List<T> all = CollectionUtil.toList(table.getManyBy(new DbClause.IntClause("height", DbClause.Op.GTE, lowerHeight).and(new DbClause.IntClause("height", DbClause.Op.LT, upperHeight)), 0, expected.size() - 1));
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyByHeightInRangeExclusive() {
        List<T> allExpectedData = getAllLatest();
        List<Integer> heights = getHeights(allExpectedData);
        int upperHeight = heights.get(0);
        int lowerHeight = heights.get(2);
        List<T> expected = allExpectedData.stream().filter(t -> t.getHeight() < upperHeight && t.getHeight() > lowerHeight).collect(Collectors.toList());
        List<T> all = CollectionUtil.toList(table.getManyBy(new DbClause.IntClause("height", DbClause.Op.GT, lowerHeight).and(new DbClause.IntClause("height", DbClause.Op.LT, upperHeight)), 0, expected.size() - 1));
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyByHeightWithNotDefinedHeight() {
        List<T> allExpectedData = getAllLatest();
        List<Integer> heights = getHeights(allExpectedData);

        int notExpectedHeight = heights.get(0);
        List<T> expected = allExpectedData.stream().filter(t -> t.getHeight() != notExpectedHeight).collect(Collectors.toList());
        List<T> all = CollectionUtil.toList(table.getManyBy(new DbClause.IntClause("height", DbClause.Op.NE, notExpectedHeight), 0, expected.size() - 1));
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyByHeightWithDefinedHeight() {
        List<T> allExpectedData = getAllLatest();
        List<Integer> heights = getHeights(allExpectedData);

        int expectedHeight = heights.get(1);
        List<T> expected = allExpectedData.stream().filter(t -> t.getHeight() == expectedHeight).collect(Collectors.toList());
        List<T> all = CollectionUtil.toList(table.getManyBy(new DbClause.IntClause("height", DbClause.Op.EQ, expectedHeight), 0, expected.size() - 1));
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyByHeightWithInRangeInclusive() {
        List<T> allExpectedData = getAllLatest();
        List<Integer> heights = getHeights(allExpectedData);
        int upperHeight = heights.get(0);
        int lowerHeight = heights.get(2);
        List<T> expected = allExpectedData.stream().filter(t -> t.getHeight() > lowerHeight && t.getHeight() < upperHeight).collect(Collectors.toList());
        List<T> all = CollectionUtil.toList(table.getManyBy(new DbClause.IntClause("height", DbClause.Op.GT, lowerHeight).and(new DbClause.IntClause("height", DbClause.Op.LT, upperHeight)), 0, expected.size() - 1));
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyByHeightInRangeWithPagination() {
        List<T> allExpectedData = getAllLatest();
        List<Integer> heights = getHeights(allExpectedData);
        int upperHeight = heights.get(1);
        int lowerHeight = heights.get(2);
        List<T> expected = allExpectedData.stream().filter(t -> t.getHeight() <= upperHeight && t.getHeight() >= lowerHeight).collect(Collectors.toList()).subList(0, 1);
        List<T> all = CollectionUtil.toList(table.getManyBy(new DbClause.IntClause("height", DbClause.Op.GTE, lowerHeight).and(new DbClause.IntClause("height", DbClause.Op.LTE, upperHeight)), 0, 0));
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyByClauseWithCustomSort() {
        List<T> allExpectedData = getAllLatest();
        List<T> expected = sortByHeightAsc(allExpectedData);
        List<T> all = CollectionUtil.toList(table.getManyBy(new DbClause.LongClause("db_id", DbClause.Op.GT, 0).and(new DbClause.LongClause("db_id", DbClause.Op.LT, Long.MAX_VALUE)), 0, expected.size() - 1, " ORDER BY db_id "));
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyOnConnectionWithCache() {
        List<T> allExpectedData = sortByHeightDesc(getAllLatest());
        DbUtils.inTransaction(extension, (con) -> {
                    try {
                        PreparedStatement pstm = con.prepareStatement("select * from " + table.getTableName() + " order by height desc, db_id desc");
                        List<T> all = CollectionUtil.toList(table.getManyBy(con, pstm, true));
                        assertEquals(allExpectedData, all);
                        assertListInCache(allExpectedData);
                    }
                    catch (SQLException e) {
                        throw new RuntimeException(e.toString(), e);
                    }
                }
        );
        assertListNotInCache(allExpectedData);
    }
    @Test
    public void testGetManyOnConnectionWithoutCache() {
        List<T> allExpectedData = sortByHeightDesc(getAllLatest());
        DbUtils.inTransaction(extension, (con) -> {
                    try {
                        PreparedStatement pstm = con.prepareStatement("select * from " + table.getTableName() + " order by height desc, db_id desc");
                        List<T> all = CollectionUtil.toList(table.getManyBy(con, pstm, false));
                        assertEquals(allExpectedData, all);
                        assertListNotInCache(allExpectedData);
                    }
                    catch (SQLException e) {
                        throw new RuntimeException(e.toString(), e);
                    }
                }
        );
        assertListNotInCache(allExpectedData);
    }

    @Test
    public void testGetAllWithCache() {
        List<T> all = sortByHeightDesc(getAllLatest());
        testGetAllWithPagination(all, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Test
    public void testGetAllWithPagination() {
        List<T> all = sortByHeightDesc(getAllLatest());
        testGetAllWithPagination(all, 0, all.size() - 1);
    }

    @Test
    public void testGetAllWithPaginationExcludingFirst() {
        List<T> all = sortByHeightDesc(getAllLatest());
        List<T> expected = all.subList(1, all.size());
        testGetAllWithPagination(expected, 1, all.size() - 1);
    }

    @Test
    public void testGetAllWithPaginationExcludingLast() {
        List<T> all = sortByHeightDesc(getAllLatest());
        List<T> expected = all.subList(0, all.size() - 1);
        testGetAllWithPagination(expected, 0, all.size() - 2);
    }

    @Test
    public void testGetAllWithPaginationExcludingFirstAndLast() {
        List<T> all = sortByHeightDesc(getAllLatest());
        List<T> expected = all.subList(1, all.size() - 1);
        testGetAllWithPagination(expected, 1, all.size() - 2);
    }

    public void testGetAllWithPagination(List<T> expected, int from, int to) {
        DbUtils.inTransaction(extension, (con) -> {
            List<T> actual = CollectionUtil.toList(table.getAll(from, to));
            assertEquals(expected, actual);
            assertListInCache(expected);
        });

        assertListNotInCache(expected);

        List<T> actual = CollectionUtil.toList(table.getAll(from, to));
        assertEquals(expected, actual);
        assertListNotInCache(expected);

    }

    public void testGetAllWithPaginationForHeight() {
        Map.Entry<DbKey, List<T>> data = getEntryWithListOfSize(getAllLatest(), table.getDbKeyFactory(), 3);
        data.getValue()
    }















    public T getDeletedMultiversionRecord() {
        throw new UnsupportedOperationException("deleted multiversion record is not provided");
    }

    Map<DbKey, List<T>> groupByDbKey() {
        return groupByDbKey(table.getDbKeyFactory());
    }

    public void assertInCache(T value) {
        T cachedValue = getCache(table.getDbKeyFactory().newKey(value));
        assertEquals(value, cachedValue);
    }

    public void assertNotInCache(T value) {
        T cachedValue = getCache(table.getDbKeyFactory().newKey(value));
        assertNotEquals(value, cachedValue);
    }
    public void assertListNotInCache(List<T> values) {
        values.forEach(this::assertNotInCache);
    }
    public void assertListInCache(List<T> values) {
        values.forEach(this::assertInCache);
    }

    public  T getCache(DbKey dbKey) {
        if (!extension.getDatabaseManger().getDataSource().isInTransaction()) {
            return DbUtils.getInTransaction(extension, (con) -> getCacheInTransaction(dbKey));
        } else {
            return getCacheInTransaction(dbKey);
        }
    }

    public T getCacheInTransaction(DbKey dbKey) {
        Map<DbKey, Object> cache = extension.getDatabaseManger().getDataSource().getCache(derivedDbTable.getTableName());
        return (T) cache.get(dbKey);
    }


    public void removeFromCache(T value) {
        DbKey dbKey = table.getDbKeyFactory().newKey(value);
        Map<DbKey, Object> cache = extension.getDatabaseManger().getDataSource().getCache(derivedDbTable.getTableName());
        cache.remove(dbKey);
    }

    public List<T> getAllLatest() {
        return sortByHeightDesc(getAll());
    }
}
