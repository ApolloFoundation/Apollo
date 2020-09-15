package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.OptionDAO;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ShardDao;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.PrunableDbTable;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ShardState;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.shard.helper.CsvExporter;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvEscaper;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.util.ChunkedFileOps;
import com.apollocurrency.aplwallet.apl.util.FileUtils;
import com.apollocurrency.aplwallet.apl.util.Zip;
import com.apollocurrency.aplwallet.apl.util.ZipImpl;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

@ExtendWith(MockitoExtension.class)
@EnableWeld
class PrunableArchiveMigratorTest {
    @Mock
    ShardDao shardDao;
    @Mock
    OptionDAO optionDAO;
    @Mock
    DirProvider dirProvider;
    @Mock
    BlockchainConfig blockchainConfig;
    Zip zip = spy(new ZipImpl());
    @Mock
    DerivedTablesRegistry registry;
    @Mock
    CsvExporter csvExporter;
    @Mock
    CsvEscaper csvEscaper;
    @Mock
    DatabaseManager databaseManager;
    @RegisterExtension
    TemporaryFolderExtension extension = new TemporaryFolderExtension();
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(Event.class, PropertiesHolder.class, BlockchainConfig.class).build(); // only for Prunable db table mocks
    PrunableArchiveMigrator migrator;
    UUID chainId = UUID.fromString("3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6");
    Shard shard1 = new Shard(1L, new byte[32], ShardState.FULL, 2000, null, new long[3], new int[3], new int[3], new byte[32]);
    Shard shard2 = new Shard(2L, new byte[32], ShardState.FULL, 4000, null, new long[3], new int[3], new int[3], new byte[32]);
    @Inject
    Event<ChunkedFileOps> event;

    @BeforeEach
    void setUp() {
        migrator = spy(new PrunableArchiveMigrator(event, shardDao, optionDAO, dirProvider, blockchainConfig, zip, registry, databaseManager, csvEscaper));
    }


    @Test
    void testMigrateAlreadyMigrated() {
        doReturn("false").when(optionDAO).get(anyString());
        mockChain();
        migrator.migrate();
        verifyZeroInteractions(shardDao, dirProvider, zip, registry);
    }

    @Test
    void testMigrateFromScratch() throws IOException {
        MockData mockData = mockSuccessCase();

        migrator.migrate();

        checkContent(mockData.firstZipPath, shard1, mockData.hash1);
        checkContent(mockData.secondZipPath, shard2, mockData.hash2);

        verify(csvExporter, times(2)).exportShardTableIgnoringLastZipHashes(anyInt(), anyInt());
        verify(shardDao).updateShard(shard1);
        verify(shardDao).updateShard(shard2);
        verify(optionDAO).set("current-shard-for-migration", "1");
        verify(optionDAO).set("current-shard-for-migration", "2");
        verify(optionDAO).set("prunable-shard-migration-finished", "false");
    }

    void checkContent(Path path, Shard shard, byte[] hash) throws IOException {
        assertTrue(Files.exists(path));
        ChunkedFileOps fops = new ChunkedFileOps(path.toAbsolutePath().toString());
        byte[] newHash = fops.getFileHash();
        assertFalse(Arrays.equals(newHash, hash));
        assertArrayEquals(newHash, shard.getCoreZipHash());
        assertArrayEquals(new byte[32], shard.getPrunableZipHash());
        Path extractPath = extension.newFolder().toPath();
        zip.extract(path.toAbsolutePath().toString(), extractPath.toAbsolutePath().toString(), true);
        assertEquals(2, FileUtils.countElementsOfDirectory(extractPath));
        assertTrue(Files.exists(extractPath.resolve("not-prunable.csv")));
        assertTrue(Files.exists(extractPath.resolve("shard.csv")));
        assertEquals(List.of("shard1"), Files.readAllLines(extractPath.resolve("shard.csv")));
    }

    @Test
    void testResumeMigration() throws IOException {
        MockData mockData = mockSuccessCase();

        doReturn("2").when(optionDAO).get("current-shard-for-migration");
        doReturn(null).when(optionDAO).get("prunable-shard-migration-finished");

        migrator.migrate();

        checkContent(mockData.secondZipPath, shard2, mockData.hash2);

        verify(shardDao).updateShard(shard2);
        verify(shardDao).getAllCompletedShards();
        verifyNoMoreInteractions(shardDao);
        verify(optionDAO).set("current-shard-for-migration", "2");
        verify(optionDAO).set("prunable-shard-migration-finished", "false");
    }

    private MockData mockSuccessCase() throws IOException {
        mockPrunableTablesAndShards();
        mockChain();

        Path dataExportFolder = extension.newFolder().toPath();
        Files.createFile(dataExportFolder.resolve("not-prunable.csv"));
        Files.createFile(dataExportFolder.resolve("prunable_table.csv"));
        Files.createFile(dataExportFolder.resolve("shard.csv"));
        Path firstZipPath = dataExportFolder.resolve("apl_blockchain_shard_1_3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6.zip");
        Path secondZipPath = dataExportFolder.resolve("aapl_blockchain_shard_2_3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6.zip");
        ChunkedFileOps ops1 = zip.compressAndHash(firstZipPath.toAbsolutePath().toString(), dataExportFolder.toAbsolutePath().toString(), 0L, ((dir, name) -> name.endsWith(".csv")), false);
        byte[] hash1 = null;
        if (ops1 != null && ops1.isHashedOK()) {
            hash1 = ops1.getFileHash();
        }
        ChunkedFileOps ops2 = zip.compressAndHash(secondZipPath.toAbsolutePath().toString(), dataExportFolder.toAbsolutePath().toString(), 0L, ((dir, name) -> name.endsWith(".csv")), false);
        byte[] hash2 = null;
        if (ops2 != null && ops2.isHashedOK()) {
            hash2 = ops2.getFileHash();
        }
        doReturn(dataExportFolder).when(dirProvider).getDataExportDir();
        AtomicReference<Path> tempDirPath = new AtomicReference<>();
        doAnswer(invocation -> {
            Object argument = invocation.getArgument(0);
            tempDirPath.set((Path) argument);
            return csvExporter;
        }).when(migrator).createExporter(any(Path.class));
        doAnswer(invocation -> {
            Path shardFilePath = tempDirPath.get().resolve("shard.csv");
            if (Files.exists(shardFilePath)) {
                Files.delete(shardFilePath);
            }
            Files.createFile(shardFilePath);
            Files.writeString(shardFilePath, "shard1");
            return 1L;
        }).when(csvExporter).exportShardTableIgnoringLastZipHashes(anyInt(), anyInt());
        return new MockData(hash1, hash2, firstZipPath, secondZipPath);
    }

    @Test
    void testThrowExceptionDuringMigration() throws IOException {
        mockPrunableTablesAndShards();
        mockChain();

        assertThrows(RuntimeException.class, () -> migrator.migrate()); // full shards exists in db but archives are absent

        verify(shardDao).getAllCompletedShards();
        verifyNoMoreInteractions(shardDao);
        verifyZeroInteractions(zip);
    }

    private void mockPrunableTablesAndShards() {
        PrunableDbTable prunableDbTable = mock(PrunableDbTable.class);
        doReturn("prunable_table").when(prunableDbTable).getName();
        doReturn(List.of(prunableDbTable)).when(registry).getDerivedTables();
        doReturn(List.of(shard1, shard2)).when(shardDao).getAllCompletedShards();
    }

    private void mockChain() {
        Chain chain = mock(Chain.class);
        doReturn(chainId).when(chain).getChainId();
        doReturn(chain).when(blockchainConfig).getChain();
    }

    @Data
    @AllArgsConstructor
    private static class MockData {
        byte[] hash1;
        byte[] hash2;
        Path firstZipPath;
        Path secondZipPath;
    }

}