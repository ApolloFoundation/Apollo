/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.shard.ShardNameHelper;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import org.slf4j.Logger;

/**
 * Helper class for creating shard data source.
 *
 * @author yuriy.larin
 */
public class ShardDataSourceCreateHelper {
    private static final Logger log = getLogger(ShardDataSourceCreateHelper.class);

    private DatabaseManager databaseManager;
    private Long shardId;
    private String shardName;
    private TransactionalDataSource shardDb;

    public ShardDataSourceCreateHelper(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public ShardDataSourceCreateHelper(DatabaseManager databaseManager, Long shardId) {
        this.databaseManager = databaseManager;
        this.shardId = shardId;
    }

    public Long getShardId() {
        return shardId;
    }

    public String getShardName() {
        return shardName;
    }

    /**
     * Created shard's data source.
     *
     * @return transactional data source
     */
    public TransactionalDataSource getShardDb() {
        return shardDb;
    }

    /**
     * Main method creation and returning later data source.
     * @return helper class
     */
    public ShardDataSourceCreateHelper createUninitializedDataSource() {
        checkGenerateShardName();
        log.debug("Create new SHARD '{}'", shardName);
        DbProperties shardDbProperties = null;
        try {
            shardDbProperties = databaseManager.getBaseDbProperties().deepCopy()
                    .dbFileName(shardName) // change file name
                    .dbUrl(null)  // nullify dbUrl intentionally!;
                    .dbIdentity(shardId); // put shard related info
        } catch (CloneNotSupportedException e) {
            log.error("DbProperties cloning error", e);
        }
        shardDb = new TransactionalDataSource(shardDbProperties, databaseManager.getPropertiesHolder());
        return this;
    }

    public String checkGenerateShardName() {
        if (shardId == null) {
            try (Connection con = databaseManager.getDataSource().getConnection();
                 PreparedStatement pstmt = con.prepareStatement("SELECT IFNULL(max(SHARD_ID), 0) as shard_id FROM shard")) {
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        shardId = rs.getLong("shard_id");
                    } else {
                        throw new IllegalStateException("Shard Id was not retrieved");
                    }
                }
            } catch (SQLException e) {
                log.error("Error retrieve shards...", e);
            }
            log.debug("Selected SHARD_ID = {} from DB", shardId);
        }
        shardName = ShardNameHelper.getShardNameByShardId(shardId);
        return shardName;
    }
}
