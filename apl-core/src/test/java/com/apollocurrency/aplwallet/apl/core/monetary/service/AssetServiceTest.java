/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.monetary.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.stream.Stream;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.converter.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.service.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.monetary.dao.AssetTable;
import com.apollocurrency.aplwallet.apl.core.monetary.model.Asset;
import com.apollocurrency.aplwallet.apl.core.monetary.model.AssetDelete;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetIssuance;
import com.apollocurrency.aplwallet.apl.data.AssetTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private Blockchain blockchain;
    @Mock
    private BlockchainConfig blockchainConfig;
    @Mock
    private AssetTable table;
    @Mock
    private BlockChainInfoService blockChainInfoService;
    @Mock
    private AssetDeleteService assetDeleteService;
    @Mock
    private IteratorToStreamConverter<Asset> assetIteratorToStreamConverter;

//    @Inject
    AssetService service;
    AssetTestData td;

    @BeforeEach
    void setUp() {
        td = new AssetTestData();
        service = new AssetServiceImpl(table, blockChainInfoService, assetDeleteService, assetIteratorToStreamConverter);
    }

    @Test
    void getAllAssets() {
        //GIVEN
        DbIterator<Asset> dbIt = mock( DbIterator.class);
        doReturn(dbIt).when(table).getAll(eq(0), eq(10));
        Stream<Asset> expected = Stream.of(td.ASSET_0, td.ASSET_1, td.ASSET_2);
        doReturn(expected).when(assetIteratorToStreamConverter).apply(dbIt);

        //WHEN
        Stream<Asset> result = service.getAllAssetsStream(0, 10);
        assertEquals(expected, result);

        //THEN
        verify(table).getAll(eq(0), eq(10));
        verify(assetIteratorToStreamConverter).apply(dbIt);
    }

    @Test
    void getCount() {
        //GIVEN
        doReturn(10).when(table).getCount();

        //WHEN
        int result = service.getCount();
        assertEquals(10, result);

        //THEN
        verify(table).getCount();
    }

    @Test
    void getAsset() {
        //GIVEN
        doReturn(td.ASSET_0).when(table).get(any(DbKey.class));

        //WHEN
        Asset result = service.getAsset(td.ASSET_0.getAssetId());
        assertNotNull(result);

        //THEN
        verify(table).get(any(DbKey.class));
    }

    @Test
    void testGetAsset_with_height() {
        //GIVEN
        doReturn(td.ASSET_0).when(table).get(any(DbKey.class), any(Integer.class));

        //WHEN
        Asset result = service.getAsset(td.ASSET_0.getAssetId(), 10);
        assertNotNull(result);

        //THEN
        verify(table).get(any(DbKey.class), any(Integer.class));
    }

    @Test
    void getAssetsIssuedBy() {
        //GIVEN
        DbIterator<Asset> dbIt = mock( DbIterator.class);
        doReturn(dbIt).when(table).getManyBy(any(DbClause.class), eq(0), eq(10));
        Stream<Asset> expected = Stream.of(td.ASSET_0, td.ASSET_1, td.ASSET_2);
        doReturn(expected).when(assetIteratorToStreamConverter).apply(dbIt);

        //WHEN
        Stream<Asset> result = service.getAssetsIssuedByStream(100L, 0, 10);
        assertEquals(expected, result);

        //THEN
        verify(table).getManyBy(any(DbClause.class), eq(0), eq(10));
        verify(assetIteratorToStreamConverter).apply(dbIt);
    }

    @Test
    void searchAssets() {
        //GIVEN
        DbIterator<Asset> dbIt = mock( DbIterator.class);
        doReturn(dbIt).when(table).search(any(String.class), any(DbClause.class), eq(0), eq(10), any(String.class));
        Stream<Asset> expected = Stream.of(td.ASSET_0, td.ASSET_1, td.ASSET_2);
        doReturn(expected).when(assetIteratorToStreamConverter).apply(dbIt);

        //WHEN
        Stream<Asset> result = service.searchAssetsStream("seqrchQuery", 0, 10);
        assertEquals(expected, result);

        //THEN
        verify(table).search(any(String.class), any(DbClause.class), eq(0), eq(10), any(String.class));
        verify(assetIteratorToStreamConverter).apply(dbIt);
    }

    @Test
    void addAsset() {
        //GIVEN
        Transaction tr = mock(Transaction.class);
        ColoredCoinsAssetIssuance attach = mock(ColoredCoinsAssetIssuance.class);
        doNothing().when(table).insert(any(Asset.class));

        //WHEN
        service.addAsset(tr, attach);

        //THEN
        verify(table).insert(any(Asset.class));
    }

    @Test
    void deleteAsset() {
        //GIVEN
        Transaction tr = mock(Transaction.class);
        doReturn(td.ASSET_0).when(table).get(any(DbKey.class));
        doNothing().when(table).insert(any(Asset.class));
        AssetDelete assetDelete = mock(AssetDelete.class);
        doReturn(assetDelete).when(assetDeleteService).addAssetDelete(tr, td.ASSET_0.getAssetId(), 10);
        doReturn(100).when(blockChainInfoService).getHeight();

        //WHEN
        service.deleteAsset(tr, td.ASSET_0.getAssetId(), 10L);

        //THEN
        verify(table).get(any(DbKey.class));
        verify(table).insert(any(Asset.class));
        verify(assetDeleteService).addAssetDelete(tr, td.ASSET_0.getAssetId(), 10);
    }
}