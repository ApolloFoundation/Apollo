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
    public static final String DB_EXTENSION = "mv.db";
    public static final String DB_EXTENSION_WITH_DOT = "." + DbProperties.DB_EXTENSION;

    private String dbUrl;
    private String dbType;
    private String dbDir;
    private String dbFileName;
    private String dbParams;
    private String dbUsername;
    private String dbPassword;
    private UUID chainId;
    private int maxConnections;
    private int loginTimeout;
    private int defaultLockTimeout;
    private int maxMemoryRows;
    private Long dbIdentity = null;

    private String databaseName;
    private String databaseHost;
    private Integer databasePort;


    public Optional<Long> getDbIdentity() {
        return Optional.ofNullable(dbIdentity);
    }


    public DbProperties dbIdentity(long shardIdOrTempId) {
        if (shardIdOrTempId == 0) {
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
