/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.Async;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import com.apollocurrency.aplwallet.apl.core.db.derived.PrunableDbTable;
import com.apollocurrency.aplwallet.apl.core.files.FileChangedEvent;
import com.apollocurrency.aplwallet.apl.core.shard.helper.CsvExporterImpl;
import com.apollocurrency.aplwallet.apl.core.shard.observer.TrimData;
import com.apollocurrency.aplwallet.apl.util.ChunkedFileOps;
import com.apollocurrency.aplwallet.apl.util.FileUtils;
import com.apollocurrency.aplwallet.apl.util.Zip;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class ShardPrunableZipHashCalculator {
    private final DerivedTablesRegistry registry;
    private final Zip zip;
    private final DatabaseManager databaseManager;
    private final ShardDao shardDao;
    private final BlockchainConfig blockchainConfig;
    private final DirProvider dirProvider;
    private int lastPruningTime = 0;
    private final Event<ChunkedFileOps> fileChangedEvent;
    
    @Inject
    public ShardPrunableZipHashCalculator(Event<ChunkedFileOps> fileChangedEvent, DerivedTablesRegistry registry, Zip zip, DatabaseManager databaseManager, ShardDao shardDao, BlockchainConfig blockchainConfig, DirProvider dirProvider) {
        this.registry = registry;
        this.zip = zip;
        this.databaseManager = databaseManager;
        this.shardDao = shardDao;
        this.blockchainConfig = blockchainConfig;
        this.dirProvider = dirProvider;
        this.fileChangedEvent = fileChangedEvent;
    }

    public void onTrimDone(@Observes @Async TrimData trimData) {
        tryRecalculatePrunableArchiveHashes(trimData.getPruningTime());
    }

    public void tryRecalculatePrunableArchiveHashes(int time) {
        if (time > lastPruningTime) {
            log.debug("Recalculate prunable archive hashes at {}", time);
            lastPruningTime = time;
            recalculatePrunableArchiveHashes();
        }
    }

    public int getLastPruningTime() {
        return lastPruningTime;
    }

    private void recalculatePrunableArchiveHashes() {
        UUID chainId = blockchainConfig.getChain().getChainId();
        List<Shard> allCompletedShards = shardDao.getAllCompletedShards().stream().filter(shard -> shard.getPrunableZipHash() != null).collect(Collectors.toList()); // TODO change to completed and imported
        allCompletedShards.forEach(shard -> {
            try {
                Path tempDirectory = Files.createTempDirectory("shard-" + shard.getShardId());
                CsvExporterImpl csvExporter = new CsvExporterImpl(databaseManager, tempDirectory); // create new instance of CsvExporter for each directory
                List<PrunableDbTable> prunableTables = registry.getDerivedTables().stream().filter(t -> t instanceof PrunableDbTable).map(t -> (PrunableDbTable) t).collect(Collectors.toList());
                prunableTables.forEach(t -> csvExporter.exportPrunableDerivedTable(t, shard.getShardHeight(), lastPruningTime, 100));
                long count = Files.list(tempDirectory).count();
                ShardNameHelper shardNameHelper = new ShardNameHelper();
                String prunableArchiveName = shardNameHelper.getPrunableShardArchiveNameByShardId(shard.getShardId(), chainId );
                Path prunableArchivePath = dirProvider.getDataExportDir().resolve(prunableArchiveName);
                if (count == 0) {
                    shard.setPrunableZipHash(null);
                    shardDao.updateShard(shard);
                    Files.deleteIfExists(prunableArchivePath);
                } else {
                    String zipName = "shard-" + shard.getShardId() + ".zip";
                    ChunkedFileOps ops = zip.compressAndHash(tempDirectory.resolve(zipName).toAbsolutePath().toString(), tempDirectory.toAbsolutePath().toString(), 0L, null, false);
                    fileChangedEvent.select(new AnnotationLiteral<FileChangedEvent>(){}).fireAsync(ops);        
                    log.debug("Firing 'FILE_CHANDED' event {}", ops.getFileId());
                    byte[] hash = ops.getFileHash();
                    ops.setFileId(shardNameHelper.getFullShardId(shard.getShardId(), chainId));
                    Files.move(tempDirectory.resolve(zipName), prunableArchivePath, StandardCopyOption.REPLACE_EXISTING);
                    shard.setPrunableZipHash(hash);
                    shardDao.updateShard(shard);
                }
                FileUtils.clearDirectorySilently(tempDirectory); // clean is not mandatory, but desirable
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
