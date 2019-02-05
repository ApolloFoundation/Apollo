/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.injectable;

import javax.enterprise.inject.Vetoed;

@Vetoed
public final class DbProperties {

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
