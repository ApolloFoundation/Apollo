package com.apollocurrency.aplwallet.apl.core.monetary.service;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import javax.inject.Inject;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.service.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.monetary.dao.AssetDeleteTable;
import com.apollocurrency.aplwallet.apl.core.monetary.model.AssetDelete;
import com.apollocurrency.aplwallet.apl.data.AssetDeleteTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetDeleteServiceTest {

    @Mock
    private Blockchain blockchain;
    @Mock
    private BlockchainConfig blockchainConfig;
    @Mock
    private AssetDeleteTable table;
    @Mock
    private BlockChainInfoService blockChainInfoService;

    @Inject
    AssetDeleteService service;
    AssetDeleteTestData td;

    @BeforeEach
    void setUp() {
        td = new AssetDeleteTestData();
        service = spy(new AssetDeleteServiceImpl(table, blockChainInfoService));
    }

    @Disabled
    void getAssetDeletes() {
        //GIVEN
        DbClause clause = new DbClause.LongClause("asset_id", td.ASSET_DELETE_0.getAssetId());
        DbIterator<AssetDelete> dbIt = mock( DbIterator.class);
/*
        DbIterator<AssetDelete> dbIt = mock( new DbIterator<>(mock(Connection.class), mock(PreparedStatement.class),
            (conection, rs) -> {
                long account = rs.getLong("account");
                long timestamp = rs.getLong("timestamp");
                return new AssetDelete(1L, 1L, 1L, 10, 10, 10);
            }));
*/
        doReturn(dbIt).when(table).getManyBy(clause, 0, 10);
//
//        @SuppressWarnings("unchecked") final EntityDbTable<AssetDelete> table = mock(EntityDbTable.class);

//        List<AssetDelete> result = toList(service.getAssetDeletes(td.ASSET_DELETE_0.getAssetId(), 0, 10) );
//        assertEquals(td.ALL_ASSETS_DELETE_ORDERED_BY_DBID, result);

        //WHEN
        service.getAssetDeletes(td.ASSET_DELETE_0.getAssetId(), 0, 10);

        //THEN
        verify(table).getManyBy(clause, 0, 10);
    }

    @Test
    void getAccountAssetDeletes() {
    }

    @Test
    void testGetAccountAssetDeletes() {
    }

    @Test
    void addAssetDelete() {
    }
}