/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.apollocurrency.aplwallet.apl.core.app.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;
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

public abstract class MultiversionEntityDbTableTest<T extends VersionedDerivedEntity> extends EntityDbTableTest<T> {
    public MultiversionEntityDbTableTest(Class<T> clazz) {
        super(clazz);
    }

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        Map<DbKey, List<T>> dbKeyListMap = groupByDbKey();
        Assertions.assertTrue(dbKeyListMap.entrySet().stream().anyMatch(e-> e.getValue().size() == 1 && !e.getValue().get(0).isLatest()), "At least one blockchain deleted record should exist");
        Assertions.assertTrue(dbKeyListMap.entrySet().stream().anyMatch(e-> e.getValue().size() == 1 && e.getValue().get(0).isLatest()), "At least one not updated record should exist");
        Assertions.assertTrue(dbKeyListMap.entrySet().stream().anyMatch(e-> e.getValue().size() == 2 && e.getValue().get(0).isLatest() && !e.getValue().get(1).isLatest()), "At least one updated record should exist");
        Assertions.assertTrue(dbKeyListMap.entrySet().stream().anyMatch(e-> e.getValue().size() == 3 && e.getValue().get(0).isLatest() && !e.getValue().get(1).isLatest() && !e.getValue().get(2).isLatest()), "At least one updated twice record should exist");
    }

    @Override
    public List<T> getAllLatest() {
        return sortByHeightDesc(getAll().stream().filter(VersionedDerivedEntity::isLatest).collect(Collectors.toList()));
    }

    @Override
    public T getDeletedMultiversionRecord() {
        return groupByDbKey().entrySet().stream().filter(e -> e.getValue().size() == 1 && !e.getValue().get(0).isLatest()).map(e-> e.getValue().get(0)).findAny().get();
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
            assertEquals(allLatest, all);
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
            assertEquals(allLatest, all);
            assertListInCache(allLatest);
        });
        assertListNotInCache(allLatest);
        assertNotInCache(t);
    }

    @Override
    public void testTrim() throws SQLException {

    }

    public void testTrim(int height) throws SQLException {
        List<T> all = getAll();
        Map<DbKey, List<T>> dbKeyListMap = groupByDbKey();
        List<T> trimmed = new ArrayList<>();
        dbKeyListMap.forEach(((key, value) -> {
            List<T> list = value.stream().filter(t -> t.getHeight() < height && !t.isLatest()).collect(Collectors.toList());
            if (list.size() != 0) {
                if (list.stream().noneMatch(VersionedDerivedEntity::isLatest) && value.size() > list.size()) {
                    trimmed.addAll(list);
                } else {
                    Integer maxHeight = list.stream().map(VersionedDerivedEntity::getHeight).max(Comparator.naturalOrder()).get();
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
        DbUtils.inTransaction(extension, (con)-> table.trim(height));
        List<T> values = table.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        assertEquals(expected, values);
    }
}
