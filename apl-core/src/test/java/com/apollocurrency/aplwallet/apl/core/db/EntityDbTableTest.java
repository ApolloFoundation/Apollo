/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.db.model.DerivedIdEntity;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedIdEntity;
import com.apollocurrency.aplwallet.apl.core.db.table.EntityDbTableImpl;
import com.apollocurrency.aplwallet.apl.core.db.table.VersionedEntityDbTableImpl;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.DerivedTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
@EnableWeld
public class EntityDbTableTest {
    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getInMemDbProps(), "db/derived-data.sql", null);
    private Blockchain blockchain = mock(Blockchain.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            FullTextConfigImpl.class,
            DerivedDbTablesRegistryImpl.class)
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
            .addBeans(MockBean.of(blockchainProcessor, BlockchainProcessor.class, BlockchainProcessorImpl.class))
            .build();


    private DbKey THROWING_DB_KEY = createThrowingKey();
    private String DB_ID_HEIGHT_SORT = " ORDER BY height DESC, db_id DESC";

    private DbKey createThrowingKey() {
        DbKey throwingKey = mock(DbKey.class);
        try {
            doThrow(SQLException.class).when(throwingKey).setPK(any(PreparedStatement.class));
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return throwingKey;
    }


    private EntityDbTable<VersionedDerivedIdEntity> versionedTable;
    private EntityDbTable<DerivedIdEntity> ordinaryTable;
    private CacheUtils<DerivedIdEntity> derivedCacheUtils;
    private CacheUtils<VersionedDerivedIdEntity> versionedCacheUtils;
    private DerivedTestData data;

    @BeforeEach
    public void setUp() {
        data = new DerivedTestData();
        versionedTable = new VersionedEntityDbTableImpl();
        ordinaryTable = new EntityDbTableImpl();
        derivedCacheUtils = new CacheUtils<>(ordinaryTable, extension);
        versionedCacheUtils = new CacheUtils<>(versionedTable, extension);
    }

    private void mockBlockchain(int height) {
        doReturn(height).when(blockchain).getHeight();
    }

    @Test
    public void testGetByDbKey() {
        DbKey dbKey = versionedTable.getDbKeyFactory().newKey(data.VERSIONED_ENTITY_2_3);
        VersionedDerivedIdEntity actual = versionedTable.get(dbKey);
        assertEquals(data.VERSIONED_ENTITY_2_3, actual);
    }

    @Test
    public void testByUnknownDbKey() {
        VersionedDerivedIdEntity unknownValue = versionedTable.get(new LongKey(Long.MAX_VALUE));

        assertNull(unknownValue, "value with unknown db key should be null");
    }

    @Test
    public void testGetFromCache() {
        VersionedDerivedIdEntity expected = data.VERSIONED_ENTITY_3_2;
        DbKey dbKey = versionedTable.getDbKeyFactory().newKey(expected);
        DbUtils.inTransaction(extension, (con) -> {
            VersionedDerivedIdEntity actual = versionedTable.get(dbKey, true);
            assertEquals(expected, actual);
            versionedCacheUtils.assertInCache(expected);
        });
        versionedCacheUtils.assertNotInCache(expected);
        VersionedDerivedIdEntity actual = versionedTable.get(dbKey, true);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetWithoutCache() {
        VersionedDerivedIdEntity expected = data.VERSIONED_ENTITY_1_1;
        DbKey dbKey = versionedTable.getDbKeyFactory().newKey(expected);
        DbUtils.inTransaction(extension, (con) -> {
            VersionedDerivedIdEntity actual = versionedTable.get(dbKey, false);
            assertEquals(expected, actual);
            versionedCacheUtils.assertNotInCache(expected);
        });
        versionedCacheUtils.assertNotInCache(expected);
        VersionedDerivedIdEntity actual = versionedTable.get(dbKey, false);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetFromCacheWithoutTransaction() {
        VersionedDerivedIdEntity expected = data.VERSIONED_ENTITY_2_3;
        DbKey dbKey = versionedTable.getDbKeyFactory().newKey(expected);
        VersionedDerivedIdEntity actual = versionedTable.get(dbKey, true);
        assertEquals(expected, actual);
        versionedCacheUtils.assertNotInCache(expected);
    }

    @Test
    public void testGetWithSqlException() {

        assertThrows(RuntimeException.class, () -> versionedTable.get(THROWING_DB_KEY));
    }

    @Test
    public void testMultiversionGetByHeight() {
        mockBlockchain(Integer.MAX_VALUE);
        VersionedDerivedIdEntity latest  = data.VERSIONED_ENTITY_2_3;
        VersionedDerivedIdEntity  notLatest  = data.VERSIONED_ENTITY_2_2;
        VersionedDerivedIdEntity  first  = data.VERSIONED_ENTITY_2_1;

        DbKey dbKey = versionedTable.getDbKeyFactory().newKey(latest);

        VersionedDerivedIdEntity  actual = versionedTable.get(dbKey, latest.getHeight() + 1);
        assertEquals(latest, actual);

        actual = versionedTable.get(dbKey, latest.getHeight());
        assertEquals(latest, actual);

        actual = versionedTable.get(dbKey, latest.getHeight() - 1);
        assertEquals(notLatest, actual);

        actual = versionedTable.get(dbKey, notLatest.getHeight());
        assertEquals(notLatest, actual);

        actual = versionedTable.get(dbKey, notLatest.getHeight() - 1);
        assertEquals(first, actual);

        actual = versionedTable.get(dbKey, first.getHeight());
        assertEquals(first, actual);

        VersionedDerivedIdEntity  deleted = data.VERSIONED_ENTITY_4_2;

        actual = versionedTable.get(versionedTable.getDbKeyFactory().newKey(deleted), deleted.getHeight());

        assertNull(actual);

        VersionedDerivedIdEntity expected = data.VERSIONED_ENTITY_1_1;
        dbKey = versionedTable.getDbKeyFactory().newKey(expected);

        actual = versionedTable.get(dbKey, expected.getHeight());
        assertEquals(expected, actual);

        actual = versionedTable.get(dbKey, expected.getHeight() + 1);
        assertEquals(expected, actual);
    }
    @Test
    public void testGetOrdinaryByHeight() {
        DerivedIdEntity  expected = data.ENTITY_3;
        DerivedIdEntity  actual = ordinaryTable.get(ordinaryTable.getDbKeyFactory().newKey(expected), expected.getHeight());
        assertEquals(expected, actual);

        actual = ordinaryTable.get(ordinaryTable.getDbKeyFactory().newKey(expected), expected.getHeight() + 1);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetByHeightForUnknownDbKey() {
        VersionedDerivedIdEntity  unknownEntity = versionedTable.get(new LongKey(Long.MAX_VALUE), Integer.MAX_VALUE);
        assertNull(unknownEntity, "Entity with unknown db key should noVersionedDerivedIdEntity  exist");
    }

    @Test
    public void testGetByHeightWithException() {
        assertThrows(RuntimeException.class, () -> versionedTable.get(THROWING_DB_KEY, Integer.MAX_VALUE));
    }

    @Test
    public void testGetMultiversionByDbClause() {
        VersionedDerivedIdEntity expected = data.VERSIONED_ENTITY_3_2;

        VersionedDerivedIdEntity  actual = versionedTable.getBy(new DbClause.IntClause("height", DbClause.Op.EQ, expected.getHeight()).and(new DbClause.LongClause("db_id", DbClause.Op.EQ, expected.getDbId())));
        assertEquals(expected, actual);
    }
    @Test
    public void testGetOrdinaryByDbClause() {
        DerivedIdEntity  expected = data.ENTITY_2;
        DerivedIdEntity  actual = ordinaryTable.getBy(new DbClause.IntClause("height", DbClause.Op.EQ, expected.getHeight()).and(new DbClause.LongClause("db_id", DbClause.Op.EQ, expected.getDbId())));
        assertEquals(expected, actual);
    }

    @Test
    public void testGetByDbClauseUsingIncorrectDbId() {
        VersionedDerivedIdEntity  uknownEntity = versionedTable.getBy(new DbClause.LongClause("db_id", DbClause.Op.EQ, Long.MAX_VALUE));
        assertNull(uknownEntity, "Entity with unknown db_id should not exist");
    }

    @Test
    public void testGetMultiversionByDbClauseWithHeight() {
        VersionedDerivedIdEntity  expected = data.VERSIONED_ENTITY_1_1;

        VersionedDerivedIdEntity  actual = versionedTable.getBy((new DbClause.LongClause("db_id", DbClause.Op.EQ, expected.getDbId())), expected.getHeight());
        assertEquals(expected, actual);
    }

    @Test
    public void testGetOrdinaryByDbClauseWithHeight() {
        DerivedIdEntity  expected = data.ENTITY_4;

        DerivedIdEntity  actual = ordinaryTable.getBy((new DbClause.LongClause("db_id", DbClause.Op.EQ, expected.getDbId())), expected.getHeight());
        assertEquals(expected, actual);
    }


    @Test
    public void testGetByDbClauseWithHeightUsingIncorrectDbId() {
        VersionedDerivedIdEntity  uknownEntity = versionedTable.getBy(new DbClause.LongClause("db_id", DbClause.Op.EQ, Long.MAX_VALUE));
        assertNull(uknownEntity, "Entity with unknown db_id should not exist");
    }


//    public List<VersionedDerivedIdEntity> getAllLatest() {
//        return sortByHeightDesc(groupByDbKey(versionedTable.getDbKeyFactory()).values().stream().map(l -> sortByHeightDesc(l).get(0)).collect(Collectors.toList()));
//    }

    @Test
    public void testGetManyByEmptyClause() {
        List<VersionedDerivedIdEntity> all = CollectionUtil.toList(versionedTable.getManyBy(DbClause.EMPTY_CLAUSE, 0, Integer.MAX_VALUE));
        List<VersionedDerivedIdEntity> expected = List.of(data.VERSIONED_ENTITY_1_1, data.VERSIONED_ENTITY_2_3, data.VERSIONED_ENTITY_3_2);
        assertEquals(expected, all);
    }


    @Test
    public void testGetManyByEmptyClauseWithOffset() {
        List<VersionedDerivedIdEntity> all = CollectionUtil.toList(versionedTable.getManyBy(DbClause.EMPTY_CLAUSE, 2, Integer.MAX_VALUE));
        List<VersionedDerivedIdEntity> expected = List.of(data.VERSIONED_ENTITY_3_2);
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyByEmptyClauseWithLimit() {
        List<VersionedDerivedIdEntity> all = CollectionUtil.toList(versionedTable.getManyBy(DbClause.EMPTY_CLAUSE, 0, 1));
        List<VersionedDerivedIdEntity> expected = List.of(data.VERSIONED_ENTITY_1_1, data.VERSIONED_ENTITY_2_3);
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyByEmptyClauseWithLimitAndOffset() {
        List<VersionedDerivedIdEntity> all = CollectionUtil.toList(versionedTable.getManyBy(DbClause.EMPTY_CLAUSE, 1, 1));
        List<VersionedDerivedIdEntity> expected = List.of(data.VERSIONED_ENTITY_2_3);
        assertEquals(expected, all);
    }

    @Test
    public void testGetManyByIncorrectClause() {
        List<VersionedDerivedIdEntity> all = CollectionUtil.toList(versionedTable.getManyBy(new DbClause.LongClause("db_id", Long.MAX_VALUE), 0, Integer.MAX_VALUE));
        assertEquals(0, all.size());
    }

    @Test
    public void testGetManyByHeightAllClause() {
        List<VersionedDerivedIdEntity> all = CollectionUtil.toList(versionedTable.getManyBy(new DbClause.IntClause("height", DbClause.Op.GTE, 0), 0, 3));
        assertEquals(List.of(data.VERSIONED_ENTITY_1_1, data.VERSIONED_ENTITY_2_3, data.VERSIONED_ENTITY_3_2), all);
    }

    @Test
    public void testGetManyByHeightWithUpperBound() {

        int height = data.VERSIONED_ENTITY_2_3.getHeight();
        List<VersionedDerivedIdEntity> all = CollectionUtil.toList(versionedTable.getManyBy(new DbClause.IntClause("height", DbClause.Op.GTE, 0).and(new DbClause.IntClause("height", DbClause.Op.LT, height)), 0, 1));
        assertEquals(List.of(data.VERSIONED_ENTITY_1_1), all);
    }


    @Test
    public void testGetManyByHeightInRange() {
        int upperHeight = data.VERSIONED_ENTITY_4_2.getHeight();
        int lowerHeight = data.VERSIONED_ENTITY_1_1.getHeight();
        List<VersionedDerivedIdEntity> all = CollectionUtil.toList(versionedTable.getManyBy(new DbClause.IntClause("height", DbClause.Op.GTE, lowerHeight).and(new DbClause.IntClause("height", DbClause.Op.LT, upperHeight)), 0, 1));
        assertEquals(List.of(data.VERSIONED_ENTITY_1_1), all);
    }

    @Test
    public void testGetManyByHeightInRangeExclusive() {
        int upperHeight = data.VERSIONED_ENTITY_3_2.getHeight();
        int lowerHeight = data.VERSIONED_ENTITY_1_1.getHeight();
        List<VersionedDerivedIdEntity> all = CollectionUtil.toList(versionedTable.getManyBy(new DbClause.IntClause("height", DbClause.Op.GT, lowerHeight).and(new DbClause.IntClause("height", DbClause.Op.LT, upperHeight)), 0, 2));
        assertEquals(List.of(data.VERSIONED_ENTITY_2_3), all);
    }

    @Test
    public void testGetManyByHeightWithNotDefinedHeight() {
        int notExpectedHeight = data.VERSIONED_ENTITY_2_3.getHeight();
        List<VersionedDerivedIdEntity> all = CollectionUtil.toList(versionedTable.getManyBy(new DbClause.IntClause("height", DbClause.Op.NE, notExpectedHeight), 0, Integer.MAX_VALUE));
        assertEquals(List.of(data.VERSIONED_ENTITY_1_1, data.VERSIONED_ENTITY_3_2), all);
    }

    @Test
    public void testGetManyByHeightWithDefinedHeight() {
        int expectedHeight = data.VERSIONED_ENTITY_2_3.getHeight();
        List<VersionedDerivedIdEntity> all = CollectionUtil.toList(versionedTable.getManyBy(new DbClause.IntClause("height", DbClause.Op.EQ, expectedHeight), 0, Integer.MAX_VALUE));
        assertEquals(List.of(data.VERSIONED_ENTITY_2_3), all);
    }

    @Test
    public void testGetManyByHeightWithInRangeInclusive() {
        int lowerHeight = data.VERSIONED_ENTITY_1_1.getHeight();
        int upperHeight = data.VERSIONED_ENTITY_3_2.getHeight();
        List<VersionedDerivedIdEntity> all = CollectionUtil.toList(versionedTable.getManyBy(new DbClause.IntClause("height", DbClause.Op.GTE, lowerHeight).and(new DbClause.IntClause("height", DbClause.Op.LTE, upperHeight)), 0, Integer.MAX_VALUE));
        assertEquals(List.of(data.VERSIONED_ENTITY_1_1, data.VERSIONED_ENTITY_2_3, data.VERSIONED_ENTITY_3_2), all);
    }

    @Test
    public void testGetManyByHeightInRangeWithPagination() {
        int upperHeight = data.VERSIONED_ENTITY_3_2.getHeight();
        int lowerHeight = data.VERSIONED_ENTITY_1_1.getHeight();
        List<VersionedDerivedIdEntity> all = CollectionUtil.toList(versionedTable.getManyBy(new DbClause.IntClause("height", DbClause.Op.GTE, lowerHeight).and(new DbClause.IntClause("height", DbClause.Op.LTE, upperHeight)), 0, 0));
        assertEquals(List.of(data.VERSIONED_ENTITY_1_1), all);
    }

    @Test
    public void testGetManyByClauseWithCustomSort() {
        List<VersionedDerivedIdEntity> all = CollectionUtil.toList(versionedTable.getManyBy(new DbClause.LongClause("db_id", DbClause.Op.GT, 0).and(new DbClause.LongClause("db_id", DbClause.Op.LT, Long.MAX_VALUE)), 0, Integer.MAX_VALUE, " ORDER BY db_id desc"));
        assertEquals(List.of(data.VERSIONED_ENTITY_3_2, data.VERSIONED_ENTITY_2_3, data.VERSIONED_ENTITY_1_1), all);
    }


    @Test
    public void testGetManyOnConnectionWithCache() {
        // duplicates will exist in cache because cache populates row by row using previous records with same dbkey
        List<VersionedDerivedIdEntity> expected = List.of(data.VERSIONED_ENTITY_3_2, data.VERSIONED_ENTITY_4_2, data.VERSIONED_ENTITY_2_3, data.VERSIONED_ENTITY_4_2, data.VERSIONED_ENTITY_2_3, data.VERSIONED_ENTITY_3_2, data.VERSIONED_ENTITY_2_3, data.VERSIONED_ENTITY_1_1);

        DbUtils.inTransaction(extension, (con) -> {
                    try {
                        PreparedStatement pstm = con.prepareStatement("select * from " + versionedTable.getTableName() + " ORDER BY height desc, DB_ID desc ");
                        List<VersionedDerivedIdEntity> all = CollectionUtil.toList(versionedTable.getManyBy(con, pstm, true));

                        assertEquals(expected, all);
                        versionedCacheUtils.assertListInCache(expected);
                    }
                    catch (SQLException e) {
                        throw new RuntimeException(e.toString(), e);
                    }
                }
        );
        versionedCacheUtils.assertListNotInCache(expected);
    }



    @Test
    public void testGetManyOnConnectionWithoutCache() {
        List<VersionedDerivedIdEntity> allExpected = new ArrayList<>(data.ALL_VERSIONED);
        Collections.reverse(allExpected);
        DbUtils.inTransaction(extension, (con) -> {
                    try {
                        PreparedStatement pstm = con.prepareStatement("select * from " + versionedTable.getTableName() + " order by height desc, db_id desc");
                        List<VersionedDerivedIdEntity> all = CollectionUtil.toList(versionedTable.getManyBy(con, pstm, false));
                        assertEquals(allExpected, all);
                        versionedCacheUtils.assertListNotInCache(allExpected);
                    }
                    catch (SQLException e) {
                        throw new RuntimeException(e.toString(), e);
                    }
                }
        );
        versionedCacheUtils.assertListNotInCache(allExpected);
    }

    @Test
    public void testGetAllWithMaxPagination() {
        testGetAllWithPaginationHeightSorted(0, Integer.MAX_VALUE, List.of(data.VERSIONED_ENTITY_3_2, data.VERSIONED_ENTITY_2_3, data.VERSIONED_ENTITY_1_1));
        testGetAllWithPaginationCustomSorted(0, Integer.MAX_VALUE, List.of(data.VERSIONED_ENTITY_1_1, data.VERSIONED_ENTITY_2_3, data.VERSIONED_ENTITY_3_2));
    }

    @Test
    public void testGetAllWithDataSizePagination() {
        testGetAllWithPaginationHeightSorted(0, 3, List.of(data.VERSIONED_ENTITY_3_2, data.VERSIONED_ENTITY_2_3, data.VERSIONED_ENTITY_1_1));
        testGetAllWithPaginationCustomSorted(0, 3, List.of(data.VERSIONED_ENTITY_1_1, data.VERSIONED_ENTITY_2_3, data.VERSIONED_ENTITY_3_2));
    }

    @Test
    public void testGetAllWithPaginationExcludingFirst() {
        testGetAllWithPaginationHeightSorted(1, 3, List.of(data.VERSIONED_ENTITY_2_3, data.VERSIONED_ENTITY_1_1));
        testGetAllWithPaginationCustomSorted(1, 3, List.of(data.VERSIONED_ENTITY_2_3, data.VERSIONED_ENTITY_3_2));
    }

    @Test
    public void testGetAllWithPaginationExcludingLast() {
        testGetAllWithPaginationHeightSorted(0, 2, List.of(data.VERSIONED_ENTITY_3_2, data.VERSIONED_ENTITY_2_3));
        testGetAllWithPaginationCustomSorted(0, 2, List.of(data.VERSIONED_ENTITY_1_1, data.VERSIONED_ENTITY_2_3));
    }

    @Test
    public void testGetAllWithPaginationExcludingFirstAndLast() {
        testGetAllWithPaginationHeightSorted(1, 2, List.of(data.VERSIONED_ENTITY_2_3));
        testGetAllWithPaginationCustomSorted(1, 2, List.of(data.VERSIONED_ENTITY_2_3));
    }

    private void testGetAllWithPaginationHeightSorted(int from, int to, List<VersionedDerivedIdEntity> expected) {
        testGetAllWithPagination(from, to, expected, DB_ID_HEIGHT_SORT);
    }

    private void testGetAllWithPaginationCustomSorted(int from, int to, List<VersionedDerivedIdEntity> expected) {
        testGetAllWithPagination(from, to, expected, null);
    }

    private void testGetAllWithPagination(int from, int to, List<VersionedDerivedIdEntity> expected, String sort) {
        DbUtils.inTransaction(extension, (con) -> {
            List<VersionedDerivedIdEntity> actual = getAllWithSort(from, to, sort);
            assertEquals(expected, actual);
            versionedCacheUtils.assertListInCache(expected);
        });

        versionedCacheUtils.assertListNotInCache(expected);

        List<VersionedDerivedIdEntity> actual = getAllWithSort(from, to, sort);
        assertEquals(expected, actual);
        versionedCacheUtils.assertListNotInCache(expected);

    }

    private List<VersionedDerivedIdEntity> getAllWithSort(int from, int to, String sort) {
        List<VersionedDerivedIdEntity> actual;
        if (StringUtils.isBlank(sort)) {
            actual = CollectionUtil.toList(versionedTable.getAll(from, to - 1)); //default sort
        } else {
            actual = CollectionUtil.toList(versionedTable.getAll(from, to - 1, sort)); //default
        }
        return actual;
    }

    @Test
    void testGetAllWithPaginationForMaxHeightWithDefaultSort() {
        testGetAllWithPaginationForHeightHeightSorted(0, 2, List.of(data.VERSIONED_ENTITY_3_2, data.VERSIONED_ENTITY_2_3), Integer.MAX_VALUE);
        testGetAllWithPaginationForHeightHeightSorted(1, 2, List.of(data.VERSIONED_ENTITY_2_3), Integer.MAX_VALUE);
    }

    @Test
    void testGetAllWithPaginationForLastHeightWithDefaultSort() {
        mockBlockchain(Integer.MAX_VALUE);
        int height = data.VERSIONED_ENTITY_3_2.getHeight();
        testGetAllWithPaginationForHeightHeightSorted(1, 3, List.of(data.VERSIONED_ENTITY_2_3, data.VERSIONED_ENTITY_1_1), height);
        testGetAllWithPaginationForHeightHeightSorted(2, 3, List.of(data.VERSIONED_ENTITY_1_1), height);
    }

    @Test
    void testGetAllWithPaginationForMiddleHeightWithDefaultSort() {
        mockBlockchain(Integer.MAX_VALUE);
        int height = data.VERSIONED_ENTITY_2_2.getHeight();
        testGetAllWithPaginationForHeightHeightSorted(1, 4, List.of(data.VERSIONED_ENTITY_2_2, data.VERSIONED_ENTITY_3_1, data.VERSIONED_ENTITY_1_1), height);
        testGetAllWithPaginationForHeightHeightSorted(0, 1, List.of(data.VERSIONED_ENTITY_4_1), height);
    }

    @Test
    void testGetAllWithPaginationForMinHeightWithDefaultSort() {
        mockBlockchain(Integer.MAX_VALUE);
        int height = data.VERSIONED_ENTITY_1_1.getHeight();
        testGetAllWithPaginationForHeightHeightSorted(0, 2, List.of(data.VERSIONED_ENTITY_1_1), height);
    }

    @Test
    void testGetAllWithPaginationForMaxHeightWithCustomSort() {
        testGetAllWithPaginationForHeightCustomSorted(1, 2, List.of(data.VERSIONED_ENTITY_2_3), Integer.MAX_VALUE);
        testGetAllWithPaginationForHeightCustomSorted(0, 3, List.of(data.VERSIONED_ENTITY_1_1, data.VERSIONED_ENTITY_2_3, data.VERSIONED_ENTITY_3_2), Integer.MAX_VALUE);
    }

    @Test
    void testGetAllWithPaginationForLastHeightWithCustomSort() {
        mockBlockchain(Integer.MAX_VALUE);
        int height = data.VERSIONED_ENTITY_3_2.getHeight();
        testGetAllWithPaginationForHeightCustomSorted(0, 2, List.of(data.VERSIONED_ENTITY_1_1, data.VERSIONED_ENTITY_2_3), height);
        testGetAllWithPaginationForHeightCustomSorted(2, 3, List.of(data.VERSIONED_ENTITY_3_2), height);
    }

    @Test
    void testGetAllWithPaginationForMiddleHeightWithCustomSort() {
        mockBlockchain(Integer.MAX_VALUE);
        int height = data.VERSIONED_ENTITY_2_2.getHeight();
        testGetAllWithPaginationForHeightCustomSorted(0, 2, List.of(data.VERSIONED_ENTITY_1_1, data.VERSIONED_ENTITY_2_2), height);
        testGetAllWithPaginationForHeightCustomSorted(2, 4, List.of(data.VERSIONED_ENTITY_3_1, data.VERSIONED_ENTITY_4_1), height);
    }

    @Test
    void testGetAllWithPaginationForMinHeightWithCustomSort() {
        mockBlockchain(Integer.MAX_VALUE);
        int height = data.VERSIONED_ENTITY_1_1.getHeight();
        testGetAllWithPaginationForHeightCustomSorted(0, 2, List.of(data.VERSIONED_ENTITY_1_1), height);
    }

    private void testGetAllWithPaginationForHeightHeightSorted(int from, int to, List<VersionedDerivedIdEntity> expected, int height) {
        testGetAllWithPaginationForHeight(from, to, height, expected, DB_ID_HEIGHT_SORT);
    }

    private void testGetAllWithPaginationForHeightCustomSorted(int from, int to,List<VersionedDerivedIdEntity> expected, int height) {
        testGetAllWithPaginationForHeight(from, to, height, expected, null);
    }


    void testGetAllWithPaginationForHeight(int from, int to, int height, List<VersionedDerivedIdEntity> expected, String sort) {


        DbUtils.inTransaction(extension, (con) -> {

            List<VersionedDerivedIdEntity> actual;
            if (StringUtils.isBlank(sort)) {
                actual = CollectionUtil.toList(versionedTable.getAll(height, from, to - 1)); // default sort
            } else {
                actual = CollectionUtil.toList(versionedTable.getAll(height, from, to - 1, sort)); // custom sort
            }
            //check cache, which should not contain data
            assertEquals(expected, actual);
            if (blockchain.getHeight() <= height || height < 0) {
                versionedCacheUtils.assertListInCache(expected);
            } else {
                versionedCacheUtils.assertListNotInCache(expected);
            }
        });
        versionedCacheUtils.assertListNotInCache(expected);
    }


    @Test
    public void testGetCount() {
        assertEquals(3, versionedTable.getCount());
    }

    @Test
    public void testGetCountByEmptyDbClause() {
        assertEquals(3, versionedTable.getCount(DbClause.EMPTY_CLAUSE));
    }

    @Test
    public void testGetCountByHeightClause() {
        int count = versionedTable.getCount(new DbClause.IntClause("height", DbClause.Op.GTE, data.VERSIONED_ENTITY_2_1.getHeight()));

        assertEquals(2, count);
    }

    @Test
    void testGetCountByDbIdClause() {
        int count = versionedTable.getCount(new DbClause.LongClause("db_id", DbClause.Op.LTE, data.VERSIONED_ENTITY_2_3.getDbId()).and(new DbClause.IntClause("height", DbClause.Op.EQ, data.VERSIONED_ENTITY_1_1.getHeight())));

        assertEquals(1, count);
    }

    @Test
    void testCountByIdClause() {
        int count = versionedTable.getCount(new DbClause.LongClause("id", DbClause.Op.NE, data.VERSIONED_ENTITY_2_3.getId()));

        assertEquals(2, count);
    }

    @Test
    void testGetCountByDbClauseWithLastHeight() {
        mockBlockchain(Integer.MAX_VALUE);

        int count = versionedTable.getCount(new DbClause.IntClause("height", DbClause.Op.NE, data.VERSIONED_ENTITY_2_3.getHeight()), data.VERSIONED_ENTITY_3_2.getHeight());

        assertEquals(2, count);
    }

    @Test
    void testGetCountByDbClauseWithMiddleHeight() {
        mockBlockchain(Integer.MAX_VALUE);

        int count = versionedTable.getCount(new DbClause.LongClause("id", DbClause.Op.NE, data.VERSIONED_ENTITY_2_3.getId()), data.VERSIONED_ENTITY_2_2.getHeight());

        assertEquals(3, count);
    }

    @Test
    void testGetCountByDbClauseWithMinHeight() {
        mockBlockchain(data.VERSIONED_ENTITY_3_2.getHeight() + 1);

        int count = versionedTable.getCount(new DbClause.LongClause("db_id", DbClause.Op.GTE, data.VERSIONED_ENTITY_1_1.getDbId()), data.VERSIONED_ENTITY_1_1.getHeight());

        assertEquals(1, count);
    }

    @Test
    void testGetCountByDbClauseWithMaxHeight() {
        assertEquals(3, versionedTable.getCount(DbClause.EMPTY_CLAUSE, Integer.MAX_VALUE));
    }


    @Test
    void testGetRowCount() {
        assertEquals(8, versionedTable.getRowCount());
    }

    @Test
    void testInsertOutsideTransaction() {
        Assertions.assertThrows(IllegalStateException.class, () -> versionedTable.insert(mock(VersionedDerivedIdEntity.class)));
    }

    @Test
    void testInsertWhenCachedValueDbKeyEqualsToInsertedButReferencesDiffer() {
        Assertions.assertThrows(IllegalStateException.class, () -> DbUtils.inTransaction(extension, (con) -> {
            VersionedDerivedIdEntity t = data.VERSIONED_ENTITY_3_2;
            DbKey dbKey = versionedTable.getDbKeyFactory().newKey(t);
            versionedTable.get(dbKey, true); //add to cache
            VersionedDerivedIdEntity mock = mock(VersionedDerivedIdEntity.class);
            doReturn(dbKey).when(mock).getDbKey();
            versionedTable.insert(mock);
        }));
    }

    @Test
    void testInsert() {
        VersionedDerivedIdEntity value = data.NEW_VERSIONED_ENTITY;
        DbUtils.inTransaction(extension, (con) -> {
            versionedTable.insert(value);
            assertEquals(value, versionedTable.get(versionedTable.getDbKeyFactory().newKey(value)));
            versionedCacheUtils.assertInCache(value);
        });
        versionedCacheUtils.assertNotInCache(value);
    }

    @Test
    public void testInsertAlreadyExist() {
        VersionedDerivedIdEntity value = data.VERSIONED_ENTITY_2_3;
        DbUtils.inTransaction(extension, (con) -> {
            value.setDbId(data.NEW_VERSIONED_ENTITY.getDbId());
            value.setHeight(value.getHeight() + 1);
            versionedTable.insert(value);
            VersionedDerivedIdEntity t = versionedTable.get(versionedTable.getDbKeyFactory().newKey(value));
            assertEquals(t, value);
            versionedCacheUtils.assertInCache(t);
        });
        VersionedDerivedIdEntity actual = versionedTable.get(versionedTable.getDbKeyFactory().newKey(value));
        assertEquals(value, actual);
    }

}
