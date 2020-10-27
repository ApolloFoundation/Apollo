/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedTableData;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;


@Slf4j
public abstract class DerivedDbTableTest<T extends DerivedEntity> extends DbContainerBaseTest {

    @RegisterExtension
    DbExtension extension = new DbExtension(mariaDBContainer);

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
        return extension.getDatabaseManager();
    }

    @Test
    public void testGetAll() throws SQLException {
        DerivedTableData<T> allByDbId = derivedDbTable.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE);
        List<T> all = allByDbId.getValues();

        List<T> expected = sortByHeightAsc(getAll());
        assertIterableEquals(expected, all);
    }

    @Test
    public void testTrimForZeroHeight() throws SQLException {
        testTrim(0, Integer.MAX_VALUE);
    }

    @Test
    public void testTrimForMaxHeight() throws SQLException {
        testTrim(sortByHeightDesc(getAll()).get(0).getHeight(), Integer.MAX_VALUE);
    }

    public void testTrim(int height, int blockchainHeight) throws SQLException {
        DbUtils.inTransaction(extension, (con) -> derivedDbTable.trim(height, true));

        List<T> expected = getAll();
        List<T> all = derivedDbTable.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        assertIterableEquals(expected, all);
    }

    @Test
    public void testDelete() throws SQLException {
        assertThrows(UnsupportedOperationException.class, () -> derivedDbTable.deleteAtHeight(mock(clazz), 1));
    }

    @Test
    public void testTruncate() throws SQLException {
        DbUtils.inTransaction(extension, (con) -> derivedDbTable.truncate());

        List<T> all = derivedDbTable.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();

        assertTrue(all.isEmpty(), "Table should not contain any records after 'truncate' operation");
    }

    @Test
    public void testInsert() {
        assertThrows(UnsupportedOperationException.class, () -> DbUtils.inTransaction(extension, (con) -> derivedDbTable.insert(mock(clazz))));
    }

    @Test
    public void testRollbackToNegativeHeight() throws SQLException {
        testRollback(0);
    }

    @Test
    public void testRollbackToLastEntry() throws SQLException {
        List<Integer> heights = getHeights();
        testRollback(heights.get(0));
    }

    @Test
    public void testRollbackToFirstEntry() throws SQLException {
        List<Integer> heights = getHeights();
        Integer rollbackHeight = heights.get(heights.size() - 1);
        testRollback(rollbackHeight);
    }

    public void testRollback(int height) throws SQLException {
        List<T> expected = sublistByHeight(getAll(), height);
        DbUtils.inTransaction(extension, (con) -> derivedDbTable.rollback(height));
        List<T> actual = derivedDbTable.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        assertIterableEquals(expected, actual);
    }


    public List<T> sublistByHeightDesc(List<T> list, int maxHeight) {
        return list
            .stream()
            .filter(d -> d.getHeight() <= maxHeight)
            .sorted(Comparator.comparing(DerivedEntity::getHeight).thenComparing(DerivedEntity::getDbId).reversed())
            .collect(toList());
    }


    public List<T> sublistByHeight(List<T> list, int maxHeight) {
        return list
            .stream()
            .filter(d -> d.getHeight() <= maxHeight)
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
                    l -> l.stream().sorted(
                        Comparator.comparing(DerivedEntity::getHeight)
                            .thenComparing(DerivedEntity::getDbId)
                            .reversed()).collect(toList()))));
    }

    public Map<DbKey, List<T>> groupByDbKey(KeyFactory<T> keyFactory) {
        return groupByDbKey(getAll(), keyFactory);
    }

    protected Map.Entry<DbKey, List<T>> getEntryWithListOfSize(List<T> data, KeyFactory<T> keyFactory, int size) {
        return groupByDbKey(data, keyFactory)
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().size() == size)
            .findFirst()
            .get();
    }

    protected Map.Entry<DbKey, List<T>> getEntryWithListOfSize(List<T> data, KeyFactory<T> keyFactory, int size, boolean skipDeleted) {
        return groupByDbKey(data, keyFactory)
            .entrySet()
            .stream()
            .filter(entry -> !skipDeleted || entry.getValue().stream().anyMatch(e -> ((VersionedDerivedEntity) e).isLatest()))
            .filter(entry -> getHeights(entry.getValue()).size() == size)
            .findFirst()
            .get();
    }


    protected abstract List<T> getAll();
}
