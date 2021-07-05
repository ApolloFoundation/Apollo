/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.injectable;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import javax.enterprise.inject.Vetoed;
import java.util.Optional;
import java.util.UUID;

@Vetoed
@ToString
@Builder
@Data
public class DbProperties implements Cloneable {
    private static final String fullUrlString = "jdbc:%s://%s:%d/%s?user=%s&password=%s%s";
    private static final String passwordlessUrlString = "jdbc:%s://%s:%d/%s?user=%s%s"; // skip password for 'password less mode' (in docker container)
    //TODO APL-1714
    @Deprecated
    public static final String DB_EXTENSION = "mv.db";
    @Deprecated
    public static final String DB_EXTENSION_WITH_DOT = "." + DbProperties.DB_EXTENSION;
    public static final String DB_SYSTEM_NAME = "mysql";

    private String dbUrl;
    private String dbType;
    private String dbDir;
    private String dbName;
    private String dbParams;
    private String dbUsername;
    private String dbPassword;
    private UUID chainId;
    private int maxConnections;
    private int loginTimeout;
    private int defaultLockTimeout;
    private int maxMemoryRows;
    private String dbIdentity;

    private String databaseHost;
    private Integer databasePort;
    private String systemDbUrl;
    private ThresholdData thresholdData;

    public Optional<String> getDbIdentity() {
        return Optional.ofNullable(dbIdentity);
    }


    public DbProperties dbIdentity(String shardIdOrTempId) {
        if (shardIdOrTempId == null || shardIdOrTempId.isEmpty()) {
            return this;
        }
        this.dbIdentity = shardIdOrTempId;
        return this;
    }

    public DbProperties deepCopy() {
        try {
            return (DbProperties) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public String formatJdbcUrlString(boolean isSystemDb) {
        String finalDbUrl;
        String fullUrlString = "jdbc:%s://%s:%d/%s?user=%s&password=%s%s";
        String passwordlessUrlString = "jdbc:%s://%s:%d/%s?user=%s%s"; // skip password for 'password less mode' (in docker container)
        String tempDbName = getDbName();
        if (isSystemDb) {
            tempDbName = "testdb".equalsIgnoreCase(getDbName()) ? getDbName() : DbProperties.DB_SYSTEM_NAME;
        }
        if (getDbPassword() != null && !getDbPassword().isEmpty()) {
            finalDbUrl = String.format(
                fullUrlString,
                getDbType(),
                getDatabaseHost(),
                getDatabasePort(),
                tempDbName,
                getDbUsername() != null ? getDbUsername() : "",
                getDbPassword(),
                getDbParams() != null ? getDbParams() : ""
            );
        } else {
            // skip password for 'password less mode' (in docker container)
            finalDbUrl = String.format(
                passwordlessUrlString,
                getDbType(),
                getDatabaseHost(),
                getDatabasePort(),
                tempDbName,
                getDbUsername() != null ? getDbUsername() : "",
                getDbParams() != null ? getDbParams() : ""
            );
        }
        return finalDbUrl;
    }


    public String formatEmbeddedJdbcUrlString() {
        String finalDbUrl;
        String embeddedUrlTemplate = "jdbc:%s:%s/%s;%s";
        finalDbUrl = String.format(
            embeddedUrlTemplate,
            getDbType(),
            getDbDir(),
            getDbName(),
            getDbParams() != null ? getDbParams() : ""
        );
        return finalDbUrl;
    }

    @Data
    @Builder
    public static class ThresholdData {
        int stmtThreshold;
        int txThreshold;
        int txInterval;
        boolean enableSqlLogs;
    }
}
