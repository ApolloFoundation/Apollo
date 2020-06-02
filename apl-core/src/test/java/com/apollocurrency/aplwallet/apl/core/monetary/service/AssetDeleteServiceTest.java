/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.monetary.service;

import com.apollocurrency.aplwallet.apl.core.converter.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.service.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.monetary.dao.AssetDeleteTable;
import com.apollocurrency.aplwallet.apl.core.monetary.model.AssetDelete;
import com.apollocurrency.aplwallet.apl.data.AssetDeleteTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AssetDeleteServiceTest {

    @Mock
    private AssetDeleteTable table;
    @Mock
    private BlockChainInfoService blockChainInfoService;
    @Mock
    private IteratorToStreamConverter<AssetDelete> assetDeleteIteratorToStreamConverter;

    @Inject
    AssetDeleteService service;
    AssetDeleteTestData td;

    @BeforeEach
    void setUp() {
        td = new AssetDeleteTestData();
        service = spy(new AssetDeleteServiceImpl(table, blockChainInfoService, assetDeleteIteratorToStreamConverter));
    }

    @Test
    void test_getAssetDeletes() {
        //GIVEN
        DbIterator<AssetDelete> dbIt = mock( DbIterator.class);
        doReturn(dbIt).when(table).getManyBy(any(DbClause.class), eq(0), eq(10));

        //WHEN
        service.getAssetDeletes(1000L, 0, 10);

        //THEN
        verify(table).getManyBy(any(DbClause.class), eq(0), eq(10));
    }

    @Test
    void test_getAssetDeletesStream() {
        //GIVEN
        DbIterator<AssetDelete> dbIt = mock( DbIterator.class);
        doReturn(dbIt).when(table).getManyBy(any(DbClause.class), eq(0), eq(10));
        Stream<AssetDelete> expected = Stream.of(td.ASSET_DELETE_0, td.ASSET_DELETE_1, td.ASSET_DELETE_2);
        doReturn(expected).when(assetDeleteIteratorToStreamConverter).apply(dbIt);

        //WHEN
        Stream<AssetDelete> result = service.getAssetDeletesStream(1000L, 0, 10);
        assertEquals(expected, result);

        //THEN
        verify(table).getManyBy(any(DbClause.class), eq(0), eq(10));
        verify(assetDeleteIteratorToStreamConverter).apply(dbIt);
    }

    @Test
    void getAccountAssetDeletes() {
        //GIVEN
        DbIterator<AssetDelete> dbIt = mock( DbIterator.class);
        doReturn(dbIt).when(table).getManyBy(any(DbClause.class), eq(0), eq(10), any(String.class));

        //WHEN
        service.getAccountAssetDeletes(1000L, 0, 10);

        //THEN
        verify(table).getManyBy(any(DbClause.class), eq(0), eq(10), any(String.class));
    }

    @Test
    void getAccountAssetDeletesStream() {
        //GIVEN
        DbIterator<AssetDelete> dbIt = mock( DbIterator.class);
        doReturn(dbIt).when(table).getManyBy(any(DbClause.class), eq(0), eq(10), any(String.class));
        Stream<AssetDelete> expected = Stream.of(td.ASSET_DELETE_0, td.ASSET_DELETE_1, td.ASSET_DELETE_2);
        doReturn(expected).when(assetDeleteIteratorToStreamConverter).apply(dbIt);

        //WHEN
        Stream<AssetDelete> result = service.getAccountAssetDeletesStream(1000L, 0, 10);
        assertEquals(expected, result);

        //THEN
        verify(table).getManyBy(any(DbClause.class), eq(0), eq(10), any(String.class));
        verify(assetDeleteIteratorToStreamConverter).apply(dbIt);
    }

    @Test
    void testGetAccountAssetDeletes() {
        //GIVEN
        DbIterator<AssetDelete> dbIt = mock( DbIterator.class);
        doReturn(dbIt).when(table).getManyBy(any(DbClause.class), eq(0), eq(10), any(String.class));

        //WHEN
        service.getAccountAssetDeletes(1000L, 1000L, 0, 10);

        //THEN
        verify(table).getManyBy(any(DbClause.class), eq(0), eq(10), any(String.class));
    }

    @Test
    void testGetAccountAssetDeletesStream() {
        //GIVEN
        DbIterator<AssetDelete> dbIt = mock( DbIterator.class);
        doReturn(dbIt).when(table).getManyBy(any(DbClause.class), eq(0), eq(10), any(String.class));
        Stream<AssetDelete> expected = Stream.of(td.ASSET_DELETE_0, td.ASSET_DELETE_1, td.ASSET_DELETE_2);
        doReturn(expected).when(assetDeleteIteratorToStreamConverter).apply(dbIt);

        //WHEN
        Stream<AssetDelete> result = service.getAccountAssetDeletesStream(1000L,1000L, 0, 10);
        assertEquals(expected, result);

        //THEN
        verify(table).getManyBy(any(DbClause.class), eq(0), eq(10), any(String.class));
        verify(assetDeleteIteratorToStreamConverter).apply(dbIt);
    }

}