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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import javax.inject.Inject;

import java.util.stream.Stream;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.converter.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.service.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.monetary.dao.AssetTransferTable;
import com.apollocurrency.aplwallet.apl.core.monetary.model.AssetTransfer;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetTransfer;
import com.apollocurrency.aplwallet.apl.data.AssetTransferTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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