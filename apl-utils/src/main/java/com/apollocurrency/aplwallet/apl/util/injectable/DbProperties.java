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
}
