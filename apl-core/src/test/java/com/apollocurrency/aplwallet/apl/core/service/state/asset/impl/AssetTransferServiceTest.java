/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.asset.impl;

import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.asset.AssetTransferTable;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.AssetTransfer;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetTransferService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetTransfer;
import com.apollocurrency.aplwallet.apl.data.AssetTransferTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.inject.Inject;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AssetTransferServiceTest {

    @Mock
    private AssetTransferTable table;
    @Mock
    private BlockChainInfoService blockChainInfoService;
    @Mock
    private IteratorToStreamConverter<AssetTransfer> assetTransferIteratorToStreamConverter;

    @Inject
    AssetTransferService service;
    AssetTransferTestData td;

    @BeforeEach
    void setUp() {
        td = new AssetTransferTestData();
        service = spy(new AssetTransferServiceImpl(table, blockChainInfoService, assetTransferIteratorToStreamConverter));
    }

    @Test
    void getAllTransfersStream() {
        //GIVEN
        DbIterator<AssetTransfer> dbIt = mock( DbIterator.class);
        doReturn(dbIt).when(table).getAll(eq(0), eq(10));
        Stream<AssetTransfer> expected = Stream.of(td.ASSET_TRANSFER_8, td.ASSET_TRANSFER_7, td.ASSET_TRANSFER_6, td.ASSET_TRANSFER_5);
        doReturn(expected).when(assetTransferIteratorToStreamConverter).apply(dbIt);

        //WHEN
        Stream<AssetTransfer> result = service.getAllTransfersStream(0, 10);
        assertEquals(expected, result);

        //THEN
        verify(table).getAll(eq(0), eq(10));
        verify(assetTransferIteratorToStreamConverter).apply(dbIt);
    }

    @Test
    void getCount() {
        //GIVEN
        int expected = 10;
        doReturn(expected).when(table).getCount();

        //WHEN
        int result = service.getCount();
        assertEquals(expected, result);

        //THEN
        verify(table).getCount();
    }

    @Test
    void getAssetTransfersStream() {
        //GIVEN
        DbIterator<AssetTransfer> dbIt = mock( DbIterator.class);
        doReturn(dbIt).when(table).getManyBy(any(DbClause.class), eq(0), eq(10));
        Stream<AssetTransfer> expected = Stream.of(td.ASSET_TRANSFER_8, td.ASSET_TRANSFER_7, td.ASSET_TRANSFER_6, td.ASSET_TRANSFER_5);
        doReturn(expected).when(assetTransferIteratorToStreamConverter).apply(dbIt);

        //WHEN
        Stream<AssetTransfer> result = service.getAssetTransfersStream(1000L, 0, 10);
        assertEquals(expected, result);

        //THEN
        verify(table).getManyBy(any(DbClause.class), eq(0), eq(10));
        verify(assetTransferIteratorToStreamConverter).apply(dbIt);
    }

    @Test
    void getAccountAssetTransfersStream() {
        //GIVEN
        DbIterator<AssetTransfer> dbIt = mock( DbIterator.class);
        doReturn(dbIt).when(table).getAccountAssetTransfers(eq(1000L), eq(0), eq(10));
        Stream<AssetTransfer> expected = Stream.of(td.ASSET_TRANSFER_8, td.ASSET_TRANSFER_7, td.ASSET_TRANSFER_6, td.ASSET_TRANSFER_5);
        doReturn(expected).when(assetTransferIteratorToStreamConverter).apply(dbIt);

        //WHEN
        Stream<AssetTransfer> result = service.getAccountAssetTransfersStream(1000L, 0, 10);
        assertEquals(expected, result);

        //THEN
        verify(table).getAccountAssetTransfers(eq(1000L), eq(0), eq(10));
        verify(assetTransferIteratorToStreamConverter).apply(dbIt);
    }

    @Test
    void testGetAccountAssetTransfersStream() {
        //GIVEN
        DbIterator<AssetTransfer> dbIt = mock( DbIterator.class);
        doReturn(dbIt).when(table).getAccountAssetTransfers(eq(1000L), eq(1000L), eq(0), eq(10));
        Stream<AssetTransfer> expected = Stream.of(td.ASSET_TRANSFER_8, td.ASSET_TRANSFER_7, td.ASSET_TRANSFER_6, td.ASSET_TRANSFER_5);
        doReturn(expected).when(assetTransferIteratorToStreamConverter).apply(dbIt);

        //WHEN
        Stream<AssetTransfer> result = service.getAccountAssetTransfersStream(1000L, 1000L, 0, 10);
        assertEquals(expected, result);

        //THEN
        verify(table).getAccountAssetTransfers(eq(1000L), eq(1000L), eq(0), eq(10));
        verify(assetTransferIteratorToStreamConverter).apply(dbIt);
    }

    @Test
    void getTransferCount() {
        //GIVEN
        int expected = 10;
        doReturn(expected).when(table).getCount(any(DbClause.class));

        //WHEN
        int result = service.getTransferCount(1000L);
        assertEquals(expected, result);

        //THEN
        verify(table).getCount(any(DbClause.class));
    }

    @Test
    void addAssetTransfer() {
        //GIVEN
        Transaction tr = mock(Transaction.class);
        ColoredCoinsAssetTransfer attach = mock(ColoredCoinsAssetTransfer.class);
        Block lastBlock = mock(Block.class);
        doReturn(10000).when(lastBlock).getTimestamp();
        doReturn(10000).when(lastBlock).getHeight();
        doReturn(lastBlock).when(blockChainInfoService).getLastBlock();
        doNothing().when(table).insert(any(AssetTransfer.class));

        //WHEN
        AssetTransfer result = service.addAssetTransfer(tr, attach);
        assertNotNull(result);

        //THEN
        verify(table).insert(any(AssetTransfer.class));
        verify(blockChainInfoService).getLastBlock();
    }
}