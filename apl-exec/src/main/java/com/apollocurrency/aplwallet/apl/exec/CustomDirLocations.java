/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exec;

import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

public class CustomDirLocations {
    public static final String DB_DIR_PROPERTY_NAME = "apl.customDbDir";
    public static final String KEYSTORE_DIR_PROPERTY_NAME = "apl.customVaultKeystoreDir";
    private String dbDir;
    private String keystoreDir;

    public CustomDirLocations(String dbDir, String keystoreDir) {
        this.dbDir = dbDir;
        this.keystoreDir = keystoreDir;
    }

    public CustomDirLocations(Properties properties) {
        Objects.requireNonNull(properties, "Properties should not be null");
        this.dbDir = properties.getProperty(DB_DIR_PROPERTY_NAME);
        this.keystoreDir = properties.getProperty(KEYSTORE_DIR_PROPERTY_NAME);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomDirLocations)) return false;
        CustomDirLocations that = (CustomDirLocations) o;
        return Objects.equals(dbDir, that.dbDir) &&
            Objects.equals(keystoreDir, that.keystoreDir);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dbDir, keystoreDir);
    }

    public Optional<String> getDbDir() {
        return Optional.ofNullable(dbDir);
    }

    public void setDbDir(String dbDir) {
        this.dbDir = dbDir;
    }

    public Optional<String> getKeystoreDir() {
        return Optional.ofNullable(keystoreDir);
    }

    public void setKeystoreDir(String keystoreDir) {
        this.keystoreDir = keystoreDir;
    }
}
