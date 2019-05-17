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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// at least 8 data records required to launch this test
// 2 deleted record, 1 latest not updated, 2 - 1 latest 1 not latest, 3 (1 latest, 1 not latest, 1 not latest)
public abstract class VersionedEntityDbTableTest<T extends VersionedDerivedEntity> extends EntityDbTableTest<T> {
    public VersionedEntityDbTableTest(Class<T> clazz) {
        super(clazz);
    }

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        Map<DbKey, List<T>> dbKeyListMap = groupByDbKey();
        Assertions.assertTrue(dbKeyListMap.entrySet().stream().anyMatch(e-> e.getValue().size() == 2 && !e.getValue().get(0).isLatest() && !e.getValue().get(1).isLatest()), "At least two blockchain deleted record should exist");
        Assertions.assertTrue(dbKeyListMap.entrySet().stream().anyMatch(e-> e.getValue().size() == 1 && e.getValue().get(0).isLatest()), "At least one not updated record should exist");
        Assertions.assertTrue(dbKeyListMap.entrySet().stream().anyMatch(e-> e.getValue().size() == 2 && e.getValue().get(0).isLatest() && !e.getValue().get(1).isLatest()), "At least one updated record should exist");
        Assertions.assertTrue(dbKeyListMap.entrySet().stream().anyMatch(e-> e.getValue().size() == 3 && e.getValue().get(0).isLatest() && !e.getValue().get(1).isLatest() && !e.getValue().get(2).isLatest()), "At least one updated twice record should exist");
    }

    @Override
    public List<T> getAllLatest() {
        return sortByHeightDesc(getAll().stream().filter(VersionedDerivedEntity::isLatest).collect(Collectors.toList()));
    }

    @Override
    public List<T> getDeletedMultiversionRecord() {
        return sortByHeightAsc(groupByDbKey().entrySet().stream().filter(e -> e.getValue().size() >= 2 && e.getValue().stream().noneMatch(VersionedDerivedEntity::isLatest)).map(e -> e.getValue().get(0)).collect(Collectors.toList()));
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
            assertEquals(allLatest.stream().sorted(getDefaultComparator()).collect(Collectors.toList()), all);
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
            List<T> expected = allLatest.stream().sorted(getDefaultComparator()).collect(Collectors.toList());
            assertEquals(expected, all);
            assertListInCache(allLatest);
        });
        assertListNotInCache(allLatest);
        assertNotInCache(t);
    }
}
