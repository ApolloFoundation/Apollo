/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

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
        List<T> all = getAllExpectedData();
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
        List<T> all = getAllExpectedData();
        T expected = all.get(2);
        DbKey dbKey = table.getDbKeyFactory().newKey(expected);
        DbUtils.inTransaction(extension, (con)-> {
            T actual = table.get(dbKey, true);
            assertEquals(expected, actual);
            assertInCache(table.getDbKeyFactory(), List.of(expected));
        });
        assertNotInCache(table.getDbKeyFactory(), List.of(expected));
        T actual = table.get(dbKey, true);
        assertEquals(expected, actual);
    }

}
