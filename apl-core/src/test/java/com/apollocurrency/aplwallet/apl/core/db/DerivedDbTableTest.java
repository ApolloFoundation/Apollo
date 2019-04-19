/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.SQLException;
import java.util.List;

public abstract class DerivedDbTableTest<T> {
    @RegisterExtension
    DbExtension extension = new DbExtension();

    private DerivedDbTable<T> derivedDbTable;
    private Class<T> clazz;

    public DerivedDbTableTest(DerivedDbTable<T> derivedDbTable, Class<T> clazz) {
        this.derivedDbTable = derivedDbTable;
        this.clazz = clazz;
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
    public void testTrimNothing() throws SQLException {
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
    public void testRollback() throws SQLException {
        DbUtils.inTransaction(extension, (con) -> derivedDbTable.rollback(-1));
        List<T> all = derivedDbTable.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        assertTrue(all.isEmpty(), "Derived table " + derivedDbTable.toString() + " should not have any entries after rollback to -1 height");
    }


    protected abstract List<T> getAllExpectedData();
}
