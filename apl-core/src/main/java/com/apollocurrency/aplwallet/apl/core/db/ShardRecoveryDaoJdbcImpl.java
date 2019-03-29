/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardRecovery;
import org.slf4j.Logger;

/**
 * Shard Recovery information management + retrieving interface
 */
@Singleton
public class ShardRecoveryDaoJdbcImpl implements ShardRecoveryDaoJdbc {
    private static final Logger log = getLogger(ShardRecoveryDaoJdbcImpl.class);

//    private DatabaseManager databaseManager;
    private ShardRecoveryJdbcMapper rowMapper = new ShardRecoveryJdbcMapper();

    class ShardRecoveryJdbcMapper {

        public ShardRecovery map(ResultSet rs) throws SQLException {
            ShardRecovery recovery = null;
//            if (rs.next()) {
                recovery = ShardRecovery.builder()
                        .shardRecoveryId(rs.getLong("shard_recovery_id"))
                        .state(rs.getString("state"))
                        .objectName(rs.getString("object_name"))
                        .columnName(rs.getString("column_name"))
                        .lastColumnValue(rs.getLong("last_column_value"))
                        .processedObject(rs.getString("processed_object"))
                        .updated(Instant.ofEpochMilli(rs.getDate("updated").getTime()) )
                        .build();
//            }
            return recovery;
        }
    }

//    @Inject
    public ShardRecoveryDaoJdbcImpl(/*DatabaseManager databaseManager*/) {
//        this.databaseManager = databaseManager;
    }

    public ShardRecovery getShardRecoveryById(Connection con, long shardRecoveryId) {
        Objects.requireNonNull(con,"connection is NULL");
        try ( PreparedStatement pstmt = con.prepareStatement("SELECT * FROM shard_recovery where shard_recovery_id=?")) {
            pstmt.setLong(1, shardRecoveryId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return getIfPresent(rs);
            }
        } catch (SQLException e) {
            log.error("getLatest recovery error", e);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public ShardRecovery getLatestShardRecovery(TransactionalDataSource sourceDataSource) {
        Objects.requireNonNull(sourceDataSource,"sourceDataSource is NULL");
        try ( Connection con = sourceDataSource.getConnection()) {
            return this.getLatestShardRecovery(con);
        } catch (SQLException e) {
            log.error("getLatest recovery error", e);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public ShardRecovery getLatestShardRecovery(Connection con) {
        Objects.requireNonNull(con,"connection is NULL");
        try ( PreparedStatement pstmt = con.prepareStatement("SELECT * FROM shard_recovery limit 1")) {
            try (ResultSet rs = pstmt.executeQuery()) {
                return getIfPresent(rs);
            }
        } catch (SQLException e) {
            log.error("getLatest recovery error", e);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public List<ShardRecovery> getAllShardRecovery(Connection con) {
        Objects.requireNonNull(con,"connection is NULL");
        List<ShardRecovery> result = new ArrayList<>();
        try (PreparedStatement pstmt = con.prepareStatement("SELECT * FROM shard_recovery")) {
            try (ResultSet rs = pstmt.executeQuery()) {
                ShardRecovery recovery = null;
                while ((recovery =getIfPresent(rs)) != null) {
                    result.add(recovery);
                }
                return result;
            }
        } catch (SQLException e) {
            log.error("get All recovery error", e);
            throw new RuntimeException(e.toString(), e);
        }
    }

    private ShardRecovery getIfPresent(ResultSet rs) throws SQLException {
        ShardRecovery shardRecovery = null;
        if (rs.next()) {
            shardRecovery = rowMapper.map(rs);
        }
        return shardRecovery;
    }

    public long countShardRecovery(Connection con) {
        Objects.requireNonNull(con,"connection is NULL");
        Long recoveryCount = null;
        try (PreparedStatement pstmt = con.prepareStatement("SELECT count(*) FROM shard_recovery")) {
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    recoveryCount = rs.getLong(1);
                }
                return recoveryCount;
            }
        } catch (SQLException e) {
            log.error("get count recovery error", e);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public long saveShardRecovery(TransactionalDataSource sourceDataSource, ShardRecovery recovery) {
        Objects.requireNonNull(sourceDataSource,"source data source is NULL");
        Objects.requireNonNull(recovery,"recovery is NULL");
        try ( Connection con = sourceDataSource.getConnection()) {
            return this.saveShardRecovery(con, recovery);
        } catch (SQLException e) {
            log.error("Save recovery error", e);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public long saveShardRecovery(Connection con, ShardRecovery recovery) {
        Objects.requireNonNull(con,"connection is NULL");
        Objects.requireNonNull(recovery,"recovery is NULL");
        Objects.requireNonNull(recovery.getState(),"recovery State is NULL"); // NULL is not permitted !
        try (PreparedStatement pstmt = con.prepareStatement(
                "INSERT INTO shard_recovery(" +
                        "state, object_name, column_name, last_column_value, processed_object, updated) " +
                        "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP())"
        )) {
            int i = 0;
            pstmt.setString(++i, recovery.getState().name()); // recovery.getState() SHOULD NEVER be NULL, field restriction
            pstmt.setString(++i, recovery.getObjectName());
            pstmt.setString(++i, recovery.getColumnName());
            if (recovery.getLastColumnValue() != null) {
                pstmt.setLong(++i, recovery.getLastColumnValue());
            } else {
                pstmt.setNull(++i, Types.BIGINT);
            }
            pstmt.setString(++i, recovery.getProcessedObject());
            int inserted = pstmt.executeUpdate();
            log.trace("recovery inserted = {}", inserted);
            if(inserted > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            log.error("save recovery error", e);
            throw new RuntimeException(e.toString(), e);
        }
        return -1;
    }

    public int updateShardRecovery(TransactionalDataSource sourceDataSource, ShardRecovery recovery) {
        Objects.requireNonNull(sourceDataSource,"source data source is NULL");
        Objects.requireNonNull(recovery,"recovery is NULL");
        Objects.requireNonNull(recovery.getState(),"recovery State is NULL"); // NULL is not permitted !
        try ( Connection con = sourceDataSource.getConnection()) {
            return this.updateShardRecovery(con, recovery);
        } catch (SQLException e) {
            log.error("Update recovery error", e);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public int updateShardRecovery(Connection con, ShardRecovery recovery) {
        Objects.requireNonNull(con,"connection is NULL");
        Objects.requireNonNull(recovery,"recovery is NULL");
        Objects.requireNonNull(recovery.getState(),"recovery State is NULL"); // NULL is not permitted !
        int updated = -1;
        try (PreparedStatement pstmt = con.prepareStatement(
                "UPDATE shard_recovery SET state=?, object_name=?, column_name=?, " +
                        "last_column_value=?, processed_object=?, updated=CURRENT_TIMESTAMP() " +
                        "where shard_recovery_id=?")
        ) {
            int i = 0;
            pstmt.setString(++i, recovery.getState().name());
            pstmt.setString(++i, recovery.getObjectName());
            pstmt.setString(++i, recovery.getColumnName());
            if (recovery.getLastColumnValue() != null) {
                pstmt.setLong(++i, recovery.getLastColumnValue());
            } else {
                pstmt.setNull(++i, Types.BIGINT);
            }
            pstmt.setString(++i, recovery.getProcessedObject());
            pstmt.setLong(++i, recovery.getShardRecoveryId());
            updated = pstmt.executeUpdate();
            log.trace("recovery updated = {}", updated);
        } catch (SQLException e) {
            log.error("Save recovery error", e);
            throw new RuntimeException(e.toString(), e);
        }
        return updated;
    }

    public int hardDeleteShardRecovery(Connection con, long shardRecoveryId) {
        Objects.requireNonNull(con,"connection is NULL");        int deleted = -1;
        try (PreparedStatement pstmt = con.prepareStatement(
                "DELETE FROM shard_recovery where shard_recovery_id=?")
        ) {
            int i = 0;
            pstmt.setLong(++i, shardRecoveryId);
            deleted = pstmt.executeUpdate();
            log.trace("recovery deleted = {}", deleted);
        } catch (SQLException e) {
            log.error("Delete recovery error", e);
            throw new RuntimeException(e.toString(), e);
        }
        return deleted;
    }

    public int hardDeleteAllShardRecovery(Connection con) {
        Objects.requireNonNull(con,"connection is NULL");
        int deleted = -1;
        try (PreparedStatement pstmt = con.prepareStatement(
                "DELETE FROM shard_recovery")
        ) {
            deleted = pstmt.executeUpdate();
            log.trace("All recovery deleted = {}", deleted);
        } catch (SQLException e) {
            log.error("Delete All recovery error", e);
            throw new RuntimeException(e.toString(), e);
        }
        return deleted;
    }

}
