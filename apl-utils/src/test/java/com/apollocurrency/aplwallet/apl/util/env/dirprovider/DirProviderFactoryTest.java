/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DirProviderFactoryTest {
    @BeforeEach
    public void setUp() {
        RuntimeEnvironment.getInstance().setMain(this.getClass());
    }

    @Test
    void getInstance() {
        assertThrows(NullPointerException.class, () -> {
            DirProviderFactory.setup(true, null, null, null);
            DirProvider df = DirProviderFactory.getProvider();
        });

        assertThrows(NullPointerException.class, () -> {
            DirProviderFactory.setup(true, UUID.randomUUID(), null, null);
            DirProvider df = DirProviderFactory.getProvider();
        });
        DirProviderFactory.setup(true, UUID.randomUUID(), "Default", null);
        DirProvider df = DirProviderFactory.getProvider();
        assertNotNull(df.getAppBaseDir());
        assertNotNull(df.getLogsDir());
        assertNotNull(df.getPIDFile());
        assertNotNull(df.getDbDir());
        assertNotNull(df.getVaultKeystoreDir());
    }

    @Test
    void getInstance2() {
        PredefinedDirLocations dirLocations = new PredefinedDirLocations();
        DirProviderFactory.setup(true, UUID.randomUUID(), "Default", dirLocations);
        DirProvider df = DirProviderFactory.getProvider();
        assertNotNull(df.getAppBaseDir());
        assertNotNull(df.getLogsDir());
        assertNotNull(df.getPIDFile());
        assertNotNull(df.getDbDir());
        assertNotNull(df.getVaultKeystoreDir());

        dirLocations = new PredefinedDirLocations("dbDir", "logDir", "vaultDir", "pidDir", "twoFADir", "dataExportDir", "dexStorage");
        DirProviderFactory.setup(true, UUID.randomUUID(), "Default", dirLocations);
        df =  DirProviderFactory.getProvider();
        assertNotNull(df.getAppBaseDir());
        assertNotNull(df.getLogsDir());
        assertEquals("logDir", df.getLogsDir().toFile().getName());
        assertNotNull(df.getPIDFile());
        assertEquals("pidDir", df.getPIDFile().toFile().getName());
        assertNotNull(df.getDbDir());
        assertEquals("dbDir", df.getDbDir().toFile().getName());
        assertNotNull(df.getVaultKeystoreDir());
        assertEquals("vaultDir", df.getVaultKeystoreDir().toFile().getName());
        assertNotNull(df.getDataExportDir());
        assertEquals("dataExportDir", df.getDataExportDir().toFile().getName());
    }
}