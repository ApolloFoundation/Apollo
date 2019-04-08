/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.injectable;

import javax.enterprise.inject.Vetoed;
import java.util.Optional;

@Vetoed
public final class DbProperties implements Cloneable {

    private long maxCacheSize;
    private String dbUrl;
    private String dbType;
    private String dbDir;
    private String dbFileName;
    private String dbParams;
    private String dbUsername;
    private String dbPassword;
    private int maxConnections;
    private int loginTimeout;
    private int defaultLockTimeout;
    private int maxMemoryRows;
    private Optional<Long> dbIdentity = Optional.empty();

    public long getMaxCacheSize() {
        return maxCacheSize;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public String getDbType() {
        return dbType;
    }

    public String getDbDir() {
        return dbDir;
    }

    public String getDbFileName() {
        return dbFileName;
    }

    public String getDbParams() {
        return dbParams;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public int getLoginTimeout() {
        return loginTimeout;
    }

    public int getDefaultLockTimeout() {
        return defaultLockTimeout;
    }

    public int getMaxMemoryRows() {
        return maxMemoryRows;
    }

    public Optional<Long> getDbIdentity() {
        return dbIdentity;
    }

    public DbProperties maxCacheSize(int maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
        return this;
    }

    public DbProperties dbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
        return this;
    }

    public DbProperties dbFileName(String dbFileName) {
        this.dbFileName = dbFileName;
        return this;
    }

    public DbProperties dbType(String dbType) {
        this.dbType = dbType;
        return this;
    }

    public DbProperties dbDir(String dbDir) {
        this.dbDir = dbDir;
        return this;
    }

    public DbProperties dbParams(String dbParams) {
        this.dbParams = dbParams;
        return this;
    }

    public DbProperties dbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
        return this;
    }

    public DbProperties dbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
        return this;
    }

    public DbProperties maxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
        return this;
    }

    public DbProperties loginTimeout(int loginTimeout) {
        this.loginTimeout = loginTimeout;
        return this;
    }

    public DbProperties defaultLockTimeout(int defaultLockTimeout) {
        this.defaultLockTimeout = defaultLockTimeout;
        return this;
    }

    public DbProperties maxMemoryRows(int maxMemoryRows) {
        this.maxMemoryRows = maxMemoryRows;
        return this;
    }

    public DbProperties dbIdentity(long shardIdOrTempId) {
        if (shardIdOrTempId == 0) {
            return this;
        }
        this.dbIdentity = Optional.of(shardIdOrTempId);
        return this;
    }

    public DbProperties deepCopy() throws CloneNotSupportedException {
        DbProperties clonedObj = (DbProperties) super.clone();
        clonedObj.maxCacheSize = this.maxCacheSize;
        if (this.dbUrl != null) {
            clonedObj.dbUrl = new String(this.dbUrl);
        }
        if (this.dbType != null) {
            clonedObj.dbType = new String(this.dbType);
        }
        if (this.dbDir != null) {
            clonedObj.dbDir = new String(this.dbDir);
        }
        if (this.dbFileName != null) {
            clonedObj.dbFileName = new String(this.dbFileName);
        }
        if (this.dbParams != null) {
            clonedObj.dbParams = new String(this.dbParams);
        }
        if (this.dbUsername != null) {
            clonedObj.dbUsername = new String(this.dbUsername);
        }
        if (this.dbPassword != null) {
            clonedObj.dbPassword = new String(this.dbPassword);
        }
        clonedObj.maxConnections = this.maxConnections;
        clonedObj.loginTimeout = this.loginTimeout;
        clonedObj.defaultLockTimeout = this.defaultLockTimeout;
        clonedObj.maxMemoryRows = this.maxMemoryRows;
        clonedObj.dbIdentity = Optional.empty();
        return clonedObj;
    }

    @Override
    public String toString() {
        return "DbProperties{" +
                "maxCacheSize=" + maxCacheSize +
                ", dbUrl='" + dbUrl + '\'' +
                ", dbType='" + dbType + '\'' +
                ", dbDir='" + dbDir + '\'' +
                ", dbParams='" + dbParams + '\'' +
                ", dbUsername='" + dbUsername + '\'' +
                ", dbPassword='" + dbPassword + '\'' +
                ", maxConnections=" + maxConnections +
                ", loginTimeout=" + loginTimeout +
                ", defaultLockTimeout=" + defaultLockTimeout +
                ", maxMemoryRows=" + maxMemoryRows +
                '}';
    }
}
