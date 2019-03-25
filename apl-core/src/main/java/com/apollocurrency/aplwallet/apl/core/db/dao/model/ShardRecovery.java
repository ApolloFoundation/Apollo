package com.apollocurrency.aplwallet.apl.core.db.dao.model;

import java.time.Instant;

import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;

/**
 * Shard recovery state db entity
 */
public class ShardRecovery {
    private Long shardRecoveryId;
    private String state = MigrateState.INIT.name();
    private String objectName;
    private String columnName;
    private Long lastColumnValue;
    private String lastColumnStr;
    private Instant updated = Instant.now();

    public ShardRecovery() {
    }

    public ShardRecovery(Long shardRecoveryId, MigrateState state, String objectName, String columnName, Long lastColumnValue, String lastColumnStr, Instant updated) {
        this.shardRecoveryId = shardRecoveryId;
        if (state != null) {
            this.state = state.name();
        }
        this.objectName = objectName;
        this.columnName = columnName;
        this.lastColumnValue = lastColumnValue;
        this.lastColumnStr = lastColumnStr;
        this.updated = updated;
    }

    public ShardRecovery(MigrateState state, String objectName, String columnName, Long lastColumnValue, String lastColumnStr, Instant updated) {
        if (state != null) {
            this.state = state.name();
        }
        this.objectName = objectName;
        this.columnName = columnName;
        this.lastColumnValue = lastColumnValue;
        this.lastColumnStr = lastColumnStr;
        this.updated = updated;
    }

    public ShardRecovery(String objectName, String columnName, Long lastColumnValue, String lastColumnStr, Instant updated) {
        this.objectName = objectName;
        this.columnName = columnName;
        this.lastColumnValue = lastColumnValue;
        this.lastColumnStr = lastColumnStr;
        this.updated = updated;
    }

    public ShardRecovery(String objectName, String columnName, Long lastColumnValue, Instant updated) {
        this.objectName = objectName;
        this.columnName = columnName;
        this.lastColumnValue = lastColumnValue;
        this.updated = updated;
    }

    public ShardRecovery(MigrateState state) {
        if (state != null) {
            this.state = state.name();
        }
    }

    public ShardRecovery(MigrateState state, String objectName) {
        if (state != null) {
            this.state = state.name();
        }
        this.objectName = objectName;
    }

    public Long getShardRecoveryId() {
        return shardRecoveryId;
    }

    public void setShardRecoveryId(Long shardRecoveryId) {
        this.shardRecoveryId = shardRecoveryId;
    }

    public MigrateState getState() {
        if (state != null) {
            return MigrateState.valueOf(state);
        }
        return MigrateState.INIT;
    }

    public void setState(MigrateState state) {
        if (state != null) {
            this.state = state.name();
        }
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public Long getLastColumnValue() {
        return lastColumnValue;
    }

    public void setLastColumnValue(Long lastColumnValue) {
        this.lastColumnValue = lastColumnValue;
    }

    public String getLastColumnStr() {
        return lastColumnStr;
    }

    public void setLastColumnStr(String lastColumnStr) {
        this.lastColumnStr = lastColumnStr;
    }

    public Instant getUpdated() {
        return updated;
    }

    public void setUpdated(Instant updated) {
        this.updated = updated;
    }

    public static ShardRecoveryBuilder builder() {
        return new ShardRecoveryBuilder();
    }

    public static final class ShardRecoveryBuilder {
        private Long shardRecoveryId;
        private String state;
        private String objectName;
        private String columnName;
        private Long lastColumnValue;
        private String lastColumnStr;
        private Instant updated;

        private ShardRecoveryBuilder() {
        }

        public ShardRecoveryBuilder shardRecoveryId(Long shardRecoveryId) {
            this.shardRecoveryId = shardRecoveryId;
            return this;
        }

        public ShardRecoveryBuilder state(String state) {
            this.state = state;
            return this;
        }

        public ShardRecoveryBuilder objectName(String objectName) {
            this.objectName = objectName;
            return this;
        }

        public ShardRecoveryBuilder columnName(String columnName) {
            this.columnName = columnName;
            return this;
        }

        public ShardRecoveryBuilder lastColumnValue(Long lastColumnValue) {
            this.lastColumnValue = lastColumnValue;
            return this;
        }

        public ShardRecoveryBuilder lastColumnStr(String lastColumnStr) {
            this.lastColumnStr = lastColumnStr;
            return this;
        }

        public ShardRecoveryBuilder updated(Instant updated) {
            this.updated = updated;
            return this;
        }

        public ShardRecovery build() {
            return new ShardRecovery(
                    shardRecoveryId, MigrateState.valueOf(state), objectName,
                    columnName, lastColumnValue, lastColumnStr, updated);
        }
    }
}
