package com.apollocurrency.aplwallet.apl.core.db;

import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import org.slf4j.Logger;

class ShardDataSourceCreateHelper {
    private static final Logger log = getLogger(ShardDataSourceCreateHelper.class);

    private DatabaseManager databaseManager;
    private Long shardId;
    private String shardName;
    private TransactionalDataSource shardDb;

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

    public TransactionalDataSource getShardDb() {
        return shardDb;
    }

    public ShardDataSourceCreateHelper createUnitializedDataSource() {
        if (shardId == null) {
            try (Connection con = databaseManager.getDataSource().getConnection();
                 PreparedStatement pstmt = con.prepareStatement("SELECT IFNULL(max(SHARD_ID) + 1, 1) as shard_id FROM shard")) {
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        shardId = rs.getLong("shard_id");
                    }
                }
            } catch (SQLException e) {
                log.error("Error retrieve shards...", e);
            }
            log.debug("Selected SHARD_ID = {} from DB", shardId);
        }
        shardName = ShardNameHelper.getShardNameByShardId(shardId);
        log.debug("Create new SHARD '{}'", shardName);
        DbProperties shardDbProperties = null;
        try {
            shardDbProperties = databaseManager.getBaseDbProperties().deepCopy().dbFileName(shardName).dbUrl(null); // nullify dbUrl intentionally!;
        } catch (CloneNotSupportedException e) {
            log.error("DbProperties cloning error", e);
        }
        shardDb = new TransactionalDataSource(shardDbProperties, databaseManager.getPropertiesHolder());
        return this;
    }
}
