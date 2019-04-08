/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.dao.model;

import java.time.Instant;

import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;

/**
 * Shard recovery state db entity
 *
 * @author yuriy.larin
 */
public class ShardRecovery {
    private Long shardRecoveryId; // auto incremented id
    /**
     * tracked sharding state
     */
    private String state = MigrateState.INIT.name();
    /**
     * current object/table being processed
     */
    private String objectName;
    /**
     * 'key column name' is used for select/pagination
     */
    private String columnName;
    /**
     * latest 'key column value' stored in previous loop
     */
    private Long lastColumnValue;
    /**
     * list of objects/tables which were processed previously within one step
     */
    private String processedObject;
    /**
     * automatically updated date-time in UTC zone
     */
    private Instant updated = Instant.now();

    public ShardRecovery() {
    }

    public ShardRecovery(Long shardRecoveryId, MigrateState state, String objectName, String columnName, Long lastColumnValue, String processedObject, Instant updated) {
        this.shardRecoveryId = shardRecoveryId;
        if (state != null) {
            this.state = state.name();
        }
        this.objectName = objectName;
        this.columnName = columnName;
        this.lastColumnValue = lastColumnValue;
        this.processedObject = processedObject;
        this.updated = updated;
    }

    public ShardRecovery(MigrateState state, String objectName, String columnName, Long lastColumnValue, String processedObject) {
        if (state != null) {
            this.state = state.name();
        }
        this.objectName = objectName;
        this.columnName = columnName;
        this.lastColumnValue = lastColumnValue;
        this.processedObject = processedObject;
    }

    public ShardRecovery(String objectName, String columnName, Long lastColumnValue, String processedObject, Instant updated) {
        this.objectName = objectName;
        this.columnName = columnName;
        this.lastColumnValue = lastColumnValue;
        this.processedObject = processedObject;
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

    public String getProcessedObject() {
        return processedObject;
    }

    public void setProcessedObject(String processedObject) {
        this.processedObject = processedObject;
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
        private String processedObject;
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

        public ShardRecoveryBuilder processedObject(String processedObjectName) {
            this.processedObject = processedObjectName;
            return this;
        }

        public ShardRecoveryBuilder updated(Instant updated) {
            this.updated = updated;
            return this;
        }

        public ShardRecovery build() {
            return new ShardRecovery(
                    shardRecoveryId, MigrateState.valueOf(state), objectName,
                    columnName, lastColumnValue, processedObject, updated);
        }
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ShardRecovery{");
        sb.append("shardRecoveryId=").append(shardRecoveryId);
        sb.append(", state='").append(state).append('\'');
        sb.append(", objectName='").append(objectName).append('\'');
        sb.append(", columnName='").append(columnName).append('\'');
        sb.append(", lastColumnValue=").append(lastColumnValue);
        sb.append(", processedObject='").append(processedObject).append('\'');
        sb.append(", updated=").append(updated);
        sb.append('}');
        return sb.toString();
    }
}
