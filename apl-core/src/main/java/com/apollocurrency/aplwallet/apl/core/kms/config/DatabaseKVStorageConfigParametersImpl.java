package com.apollocurrency.aplwallet.apl.core.kms.config;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

import com.apollocurrency.aplwallet.apl.core.db.DbConfig;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import io.firstbridge.kms.persistence.storage.config.KVStorageConfigProvider;
import lombok.ToString;

/**
 * Class is used for local MariaDb url connection configuration.
 */
@Singleton
public class DatabaseKVStorageConfigParametersImpl implements KVStorageConfigProvider {

    /**
     *  we need DbProperties mostly to create new URL connection string to KMS db schema
     */
    private DbProperties dbProperties;
    private MariaDbConfigParameters mariaDbConfigParameters;

    @Inject
    public DatabaseKVStorageConfigParametersImpl(DbConfig dbConfig) {
        Objects.requireNonNull(dbConfig, "dbConfig is NULL");
        Objects.requireNonNull(dbConfig.getDbProperties(), "DbProperties is NULL");
        this.dbProperties = dbConfig.getDbProperties().deepCopy();
    }

    @Produces
    @Override
    public KvStoreConfigParameters getKvStoreConfigParameters() {
        if (this.mariaDbConfigParameters == null) {
            this.mariaDbConfigParameters = new MariaDbConfigParameters(this.dbProperties);
        }
        return this.mariaDbConfigParameters;
    }

    @ToString
    public static class MariaDbConfigParameters implements KvStoreConfigParameters {

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

        public MariaDbConfigParameters(DbProperties dbProperties) {
            Objects.requireNonNull(dbProperties, "dbProperties is NULL");
            this.dbUrl = dbProperties.getDbUrl();

            this.dbType = dbProperties.getDbType();
            this.dbDir = dbProperties.getDbDir();
            this.dbName = dbProperties.getDbName();
//            dbProperties.setDbName(KMS_SCHEMA_NAME); // update value
            this.dbParams = dbProperties.getDbParams();
            this.dbUsername = dbProperties.getDbUsername();
            this.dbPassword = dbProperties.getDbPassword();
            this.chainId = dbProperties.getChainId();
            this.maxConnections = dbProperties.getMaxConnections();
            this.loginTimeout = dbProperties.getLoginTimeout();
            this.defaultLockTimeout = dbProperties.getDefaultLockTimeout();
            this.maxMemoryRows = dbProperties.getMaxMemoryRows();
//            this.dbIdentity = KMS_SCHEMA_NAME;

            this.databaseHost = dbProperties.getDatabaseHost();
            this.databasePort = dbProperties.getDatabasePort();
            this.dbUrl = dbProperties.formatJdbcUrlString(true);
        }

        @Override
        public String getConnectionUrl() {
            return this.dbUrl;
        }

        @Override
        public String getDatabase() {
            return this.dbName;
        }

        @Override
        public String getHostname() {
            return this.databaseHost;
        }

        @Override
        public int getPort() {
            return this.databasePort;
        }

        @Override
        public String getUser() {
            return this.dbUsername;
        }

        @Override
        public String getPassword() {
            return this.dbPassword;
        }

        @Override
        public String getStoragePath() {
            return null;
        }

        @Override
        public Properties getConfigProperties() {
            return null;
        }

        @Override
        public void setConfigProperties(Properties properties) {

        }

        @Override
        public void setDatabase(String databaseName) {
            Objects.requireNonNull(databaseName, "databaseName is NULL");
            this.dbName = databaseName;
            this.dbUrl = formatJdbcUrlString();
        }

        public String formatJdbcUrlString() {
            String finalDbUrl;
            String fullUrlString = "jdbc:%s://%s:%d/%s?user=%s&password=%s%s";
            String passwordlessUrlString = "jdbc:%s://%s:%d/%s?user=%s%s"; // skip password for 'password less mode' (in docker container)
            String tempDbName = this.dbName;
            if (this.dbPassword != null && !this.dbPassword.isEmpty()) {
                finalDbUrl = String.format(
                    fullUrlString,
                    this.dbType,
                    this.databaseHost,
                    this.databasePort,
                    tempDbName,
                    this.dbUsername != null ? this.dbUsername : "",
                    this.dbPassword,
                    this.dbParams != null ? this.dbParams : ""
                );
            } else {
                // skip password for 'password less mode' (in docker container)
                finalDbUrl = String.format(
                    passwordlessUrlString,
                    this.dbType,
                    this.databaseHost,
                    this.databasePort,
                    tempDbName,
                    this.dbUsername != null ? this.dbUsername : "",
                    this.dbParams != null ? this.dbParams : ""
                );
            }
            return finalDbUrl;
        }
    }
}
