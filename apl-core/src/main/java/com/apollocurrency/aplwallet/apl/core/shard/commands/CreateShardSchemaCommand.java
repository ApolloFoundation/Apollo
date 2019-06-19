/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.commands;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.db.DbVersion;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.core.shard.ShardEngine;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * Command for creating initial Shard Schema in shard database/file.
 *
 * @author yuriy.larin
 */
public class CreateShardSchemaCommand implements DataMigrateOperation {
    private static final Logger log = getLogger(CreateShardSchemaCommand.class);

    private ShardEngine shardEngine;
    private DbVersion dbVersion;
    private byte[] shardHash; // shardHash can be NULL in one case
    private Long[] generatorIds;

    public CreateShardSchemaCommand(
            ShardEngine shardEngine,
            DbVersion dbVersion,
            byte[] shardHash, Long[] generatorIds) { // shardHash can be NULL
        this.shardEngine = Objects.requireNonNull(
                shardEngine, "shardEngine is NULL");
        this.dbVersion = Objects.requireNonNull(dbVersion, "dbVersion is NULL");
        this.shardHash = shardHash;
        this.generatorIds = generatorIds;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState execute() {
        log.debug("Create Shard Schema Command execute...");
        return shardEngine.addOrCreateShard(dbVersion, shardHash, generatorIds); // shardHash can be NULL or value
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("CreateShardSchemaCommand{");
        sb.append("dbVersion=").append(dbVersion);
        sb.append('}');
        return sb.toString();
    }
}
