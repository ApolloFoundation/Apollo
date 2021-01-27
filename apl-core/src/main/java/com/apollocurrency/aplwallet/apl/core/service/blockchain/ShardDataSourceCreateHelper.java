/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.shard.ShardNameHelper;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Helper class for creating shard data source.
 *
 * @author yuriy.larin
 */
public class ShardDataSourceCreateHelper {
    public static final int MAX_CACHE_SIZE = 16 * 1024; // 16mb
    public static final int MAX_CONNECTIONS = 10; // change from 60 to 10 for every shard
    public static final int MAX_MEMORY_ROWS = 10_000;
    private static final Logger log = getLogger(ShardDataSourceCreateHelper.class);
    private final DatabaseManager databaseManager;
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

    private static void logStackTrace(String initString, StackTraceElement[] stackTrace) {
        StringBuilder sb = new StringBuilder(512);
        sb.append(initString).append('\n');
        for (int i = 1; i < stackTrace.length; i++) {
            String line = stackTrace[i].toString();
            sb.append("\t\t").append(line).append('\n');
        }
        log.debug(sb.toString());
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
     *
     * @return helper class
     */
    public ShardDataSourceCreateHelper createUninitializedDataSource() {
        checkGenerateShardName();
        log.debug("Create new SHARD '{}'", shardName);
        DbProperties shardDbProperties;
        shardDbProperties = databaseManager.getBaseDbProperties().deepCopy();
        shardDbProperties.setDbName(shardName); // change file name
        shardDbProperties.setMaxConnections(MAX_CONNECTIONS);
        shardDbProperties.setMaxMemoryRows(MAX_MEMORY_ROWS);
        shardDbProperties.setDbUrl(null);  // nullify dbUrl intentionally!;
        shardDbProperties.setDbIdentity(shardDbProperties.getDbName() != null ? shardDbProperties.getDbName() : DbProperties.DB_SYSTEM_NAME); // put shard related info
        shardDb = new TransactionalDataSource(shardDbProperties, databaseManager.getPropertiesHolder());

        return this;
    }

    public String checkGenerateShardName() {
        if (shardId == null) {
            try (Connection con = databaseManager.getDataSource().getConnection();
                 @DatabaseSpecificDml(DmlMarker.IFNULL_USE)
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
        UUID chainId = databaseManager.getChainId();
        shardName = new ShardNameHelper().getShardNameByShardId(shardId, chainId);
        return shardName;
    }

}
