/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.db.model.DerivedIdEntity;
import com.apollocurrency.aplwallet.apl.core.db.table.DerivedDbTableImpl;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.DerivedTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.SQLException;
import java.util.List;
@EnableWeld
public class DerivedDbTableTest {
    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getInMemDbProps(), "db/derived-data.sql", null);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            FullTextConfigImpl.class,
            DerivedDbTablesRegistryImpl.class)
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .build();

    private DerivedDbTable<DerivedIdEntity> derivedDbTable;
    private DerivedTestData data = new DerivedTestData();

    @BeforeEach
    void setUp() {
        derivedDbTable = new DerivedDbTableImpl();
    }


    @Test
    void testGetAll() throws SQLException {
        List<DerivedIdEntity> all = derivedDbTable.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();

        assertEquals(data.ALL, all);
    }

    @Test
    void testTrimForZeroHeight() throws SQLException {
        testTrim(0);
    }

    @Test
    void testTrimForMaxEntityHeight() throws SQLException {
        testTrim(data.ENTITY_6.getHeight());
    }

    void testTrim(int height) throws SQLException {
        DbUtils.inTransaction(extension, (con) -> derivedDbTable.trim(height));

        List<DerivedIdEntity> all = derivedDbTable.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        assertEquals(data.ALL, all);
    }

    @Test
    void testDelete() {
        assertThrows(UnsupportedOperationException.class, () -> derivedDbTable.delete(data.ENTITY_1));
    }

    @Test
    void testTruncate() throws SQLException {
        DbUtils.inTransaction(extension, (con) -> derivedDbTable.truncate());

        List<DerivedIdEntity> all = derivedDbTable.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();

        assertTrue(all.isEmpty(), "Table should not contain any records after 'truncate' operation");
    }

    @Test
    void testInsert() {
        assertThrows(UnsupportedOperationException.class, () -> DbUtils.inTransaction(extension, (con) -> derivedDbTable.insert(data.NEW_ENTITY)));
    }

    @Test
    void testRollbackToZero() throws SQLException {
        testRollback(List.of(), 0);
    }

    @Test
    void testRollbackToLastEntry() throws SQLException {
        testRollback(data.ALL, data.ENTITY_6.getHeight());
    }

    @Test
    void testRollbackToFirstEntry() throws SQLException {
        testRollback(List.of(data.ENTITY_1, data.ENTITY_2), data.ENTITY_1.getHeight());
    }

    private void testRollback(List<DerivedIdEntity> expected, int height) throws SQLException {
        DbUtils.inTransaction(extension, (con) -> derivedDbTable.rollback(height));
        List<DerivedIdEntity> actual = derivedDbTable.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        assertEquals(expected, actual);
    }
}
