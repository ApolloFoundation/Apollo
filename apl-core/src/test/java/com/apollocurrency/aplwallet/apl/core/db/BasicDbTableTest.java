/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.db.model.DerivedIdEntity;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedIdEntity;
import com.apollocurrency.aplwallet.apl.core.db.table.DerivedDbTableImpl;
import com.apollocurrency.aplwallet.apl.core.db.table.VersionedBasicDbTableImpl;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.DerivedTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.SQLException;
import java.util.List;
@EnableWeld
public class BasicDbTableTest {
    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getInMemDbProps(), "db/derived-data.sql", null);
    private VersionedBasicDbTableImpl versionedTable;
    private DerivedDbTableImpl table;
    private DerivedTestData data;

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            FullTextConfigImpl.class,
            DerivedDbTablesRegistryImpl.class)
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .build();

    @BeforeEach
    public void setUp() {
        data = new DerivedTestData();
        versionedTable = new VersionedBasicDbTableImpl();
        table = new DerivedDbTableImpl();
    }

    @Test
    void testMultiversionTrimForMaxHeight() throws SQLException {
        testMultiversionTrim(List.of(data.VERSIONED_ENTITY_1_1, data.VERSIONED_ENTITY_3_1, data.VERSIONED_ENTITY_2_3, data.VERSIONED_ENTITY_3_2), data.VERSIONED_ENTITY_3_2.getHeight());
    }

    @Test
    void testMultiversionTrimForZeroHeight() throws SQLException {
        testMultiversionTrim(data.ALL_VERSIONED, 0);
    }

    @Test
    void testMultiversionTrimForMaxHeightInclusive() throws SQLException {
        int maxHeight = data.VERSIONED_ENTITY_3_2.getHeight() + 1;
        testMultiversionTrim(List.of(data.VERSIONED_ENTITY_1_1, data.VERSIONED_ENTITY_2_3, data.VERSIONED_ENTITY_3_2), maxHeight);
    }

    @Test
    void testTrimForMaxHeightExclusive() throws SQLException {
        int maxHeight = data.VERSIONED_ENTITY_3_2.getHeight() - 1;
        testMultiversionTrim(List.of(data.VERSIONED_ENTITY_1_1, data.VERSIONED_ENTITY_3_1, data.VERSIONED_ENTITY_2_3, data.VERSIONED_ENTITY_3_2), maxHeight);
    }

    @Test
    void testTrimAllDeleted() throws SQLException {
        int height = data.VERSIONED_ENTITY_4_2.getHeight() + 1;
        testMultiversionTrim(List.of(data.VERSIONED_ENTITY_1_1, data.VERSIONED_ENTITY_3_1, data.VERSIONED_ENTITY_2_3, data.VERSIONED_ENTITY_3_2), height);
    }

    @Test
    void testTrimDeletedEqualAtLastDeletedHeight() throws SQLException {
        int height = data.VERSIONED_ENTITY_4_2.getHeight();
        testMultiversionTrim(List.of(data.VERSIONED_ENTITY_1_1, data.VERSIONED_ENTITY_3_1, data.VERSIONED_ENTITY_2_2, data.VERSIONED_ENTITY_4_1,  data.VERSIONED_ENTITY_2_3, data.VERSIONED_ENTITY_4_2, data.VERSIONED_ENTITY_3_2), height);
    }
    @Test
    void testTrimDeletedAtHeightLessThanLastDeletedRecord() throws SQLException {
        int height = data.VERSIONED_ENTITY_4_2.getHeight() - 1;
        testMultiversionTrim(data.ALL_VERSIONED, height);
    }

    @Test
    void testTrimForThreeUpdatedRecords() throws SQLException {
        int height = data.VERSIONED_ENTITY_2_3.getHeight() + 1;

        testMultiversionTrim(List.of(
                data.VERSIONED_ENTITY_1_1,
                data.VERSIONED_ENTITY_3_1,
                data.VERSIONED_ENTITY_2_3,
                data.VERSIONED_ENTITY_3_2), height);
    }

    @Test
    void testTrimNothingForThreeUpdatedRecords() throws SQLException {
        int height = data.VERSIONED_ENTITY_2_2.getHeight();

        testMultiversionTrim(List.of(
                data.VERSIONED_ENTITY_1_1,
                data.VERSIONED_ENTITY_2_1,
                data.VERSIONED_ENTITY_3_1,
                data.VERSIONED_ENTITY_2_2,
                data.VERSIONED_ENTITY_4_1,
                data.VERSIONED_ENTITY_2_3,
                data.VERSIONED_ENTITY_4_2,
                data.VERSIONED_ENTITY_3_2), height);
    }

    @Test
    void testTrimOutsideTransaction() {
        if (versionedTable.isMultiversion()) {
            Assertions.assertThrows(IllegalStateException.class, () -> versionedTable.trim(0));
        }
    }

    @Test
    void testRollbackOutsideTransaction() {
        Assertions.assertThrows(IllegalStateException.class, () -> versionedTable.rollback(0));
    }

    @Test
    void testRollbackDeletedEntries() throws SQLException {
        int height = data.VERSIONED_ENTITY_4_1.getHeight() - 1;
        data.VERSIONED_ENTITY_2_1.setLatest(true);
        data.VERSIONED_ENTITY_3_1.setLatest(true);

        testMultiversionRollback(List.of(data.VERSIONED_ENTITY_1_1, data.VERSIONED_ENTITY_2_1, data.VERSIONED_ENTITY_3_1), height);
    }

    @Test
    void testRollbackForThreeUpdatedRecords() throws SQLException {
        int height = data.VERSIONED_ENTITY_2_3.getHeight() - 1;
        data.VERSIONED_ENTITY_3_1.setLatest(true);
        data.VERSIONED_ENTITY_2_2.setLatest(true);
        data.VERSIONED_ENTITY_4_1.setLatest(true);
        testMultiversionRollback(List.of(data.VERSIONED_ENTITY_1_1, data.VERSIONED_ENTITY_2_1, data.VERSIONED_ENTITY_3_1, data.VERSIONED_ENTITY_2_2, data.VERSIONED_ENTITY_4_1), height);
    }

    @Test
    void testRollbackNothingForThreeUpdatedRecords() throws SQLException {
        int height = data.VERSIONED_ENTITY_2_3.getHeight();
        data.VERSIONED_ENTITY_3_1.setLatest(true);
        testMultiversionRollback(List.of(
                data.VERSIONED_ENTITY_1_1,
                data.VERSIONED_ENTITY_2_1,
                data.VERSIONED_ENTITY_3_1,
                data.VERSIONED_ENTITY_2_2,
                data.VERSIONED_ENTITY_4_1,
                data.VERSIONED_ENTITY_2_3,
                data.VERSIONED_ENTITY_4_2), height);
    }

    @Test
    void testRollbackEntirelyForTwoRecords() throws SQLException {
        int height = data.VERSIONED_ENTITY_3_1.getHeight() - 1;
        testMultiversionRollback(List.of(data.VERSIONED_ENTITY_1_1), height);
    }

    @Test
    void testRollbackNothing() throws SQLException {
        testMultiversionRollback(data.ALL_VERSIONED, data.VERSIONED_ENTITY_3_2.getHeight());
    }

    @Test
    void testRollbackForNotMultiversionTable() throws SQLException {
        DbUtils.inTransaction(extension, (con) -> table.rollback(data.ENTITY_3.getHeight()));

        List<DerivedIdEntity> values = table.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        assertEquals(List.of(data.ENTITY_1, data.ENTITY_2, data.ENTITY_3), values);
    }

    @Test
    void testTrimForNotMultiversionTable() throws SQLException {
        DbUtils.inTransaction(extension, (con)-> table.trim(Integer.MAX_VALUE)); //should trim all records but trim nothing due to parent implementation, which do nothing

        List<DerivedIdEntity> values = table.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        assertEquals(data.ALL, values);
    }

    private void testMultiversionRollback(List<VersionedDerivedIdEntity> expected, int height) throws SQLException {
        DbUtils.inTransaction(extension, (con)-> versionedTable.rollback(height));
        List<VersionedDerivedIdEntity> values = versionedTable.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        assertEquals(expected, values);
    }

    private void testMultiversionTrim(List<VersionedDerivedIdEntity> expected, int height) throws SQLException {
        DbUtils.inTransaction(extension, (con)-> versionedTable.trim(height));
        List<VersionedDerivedIdEntity> values = versionedTable.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        assertEquals(expected, values);
    }
}
