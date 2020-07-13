/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.ValuesDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class ValuesDbTableTest<T extends DerivedEntity> extends BasicDbTableTest<T> {
    ValuesDbTable<T> table;
    private DbKey INCORRECT_DB_KEY = new DbKey() {
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

    public ValuesDbTableTest(Class<T> clazz) {
        super(clazz);

    }

    public ValuesDbTable<T> getTable() {
        return (ValuesDbTable<T>) getDerivedDbTable();
    }

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        table = getTable();
        assertNotNull(getEntryWithListOfSize(getAllLatest(), table.getDbKeyFactory(), 2));
        assertNotNull(getEntryWithListOfSize(getAllLatest(), table.getDbKeyFactory(), 3));
    }

    @Test
    public void testGetByDbKey() {
        Map.Entry<DbKey, List<T>> entry = getEntryWithListOfSize(getAllLatest(), table.getDbKeyFactory(), 3);
        List<T> values = sortByHeightAsc(entry.getValue());
        DbKey dbKey = entry.getKey();
        List<T> result = table.get(dbKey);
        assertEquals(values, result);
    }

    @Test
    public void testGetByUnknownDbKey() {
        List<T> actual = table.get(INCORRECT_DB_KEY);
        assertTrue(actual.isEmpty(), "No records should be found at dbkey -1");

    }

    @Override
    @Test
    public void testInsert() {
        List<T> toInsert = dataToInsert();
        DbUtils.inTransaction(extension, (con) -> {
            table.insert(toInsert);
        });
        //check db
        List<T> retrievedData = table.get(table.getDbKeyFactory().newKey(toInsert.get(0)));
        assertEquals(toInsert, retrievedData);
    }

    @Test
    public void testInsertNotInTransaction() {
        assertThrows(IllegalStateException.class, () -> table.insert(Collections.emptyList()));
    }

    @Test
    public void testInsertWithDifferentDbKeys() {
        List<T> dataToInsert = dataToInsert();
        T t = dataToInsert.get(0);
        t.setDbKey(INCORRECT_DB_KEY);
        assertThrows(IllegalArgumentException.class, () -> DbUtils.inTransaction(extension, (con) -> table.insert(dataToInsert)));
    }

    protected abstract List<T> dataToInsert();

    protected List<T> getAllLatest() {
        return getAll();
    }
}
