/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.asset.impl;

import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.asset.AssetTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.AssetDelete;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchUpdater;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetDeleteService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetIssuance;
import com.apollocurrency.aplwallet.apl.data.AssetTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    AssetService service;
    AssetTestData td;
    @Mock
    private AssetTable assetTable;
    @Mock
    private BlockChainInfoService blockChainInfoService;
    @Mock
    private AssetDeleteService assetDeleteService;
    @Mock
    private IteratorToStreamConverter<Asset> assetIteratorToStreamConverter;
    @Mock
    private FullTextSearchUpdater fullTextSearchUpdater;
    @Mock
    private FullTextSearchService fullTextSearchService;

    @BeforeEach
    void setUp() {
        td = new AssetTestData();
        service = new AssetServiceImpl(assetTable, blockChainInfoService, assetDeleteService,
            assetIteratorToStreamConverter, fullTextSearchUpdater, fullTextSearchService);
    }

    @Test
    void getAllAssets() {
        //GIVEN
        DbIterator<Asset> dbIt = mock(DbIterator.class);
        doReturn(dbIt).when(assetTable).getAll(eq(0), eq(10));
        Stream<Asset> expected = Stream.of(td.ASSET_0, td.ASSET_1, td.ASSET_2);
        doReturn(expected).when(assetIteratorToStreamConverter).apply(dbIt);

        //WHEN
        Stream<Asset> result = service.getAllAssetsStream(0, 10);
        assertEquals(expected, result);

        //THEN
        verify(assetTable).getAll(eq(0), eq(10));
        verify(assetIteratorToStreamConverter).apply(dbIt);
    }

    @Test
    void getCount() {
        //GIVEN
        doReturn(10).when(assetTable).getCount();

        //WHEN
        int result = service.getCount();
        assertEquals(10, result);

        //THEN
        verify(assetTable).getCount();
    }

    @Test
    void getAsset() {
        //GIVEN
        doReturn(td.ASSET_0).when(assetTable).get(any(DbKey.class));

        //WHEN
        Asset result = service.getAsset(td.ASSET_0.getId());
        assertNotNull(result);

        //THEN
        verify(assetTable).get(any(DbKey.class));
    }

    @Test
    void testGetAsset_with_height() {
        //GIVEN
        doReturn(td.ASSET_0).when(assetTable).get(any(DbKey.class), any(Integer.class));

        //WHEN
        Asset result = service.getAsset(td.ASSET_0.getId(), 10);
        assertNotNull(result);

        //THEN
        verify(assetTable).get(any(DbKey.class), any(Integer.class));
    }

    @Test
    void getAssetsIssuedBy() {
        //GIVEN
        DbIterator<Asset> dbIt = mock(DbIterator.class);
        doReturn(dbIt).when(assetTable).getManyBy(any(DbClause.class), eq(0), eq(10));
        Stream<Asset> expected = Stream.of(td.ASSET_0, td.ASSET_1, td.ASSET_2);
        doReturn(expected).when(assetIteratorToStreamConverter).apply(dbIt);

        //WHEN
        Stream<Asset> result = service.getAssetsIssuedByStream(100L, 0, 10);
        assertEquals(expected, result);

        //THEN
        verify(assetTable).getManyBy(any(DbClause.class), eq(0), eq(10));
        verify(assetIteratorToStreamConverter).apply(dbIt);
    }

    @Test
    void searchAssets() throws SQLException {
        //GIVEN
        DbIterator<Asset> dbIt = mock(DbIterator.class);
        doReturn("asset").when(assetTable).getTableName();
        Stream<Asset> expected = Stream.of();
        ResultSet rs = mock(ResultSet.class);
        doReturn(rs).when(fullTextSearchService)
            .search("public", "asset", "searchQuery", Integer.MAX_VALUE, 0);

        //WHEN
        Stream<Asset> result = service.searchAssetsStream("searchQuery", 0, 10);
        assertNotNull(result);

        //THEN
        verify(assetTable, never()).search(any(String.class), any(DbClause.class), eq(0), eq(10), any(String.class));
        verify(assetIteratorToStreamConverter, never()).apply(dbIt);
        verify(fullTextSearchService).search("public", "asset", "searchQuery", Integer.MAX_VALUE, 0);
    }

    @Test
    void addAsset() {
        //GIVEN
        Transaction tr = mock(Transaction.class);
        ColoredCoinsAssetIssuance attach = mock(ColoredCoinsAssetIssuance.class);
        doNothing().when(assetTable).insert(any(Asset.class));
        doReturn("asset").when(assetTable).getTableName();

        //WHEN
        service.addAsset(tr, attach);

        //THEN
        verify(assetTable).insert(any(Asset.class));
        verify(fullTextSearchUpdater).putFullTextOperationData(any(FullTextOperationData.class));
    }

    @Test
    void deleteAsset() {
        //GIVEN
        Transaction tr = mock(Transaction.class);
        doReturn(td.ASSET_0).when(assetTable).get(any(DbKey.class));
        doNothing().when(assetTable).insert(any(Asset.class));
        AssetDelete assetDelete = mock(AssetDelete.class);
        doReturn(assetDelete).when(assetDeleteService).addAssetDelete(tr, td.ASSET_0.getId(), 10);
        doReturn(100).when(blockChainInfoService).getHeight();
        doReturn("asset").when(assetTable).getTableName();

        //WHEN
        service.deleteAsset(tr, td.ASSET_0.getId(), 10L);

        //THEN
        verify(assetTable).get(any(DbKey.class));
        verify(assetTable).insert(any(Asset.class));
        verify(assetDeleteService).addAssetDelete(tr, td.ASSET_0.getId(), 10);
        verify(fullTextSearchUpdater).putFullTextOperationData(any(FullTextOperationData.class));
    }
}