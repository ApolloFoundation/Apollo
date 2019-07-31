package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.db.derived.PrunableDbTable;
import com.apollocurrency.aplwallet.apl.core.db.model.OptionDAO;
import com.apollocurrency.aplwallet.apl.util.Zip;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PrunableArchiveMigrator {
    private static final String MIGRATE_OPTION = "prunable-shard-migration-finished";
    private static final String CURRENT_SHARD_OPTION = "current-shard-for-migration";
    private ShardDao shardDao;
    private OptionDAO optionDAO;
    private DirProvider dirProvider;
    private BlockchainConfig blockchainConfig;
    private Zip zip;
    private DerivedTablesRegistry registry;

    public void migrate() {
        String migrateOption = optionDAO.get(MIGRATE_OPTION);
        if (migrateOption == null) {
            String currentShard = optionDAO.get(CURRENT_SHARD_OPTION);
            long shardIdForMigration = 1;
            if (currentShard != null) {
                shardIdForMigration = Long.parseLong(currentShard);
            }
            long finalShardIdForMigration = shardIdForMigration;
            List<Shard> shardsForMigration = shardDao.getAllCompletedShards().stream().filter(shard -> shard.getShardId() >= finalShardIdForMigration).sorted(Comparator.comparing(Shard::getShardId)).collect(Collectors.toList());
            List<String> prunableTables = registry.getDerivedTables().stream().filter(t -> t instanceof PrunableDbTable).map(DerivedTableInterface::getName).collect(Collectors.toList());
            for (Shard shard : shardsForMigration) {
                try {
                    optionDAO.set(CURRENT_SHARD_OPTION, String.valueOf(shard.getShardId()));
                    Path tempDirectory = Files.createTempDirectory("prunable-shard-migration-" + shard.getShardId());
                    ShardNameHelper shardNameHelper = new ShardNameHelper();
                    Path shardArchivePath = dirProvider.getDataExportDir().resolve(shardNameHelper.getCoreShardArchiveNameByShardId(shard.getShardId(), blockchainConfig.getChain().getChainId()));
                    String tempDirectoryString = tempDirectory.toAbsolutePath().toString();
                    zip.extract(shardArchivePath.toAbsolutePath().toString(), tempDirectoryString);
                    byte[] hash = zip.compressAndHash("shard-" + shard.getShardId() + ".zip", tempDirectoryString, 0L, (dir, name) -> prunableTables.contains(name.substring(0, name.indexOf(".csv"))), false);
                    shard.setCoreZipHash(hash);
                    shardDao.updateShard(shard);

                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
            optionDAO.set(MIGRATE_OPTION, "false");
        }
    }
}
