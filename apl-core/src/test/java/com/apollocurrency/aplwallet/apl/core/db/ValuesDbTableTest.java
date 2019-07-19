/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apollocurrency.aplwallet.apl.core.db.derived.ValuesDbTable;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedChildDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.db.table.VersionedValuesDbTableImpl;
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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
@EnableWeld
public class ValuesDbTableTest {
    private DbKey INCORRECT_DB_KEY = new DbKey() {
        @Override
        public int compareTo(Object o) {
            return 0;
        }

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

    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getInMemDbProps(), "db/derived-data.sql", null);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            FullTextConfigImpl.class,
            DerivedDbTablesRegistryImpl.class)
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .build();

    private CacheUtils<VersionedChildDerivedEntity> cacheUtils;
    private DerivedTestData data;

    @BeforeEach
    void setUp() {
        data = new DerivedTestData();
        versionedTable = new VersionedValuesDbTableImpl();
        cacheUtils = new CacheUtils<>(versionedTable, extension);
    }

    ValuesDbTable<VersionedChildDerivedEntity> versionedTable;

    @Test
    void testGetByDbKey() {
        List<VersionedChildDerivedEntity> result = versionedTable.get(versionedTable.getDbKeyFactory().newKey(data.VCE_2_1_3));
        assertEquals(List.of(data.VCE_2_1_3, data.VCE_2_2_3, data.VCE_2_3_3), result);
    }

    @Test
    void testGetByUnknownDbKey() {
        List<VersionedChildDerivedEntity> actual = versionedTable.get(INCORRECT_DB_KEY);
        assertTrue(actual.isEmpty(), "No records should be found at dbkey -1");

    }

    @Test
    void testInsert() {
        List<VersionedChildDerivedEntity> toInsert = List.of(data.NEW_VCE_1, data.NEW_VCE_2);
        DbUtils.inTransaction(extension, (con) -> {
            versionedTable.insert(toInsert);
            //check cache in transaction
            cacheUtils.assertInCache(toInsert);
        });
        //check db
        cacheUtils.assertNotInCache(toInsert);
        List<VersionedChildDerivedEntity> retrievedData = versionedTable.get(versionedTable.getDbKeyFactory().newKey(toInsert.get(0)));
        assertEquals(toInsert, retrievedData);
    }

    @Test
    void testGetInCached() {
        List<VersionedChildDerivedEntity> values = List.of(data.VCE_1_1_2, data.VCE_1_2_2);
        DbKey dbKey = versionedTable.getDbKeyFactory().newKey(data.VCE_1_2_2);
        DbUtils.inTransaction(extension, con -> {
            List<VersionedChildDerivedEntity> actual = versionedTable.get(dbKey);
            cacheUtils.assertInCache(values);
            assertEquals(values, actual);
        });
        cacheUtils.assertNotInCache(values);
    }

    @Test
    void testGetFromDeletedCache() {
        List<VersionedChildDerivedEntity> values = List.of(data.VCE_1_1_2, data.VCE_1_2_2);
        DbKey dbKey = versionedTable.getDbKeyFactory().newKey(data.VCE_1_2_2);
        DbUtils.inTransaction(extension, con -> {
            List<VersionedChildDerivedEntity> actual = versionedTable.get(dbKey);
            cacheUtils.assertInCache(values);
            assertEquals(values, actual);
            cacheUtils.removeFromCache(values);
            cacheUtils.assertNotInCache(values);
        });
        cacheUtils.assertNotInCache(values);
    }


    @Test
    void testInsertNotInTransaction() {
        assertThrows(IllegalStateException.class, () -> versionedTable.insert(Collections.emptyList()));
    }

    @Test
    void testInsertWithDifferentDbKeys() {
        List<VersionedChildDerivedEntity> dataToInsert = List.of(data.NEW_VCE_1, data.NEW_VCE_2);
        data.NEW_VCE_1.setDbKey(INCORRECT_DB_KEY);
        assertThrows(IllegalArgumentException.class, () -> DbUtils.inTransaction(extension, (con) -> versionedTable.insert(dataToInsert)));
    }

}
