/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PredefinedDirLocationsTest {

    @Test
    void createWithNulls() {
        PredefinedDirLocations locations = new PredefinedDirLocations();
        assertNull(locations.getDbDir());
        assertNull(locations.getLogsDir());
        assertNull(locations.getPidFilePath());
        assertNull(locations.getTwoFactorAuthDir());
        assertNull(locations.getVaultKeystoreDir());
    }

    @Test
    void createWithDirs() {
        PredefinedDirLocations locations = new PredefinedDirLocations(
                "dbpath", "logs", "vaultKeyStore", "pidDir", "twoFA", "dataExportDir", "dexStorage");
        assertNotNull(locations.getDbDir());
        assertNotNull(locations.getLogsDir());
        assertNotNull(locations.getPidFilePath());
        assertNotNull(locations.getTwoFactorAuthDir());
        assertNotNull(locations.getVaultKeystoreDir());
        assertNotNull(locations.getDataExportDir());
    }
}