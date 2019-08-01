package com.apollocurrency.aplwallet.apl.core.shard;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardState;
import com.apollocurrency.aplwallet.apl.core.db.derived.PrunableDbTable;
import com.apollocurrency.aplwallet.apl.core.db.model.OptionDAO;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.util.Zip;
import com.apollocurrency.aplwallet.apl.util.ZipImpl;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
@EnableWeld
class PrunableArchiveMigratorTest {
    @Mock ShardDao shardDao;
    @Mock OptionDAO optionDAO;
    @Mock DirProvider dirProvider;
    @Mock BlockchainConfig blockchainConfig;
    Zip zip = spy(new ZipImpl());
    @Mock DerivedTablesRegistry registry;
    @RegisterExtension
    TemporaryFolderExtension extension = new TemporaryFolderExtension();
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(PropertiesHolder.class, BlockchainConfig.class).build(); // only for Prunable db table mocks
    PrunableArchiveMigrator migrator;
    UUID chainId = UUID.fromString("3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6");
    Shard shard1 = new Shard(1L, new byte[32], ShardState.FULL, 2000, new byte[32], new long[3], new int[3], new int[3], new byte[32]);
    Shard shard2 = new Shard(2L, new byte[32], ShardState.FULL, 4000, new byte[32], new long[3], new int[3], new int[3], new byte[32]);

    @BeforeEach
    void setUp() {
        migrator = new PrunableArchiveMigrator(shardDao, optionDAO, dirProvider, blockchainConfig, zip, registry);
    }


    @Test
    void testMigrateAlreadyMigrated() {
        doReturn("false").when(optionDAO).get(anyString());

        migrator.migrate();

        verifyZeroInteractions(shardDao, dirProvider, blockchainConfig, zip, registry);
    }

    @Test
    void testMigrateFromScratch() throws IOException {
        mockPrunableTablesAndShards();
        mockChain();

        Path dataExportFolder = extension.newFolder().toPath();
        Files.createFile(dataExportFolder.resolve("not-prunable.csv"));
        Files.createFile(dataExportFolder.resolve("prunable_table.csv"));
        Path firstZipPath = dataExportFolder.resolve("apl-blockchain-shard-1-chain-3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6.zip");
        Path secondZipPath = dataExportFolder.resolve("apl-blockchain-shard-2-chain-3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6.zip");
        byte[] hash1 = zip.compressAndHash(firstZipPath.toAbsolutePath().toString(), dataExportFolder.toAbsolutePath().toString(), 0L, ((dir, name) -> name.endsWith(".csv")), false);
        byte[] hash2 = zip.compressAndHash(secondZipPath.toAbsolutePath().toString(), dataExportFolder.toAbsolutePath().toString(), 0L, ((dir, name) -> name.endsWith(".csv")), false);
        doReturn(dataExportFolder).when(dirProvider).getDataExportDir();

        migrator.migrate();

        assertTrue(Files.exists(firstZipPath));
        assertTrue(Files.exists(secondZipPath));
        byte[] newHash1 = zip.calculateHash(firstZipPath.toAbsolutePath().toString());
        byte[] newHash2 = zip.calculateHash(secondZipPath.toAbsolutePath().toString());
        assertFalse(Arrays.equals(newHash1, hash1));
        assertFalse(Arrays.equals(newHash2, hash2));
        assertArrayEquals(newHash1, shard1.getCoreZipHash());
        assertArrayEquals(newHash2, shard2.getCoreZipHash());
        Path extractPath = extension.newFolder().toPath();
        zip.extract(firstZipPath.toAbsolutePath().toString(), extractPath.toAbsolutePath().toString());
        assertEquals(1, Files.list(extractPath).count());
        assertTrue(Files.exists(extractPath.resolve("not-prunable.csv")));
        extractPath = extension.newFolder().toPath();
        zip.extract(secondZipPath.toAbsolutePath().toString(), extractPath.toAbsolutePath().toString());
        assertEquals(1, Files.list(extractPath).count());
        assertTrue(Files.exists(extractPath.resolve("not-prunable.csv")));

        verify(shardDao).updateShard(shard1);
        verify(shardDao).updateShard(shard2);
        verify(optionDAO).set("current-shard-for-migration", "1");
        verify(optionDAO).set("current-shard-for-migration", "2");
        verify(optionDAO).set("prunable-shard-migration-finished", "false");
    }

    @Test
    void testResumeMigration() throws IOException {
        mockPrunableTablesAndShards();
        mockChain();

        Path dataExportFolder = extension.newFolder().toPath();

        Files.createFile(dataExportFolder.resolve("not-prunable.csv"));
        Files.createFile(dataExportFolder.resolve("prunable_table.csv"));
        Path firstZipPath = dataExportFolder.resolve("apl-blockchain-shard-1-chain-3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6.zip");
        Path secondZipPath = dataExportFolder.resolve("apl-blockchain-shard-2-chain-3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6.zip");
        byte[] hash1 = zip.compressAndHash(firstZipPath.toAbsolutePath().toString(), dataExportFolder.toAbsolutePath().toString(), 0L, ((dir, name) -> name.endsWith(".csv")), false);
        byte[] hash2 = zip.compressAndHash(secondZipPath.toAbsolutePath().toString(), dataExportFolder.toAbsolutePath().toString(), 0L, ((dir, name) -> name.endsWith(".csv")), false);
        doReturn(dataExportFolder).when(dirProvider).getDataExportDir();

        doReturn("2").when(optionDAO).get("current-shard-for-migration");
        doReturn(null).when(optionDAO).get("prunable-shard-migration-finished");

        migrator.migrate();

        assertTrue(Files.exists(firstZipPath));
        assertTrue(Files.exists(secondZipPath));
        byte[] newHash1 = zip.calculateHash(firstZipPath.toAbsolutePath().toString());
        byte[] newHash2 = zip.calculateHash(secondZipPath.toAbsolutePath().toString());
        assertArrayEquals(newHash1, hash1);
        assertFalse(Arrays.equals(newHash2, hash2));
        assertArrayEquals(newHash2, shard2.getCoreZipHash());
        Path extractPath = extension.newFolder().toPath();
        zip.extract(secondZipPath.toAbsolutePath().toString(), extractPath.toAbsolutePath().toString());
        assertEquals(1, Files.list(extractPath).count());
        assertTrue(Files.exists(extractPath.resolve("not-prunable.csv")));

        verify(shardDao).updateShard(shard2);
        verify(shardDao).getAllCompletedShards();
        verifyNoMoreInteractions(shardDao);
        verify(optionDAO).set("current-shard-for-migration", "2");
        verify(optionDAO).set("prunable-shard-migration-finished", "false");
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

}