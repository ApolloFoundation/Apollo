/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableValuesDbTable;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class MultiversionValuesDbTableTest<T extends VersionedDerivedEntity> extends ValuesDbTableTest<T> {

    public MultiversionValuesDbTableTest(Class<T> clazz) {
        super(clazz);
    }

    VersionedDeletableValuesDbTable<T> table;

    @BeforeEach
    public void setUp() {
        super.setUp();
        table = (VersionedDeletableValuesDbTable<T>) getTable();
    }

    @Override
    protected List<T> getAllLatest() {
        return sortByHeightDesc(getAll().stream().filter(VersionedDerivedEntity::isLatest).collect(Collectors.toList()));
    }

    @Override
    @Test
    public void testDelete() {
        DbUtils.inTransaction(extension, (con) -> {
            List<T> allLatest = getAllLatest();
            Map.Entry<DbKey, List<T>> valuesToDelete = getEntryWithListOfSize(allLatest, table.getDbKeyFactory(), 3);
            boolean deleted = table.delete(valuesToDelete.getKey());
            assertTrue(deleted);
            List<T> values = table.get(valuesToDelete.getKey());
            assertTrue(values.isEmpty());
            List<T> all;
            try {
                all = table.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
            }
            catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
            assertFalse(all.containsAll(valuesToDelete.getValue()));
            List<T> expectedDeleted = valuesToDelete.getValue().stream().peek(v -> v.setLatest(false)).collect(Collectors.toList());
            assertTrue(all.containsAll(expectedDeleted));
            expectedDeleted = valuesToDelete.getValue().stream().peek(v -> v.setHeight(0)).collect(Collectors.toList());
            assertTrue(all.containsAll(expectedDeleted));
        });
    }

}
