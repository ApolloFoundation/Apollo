/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.commands;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.shard.ShardEngine;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import org.slf4j.Logger;

/**
 * Command for creating db backup before sharding database.
 *
 * @author yuriy.larin
 */
public class BackupDbBeforeShardCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(BackupDbBeforeShardCommand.class);

    private ShardEngine shardEngine;

   public BackupDbBeforeShardCommand(ShardEngine shardEngine) {
        this.shardEngine = Objects.requireNonNull(
                shardEngine, "shardEngine is NULL");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState execute() {
        log.debug("Create DB BACKUP Command execute...");
        return shardEngine.createBackup();
    }

    @Override
    public String toString() {
        return "BackupDbBeforeShardCommand";
    }
}
