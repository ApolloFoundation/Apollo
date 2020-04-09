package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardState;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.message.PrunableMessageTable;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvEscaperImpl;
import com.apollocurrency.aplwallet.apl.core.shard.observer.TrimData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.util.ChunkedFileOps;
import com.apollocurrency.aplwallet.apl.util.FileUtils;
import com.apollocurrency.aplwallet.apl.util.Zip;
import com.apollocurrency.aplwallet.apl.util.ZipImpl;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyZeroInteractions;

@EnableWeld
class ShardPrunableZipHashCalculatorTest {
    DerivedTablesRegistry registry = mock(DerivedTablesRegistry.class);
    Zip zip = spy(new ZipImpl());
    ShardDao shardDao = mock(ShardDao.class);
    BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    DirProvider dirProvider = mock(DirProvider.class);

    @RegisterExtension
    TemporaryFolderExtension tempFolder = new TemporaryFolderExtension();
    @RegisterExtension
    DbExtension dbExtension = new DbExtension();
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(PrunableMessageTable.class,
        Event.class,
        ShardPrunableZipHashCalculator.class,
        PropertiesHolder.class,
        FullTextConfigImpl.class,
        CsvEscaperImpl.class)
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(registry, DerivedTablesRegistry.class))
        .addBeans(MockBean.of(zip, Zip.class))
        .addBeans(MockBean.of(shardDao, ShardDao.class))
        .addBeans(MockBean.of(dirProvider, DirProvider.class))
        .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
        .build();
    @Inject
    PrunableMessageTable prunableMessageTable;
    @Inject
    Event<TrimData> trimDataEvent;
    @Inject
    Event<ChunkedFileOps> fileChangedEvent;
    @Inject
    ShardPrunableZipHashCalculator prunableZipHashCalculator;
    UUID chainId = UUID.fromString("3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6");
    Shard shard1 = new Shard(1L, new byte[32], ShardState.FULL, 12, new byte[32], new long[3], new int[3], new int[3], null);
    Shard shard2 = new Shard(2L, new byte[32], ShardState.FULL, 20, new byte[32], new long[3], new int[3], new int[3], new byte[32]);
    Shard shard3 = new Shard(3L, new byte[32], ShardState.FULL, 28, new byte[32], new long[3], new int[3], new int[3], new byte[32]);


    /*@Test
    void testTriggerAsyncTrimDoneEvent() {
        doReturn(List.of()).when(shardDao).getAllCompletedShards();
        mockChain();
        trimDataEvent.select(new AnnotationLiteral<Async>() {}).fire(new TrimData(200, 300, 250));

        verify(shardDao).getAllCompletedShards();
        verifyZeroInteractions(zip, dirProvider);
    }*/

    @Test
    void testTryRecalculatePrunableArchiveHashes() throws IOException {
        Path dataExportDir = tempFolder.newFolder().toPath();
        DerivedTableInterface derivedTable = mock(DerivedTableInterface.class);
        doReturn(50).when(blockchainConfig).getMinPrunableLifetime();
        mockChain();
        doReturn(dataExportDir).when(dirProvider).getDataExportDir();
        doReturn(List.of(shard1, shard2, shard3)).when(shardDao).getAllCompletedShards();
        doReturn(List.of(prunableMessageTable, derivedTable)).when(registry).getDerivedTables();
        Path secondZipPath = dataExportDir.resolve("apl-blockchain-shardprun-2-chain-" + chainId.toString() + ".zip");
        Path thirdZipPath = dataExportDir.resolve("apl-blockchain-shardprun-3-chain-" + chainId.toString() + ".zip");
        Files.createFile(secondZipPath);
        Files.createFile(thirdZipPath);

        prunableZipHashCalculator.tryRecalculatePrunableArchiveHashes(250);

        assertFalse(Files.exists(secondZipPath));
        assertTrue(Files.exists(thirdZipPath));
        zip.extract(thirdZipPath.toAbsolutePath().toString(), dataExportDir.toAbsolutePath().toString(), true);
        assertEquals(2, FileUtils.countElementsOfDirectory(dataExportDir));
        assertTrue(Files.exists(dataExportDir.resolve("prunable_message.csv")));
        assertNull(shard2.getPrunableZipHash());
        ChunkedFileOps ops = new ChunkedFileOps(thirdZipPath.toAbsolutePath().toString());
        assertArrayEquals(ops.getFileHash(), shard3.getPrunableZipHash());
        assertEquals(250, prunableZipHashCalculator.getLastPruningTime());
    }

    @Test
    void testSkipRecalculationWhenTimeIsNotGreaterThanPrevPruningTime() {
        prunableZipHashCalculator.tryRecalculatePrunableArchiveHashes(0);

        verifyZeroInteractions(shardDao, zip, dirProvider);
    }

    private void mockChain() {
        Chain chain = mock(Chain.class);
        doReturn(chainId).when(chain).getChainId();
        doReturn(chain).when(blockchainConfig).getChain();
    }
}