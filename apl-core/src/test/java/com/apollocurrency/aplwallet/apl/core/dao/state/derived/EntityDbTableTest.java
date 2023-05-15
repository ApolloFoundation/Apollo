/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.derived;

import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.Filter;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import com.apollocurrency.aplwallet.apl.util.db.FilteringIterator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public abstract class EntityDbTableTest<T extends DerivedEntity> extends BasicDbTableTest<T> {

    EntityDbTable<T> table;
    private DbKey THROWING_DB_KEY = createThrowingKey();
    private Comparator<T> DB_ID_HEIGHT_COMPARATOR = Comparator.comparing(T::getHeight).thenComparing(T::getDbId).reversed();
    private String DB_ID_HEIGHT_SORT = " ORDER BY height DESC, db_id DESC";

    public EntityDbTableTest(Class<T> clazz) {
        super(clazz);
    }

    private DbKey createThrowingKey() {
        DbKey throwingKey = mock(DbKey.class);
        try {
            doThrow(SQLException.class).when(throwingKey).setPK(any(PreparedStatement.class));
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return throwingKey;
    }

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
        T unknownValue = table.get(table.getDbKeyFactory().newKey(valueToInsert()));

        assertNull(unknownValue, "value with unknown db key should be null");
    }

    @Test
    public void testGetFromCache() {
        List<T> all = getAllLatest();
        T expected = all.get(2);
        DbKey dbKey = table.getDbKeyFactory().newKey(expected);
        DbUtils.inTransaction(getDatabaseManager(), (con) -> {
            T actual = table.get(dbKey, true);
            assertEquals(expected, actual);
        });
        T actual = table.get(dbKey, true);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetWithoutCache() {
        List<T> all = getAllLatest();
        T expected = all.get(0);
        DbKey dbKey = table.getDbKeyFactory().newKey(expected);
        DbUtils.inTransaction(getDatabaseManager(), (con) -> {
            T actual = table.get(dbKey, false);
            assertEquals(expected, actual);
        });
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
    }

    @Test
    public void testGetWithSqlException() throws SQLException {

        assertThrows(RuntimeException.class, () -> table.get(THROWING_DB_KEY));
    }

    @Test
    public void testGetByHeight() {

        if (table.isMultiversion()) {
            Map.Entry<DbKey, List<T>> entries = getEntryWithListOfSize(getAll(), table.getDbKeyFactory(), 3, true);
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
            assertEquals(notLatest, actual);

            actual = table.get(entries.getKey(), notLatest.getHeight() - 1);
            assertEquals(first, actual);

            actual = table.get(entries.getKey(), first.getHeight());
            assertEquals(first, actual);

            if (table.supportDelete()) {
                T deleted = getDeletedMultiversionRecord().get(0);
                actual = table.get(table.getDbKeyFactory().newKey(deleted), deleted.getHeight());
                assertNull(actual);
            }


            entries = getEntryWithListOfSize(getAll(), table.getDbKeyFactory(), 1, true);

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
        T unknownEntity = table.get(table.getDbKeyFactory().newKey(valueToInsert()), Integer.MAX_VALUE);
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

    /*
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
     */

    @Test
    public void testGetByDbClauseWithHeightUsingIncorrectDbId() {
        long incorrectDbId = getIncorrectDbId();

        T uknownEntity = table.getBy(new DbClause.LongClause("db_id", DbClause.Op.EQ, incorrectDbId));
        assertNull(uknownEntity, "Entity with unknown db_id should not exist");
    }

    public long getIncorrectDbId() {
        long incorrectDbId;
        if (table.isMultiversion() && table.supportDelete()) {
            incorrectDbId = getDeletedMultiversionRecord().get(0).getDbId();
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
        List<T> expected = getAllLatest().stream().sorted(getDefaultComparator()).collect(Collectors.toList());
        assertEquals(expected, all);
    }


    @Test
    public void testGetManyByEmptyClauseWithOffset() {
        DbIterator<T> manyBy = table.getManyBy(DbClause.EMPTY_CLAUSE, 2, Integer.MAX_VALUE);
        List<T> all = CollectionUtil.toList(manyBy);
        List<T> allExpectedData = getAllLatest();
        List<T> expected = allExpectedData.stream().sorted(getDefaultComparator()).skip(2).collect(Collectors.toList());
        assertIterableEquals(expected, all);
    }

    @Test
    public void testGetManyByEmptyClauseWithLimit() {
        List<T> allExpectedData = getAllLatest();
        List<T> all = CollectionUtil.toList(table.getManyBy(DbClause.EMPTY_CLAUSE, 0, allExpectedData.size() - 2));
        List<T> expected = allExpectedData.stream().sorted(getDefaultComparator()).limit(allExpectedData.size() - 1).collect(Collectors.toList());
        assertEquals(expected, all);
    }

    /**
     * Even though FilteringIterator is a separate class,
     * this test is here so that not to create a DB with a schema one more time.
     */
    @Test
    void shouldTestFilteringIterator() {
        //GIVEN
        final int from = 3;
        final int to = 7;
        final List<T> expected = new ArrayList<>();
        final int maxSize = to - from + 1;

        //WHEN
        try (FilteringIterator<T> iterator = new FilteringIterator<>(
            table.getManyBy(DbClause.EMPTY_CLAUSE, 0, Integer.MAX_VALUE), e -> true, from, to
        )) {
            while (iterator.hasNext()) {
                expected.add(iterator.next());
            }
        }

        final IteratorToStreamConverter<T> converter = new IteratorToStreamConverter<>();
        final ArrayList<T> actual = converter.convert(
            table.getManyBy(DbClause.EMPTY_CLAUSE, 0, Integer.MAX_VALUE)
        ).skip(from).limit(maxSize).collect(Collectors.toCollection(ArrayList::new));

        //THEN
        assertEquals(expected, actual);
    }

    @Test
    public void testGetManyByIncorrectClause() {
        List<T> all = CollectionUtil.toList(table.getManyBy(new DbClause.LongClause("db_id", getIncorrectDbId()), 0, Integer.MAX_VALUE));
        assertEquals(0, all.size());
    }

    @Test
    public void testGetManyByHeightAllClause() {
        List<T> expected = getAllLatest().stream().sorted(getDefaultComparator()).collect(Collectors.toList());
        List<T> all = CollectionUtil.toList(table.getManyBy(new DbClause.IntClause("height", DbClause.Op.GTE, 0), 0, expected.size()));
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyByHeightWithUpperBound() {
        List<T> allExpectedData = getAllLatest();
        List<Integer> heights = getHeights(allExpectedData);
        int height = heights.get(1);
        List<T> expected = allExpectedData.stream().filter(t -> t.getHeight() < height).sorted(getDefaultComparator()).collect(Collectors.toList());
        List<T> all = CollectionUtil.toList(table.getManyBy(new DbClause.IntClause("height", DbClause.Op.GTE, 0).and(new DbClause.IntClause("height", DbClause.Op.LT, height)), 0, expected.size() - 1));
        assertEquals(expected, all);
    }


    @Test
    public void testGetManyByHeightInRange() {
        List<T> allExpectedData = getAllLatest();
        List<Integer> heights = getHeights(allExpectedData);
        int upperHeight = heights.get(1);
        int lowerHeight = heights.get(2);
        List<T> expected = allExpectedData.stream().filter(t -> t.getHeight() < upperHeight && t.getHeight() >= lowerHeight).sorted(getDefaultComparator()).collect(Collectors.toList());
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
        List<T> expected = allExpectedData.stream().filter(t -> t.getHeight() != notExpectedHeight).sorted(getDefaultComparator()).collect(Collectors.toList());
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
        List<T> expected = allExpectedData.stream().filter(t -> t.getHeight() <= upperHeight && t.getHeight() >= lowerHeight).sorted(getDefaultComparator()).collect(Collectors.toList()).subList(0, 1);
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
    public void testGetManyOnConnectionWithoutCache() {
        List<T> allExpectedData = sortByHeightDesc(getAll());
        DbUtils.inTransaction(getDatabaseManager(), (con) -> {
                try {
                    PreparedStatement pstm = con.prepareStatement("select * from " + table.getTableName() + " order by height desc, db_id desc");
                    List<T> all = CollectionUtil.toList(table.getManyBy(con, pstm, false));
                    assertEquals(allExpectedData, all);
                } catch (SQLException e) {
                    throw new RuntimeException(e.toString(), e);
                }
            }
        );
    }

    protected List<T> getExpectedAtHeight(int from, int to, int height, Comparator<T> comp, Filter<T> filter) {
        List<T> latest = getAllLatest();
        List<T> all = getAll();
        Map<DbKey, List<T>> dbKeyListMap = groupByDbKey(all, table.getDbKeyFactory());
        List<T> expected = all.stream().filter(e -> {
            if (e.getHeight() <= height && filter.test(e)) {
                if (latest.contains(e)) {
                    return true;
                }

                List<T> elements = dbKeyListMap.get(table.getDbKeyFactory().newKey(e));
                boolean notDeleted = elements
                    .stream()
                    .anyMatch(el -> el.getHeight() > height);
                boolean lastAtHeight = elements
                    .stream()
                    .noneMatch(el -> el.getHeight() <= height && el.getHeight() > e.getHeight());
                return notDeleted && lastAtHeight;

            } else {
                return false;
            }
        })
            .sorted(comp)
            .skip(from)
            .limit(to - from)
            .collect(Collectors.toList());
        return expected;
    }


    @Test
    public void testGetCount() {
        int size = getAllLatest().size();
        assertEquals(size, table.getCount());
    }

    @Test
    public void testGetCountByEmptyDbClause() {
        int size = getAllLatest().size();
        assertEquals(size, table.getCount(DbClause.EMPTY_CLAUSE));
    }

    @Test
    public void testGetCountByDbClauseWithSecondElementHeight() {
        testGetCountByDbClause(1);
    }

    @Test
    public void testGetCountByDbClauseWithFirstElementHeight() {
        testGetCountByDbClause(0);
    }

    @Test
    public void testGetCountByDbClauseWithLastElement() {
        List<T> all = sortByHeightDesc(getAllLatest());
        testGetCountByDbClause(all.size() - 1);
    }

    public void testGetCountByDbClause(int index) {
        List<T> all = sortByHeightDesc(getAllLatest());
        for (DbClause.Op op : DbClause.Op.values()) {
//           height
            int height = all.get(index).getHeight();
            int size = (int) all.stream().filter(e -> filterByOperation(op, () -> (long) e.getHeight(), (long) height)).count();
            assertEquals(size, table.getCount(new DbClause.IntClause("height", op, height)));
//            db_id
            long dbId = all.get(index).getDbId();
            size = (int) all.stream().filter(e -> filterByOperation(op, e::getDbId, dbId)).count();
            DbClause.LongClause dbIdClause = new DbClause.LongClause("db_id", op, dbId);
            assertEquals(size, table.getCount(dbIdClause));
//            db_id + height
            for (DbClause.Op op2 : DbClause.Op.values()) {
                size = (int) all.stream().filter(e -> filterByOperation(op, e::getDbId, dbId) && filterByOperation(op2, () -> (long) e.getHeight(), (long) height)).count();
                assertEquals(size, table.getCount(dbIdClause.and(new DbClause.IntClause("height", op2, height))));
            }
        }
    }

    @Test
    public void testGetCountByDbClauseWithLastHeight() {
        testGetCountByDbClauseWithHeight(0);
    }

    @Test
    public void testGetCountByDbClauseWithNextHeight() {
        testGetCountByDbClauseWithHeight(1);
    }

    @Test
    public void testGetCountByDbClauseWithMinHeight() {
        List<T> all = getAllLatest();
        testGetCountByDbClauseWithHeight(all.size() - 1);
    }

    @Test
    public void testGetCountByDbClauseWithMaxHeight() {
        assertEquals(getAllLatest().size(), table.getCount(DbClause.EMPTY_CLAUSE, Integer.MAX_VALUE));
    }

    public void testGetCountByDbClauseWithHeight(int index) {
        List<T> allLatest = getAllLatest();
        for (T el : allLatest) {
            testGetCountByDbClauseWithHeight(index, el.getHeight());
        }
    }


    public void testGetCountByDbClauseWithHeight(int index, int height) {
        List<T> allLatest = getAllLatest();
        List<T> expected = getExpectedAtHeight(0, Integer.MAX_VALUE, height, (v1, v2) -> 0, (v) -> true);
        assertEquals(expected.size(), table.getCount(DbClause.EMPTY_CLAUSE, height));
        for (DbClause.Op op : DbClause.Op.values()) {
            long dbId = allLatest.get(index).getDbId();
            int size = getExpectedAtHeight(0, Integer.MAX_VALUE, height, (v1, v2) -> 0, (v) -> filterByOperation(op, v::getDbId, dbId)).size();
            DbClause.LongClause dbIdClause = new DbClause.LongClause("db_id", op, dbId);
            assertEquals(size, table.getCount(dbIdClause, height));
        }
    }

    public boolean filterByOperation(DbClause.Op op, Supplier<Long> supplier, Long number) {
        switch (op) {
            case EQ:
                return supplier.get().equals(number);
            case GT:
                return supplier.get() > number;
            case LT:
                return supplier.get() < number;
            case NE:
                return !supplier.get().equals(number);
            case GTE:
                return supplier.get() >= number;
            case LTE:
                return supplier.get() <= number;
            default:
                throw new IllegalArgumentException("Db operation is not supported");
        }
    }

    @Test
    public void testGetRowCount() {
        List<T> all = getAll();
        assertEquals(all.size(), table.getRowCount());
    }

    @Test
    public void testInsertOutsideTransaction() {
        Assertions.assertThrows(IllegalStateException.class, () -> table.insert(mock(clazz)));
    }

    @Override
    @Test
    public void testInsert() {
        T value = valueToInsert();
        DbUtils.inTransaction(getDatabaseManager(), (con) -> {
            table.insert(value);
            assertEquals(value, table.get(table.getDbKeyFactory().newKey(value)));
        });
    }

    @Test
    public void testInsertAlreadyExist() {
        T value = getAllLatest().get(1);
        DbUtils.inTransaction(getDatabaseManager(), (con) -> {
            value.setHeight(value.getHeight() + 1);

            table.insert(value);

            T t = table.get(table.getDbKeyFactory().newKey(value));
            value.setDbId(sortByHeightDesc(getAll()).get(0).getDbId() + 1);
            assertEquals(t, value);
        });
        T actual = table.get(table.getDbKeyFactory().newKey(value));
        assertEquals(value, actual);
    }


    public List<T> getDeletedMultiversionRecord() {
        throw new UnsupportedOperationException("deleted multiversion record is not provided");
    }

    Map<DbKey, List<T>> groupByDbKey() {
        return groupByDbKey(table.getDbKeyFactory());
    }

    public List<T> getAllLatest() {
        return sortByHeightDesc(getAll());
    }

    public Comparator<T> getDefaultComparator() {
        return DB_ID_HEIGHT_COMPARATOR;
    }

    public abstract T valueToInsert();
}
