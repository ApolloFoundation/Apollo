/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;


public class DirProviderTest {

    public static final String APPLICATION_DIR = ".test";
    public static final String APPLICATION_NAME = "test";
    public static final Path APPLICATION_HOME = Paths.get(System.getProperty("user.home"), APPLICATION_DIR);
    public static final Path APPLICATION_INSTALLATION_DIR =
            DirProvider.getBinDir();

    public static final UUID CHAIN_ID = UUID.fromString("0c60b6ac-1b11-11e9-8d99-7773284f33f3");
    public static final String SHORTENED_CHAIN_ID = CHAIN_ID.toString().substring(0, 6);
    public static final Path USER_MODE_DB_DIR = APPLICATION_HOME.resolve(APPLICATION_NAME + "-db").resolve(SHORTENED_CHAIN_ID);
    public static final Path USER_MODE_KEYSTORE_DIR = APPLICATION_HOME.resolve(APPLICATION_NAME + "-vault-keystore").resolve(SHORTENED_CHAIN_ID);
    public static final Path USER_MODE_2FA_DIR = USER_MODE_KEYSTORE_DIR.resolve(APPLICATION_NAME + "-2fa");
    public static final Path USER_MODE_LOGS_DIR = APPLICATION_HOME.resolve(APPLICATION_NAME + "-logs");
    public static final Path USER_MODE_PID_FILE = APPLICATION_HOME.resolve(APPLICATION_NAME + "-" + SHORTENED_CHAIN_ID + ".pid");

    public static final Path SERVICE_MODE_DB_DIR = APPLICATION_INSTALLATION_DIR.resolve(APPLICATION_NAME + "-db").resolve(SHORTENED_CHAIN_ID);
    public static final Path SERVICE_MODE_KEYSTORE_DIR = APPLICATION_INSTALLATION_DIR.resolve(APPLICATION_NAME + "-vault-keystore").resolve(SHORTENED_CHAIN_ID);
    public static final Path SERVICE_MODE_2FA_DIR = SERVICE_MODE_KEYSTORE_DIR.resolve(APPLICATION_NAME + "-2fa");
    public static final Path SERVICE_MODE_LOGS_DIR = APPLICATION_INSTALLATION_DIR.resolve(APPLICATION_NAME + "-logs");
    public static final Path SERVICE_MODE_PID_FILE = APPLICATION_INSTALLATION_DIR.resolve(APPLICATION_NAME + "-" + SHORTENED_CHAIN_ID + ".pid");
    public static final Path UNIX_SERVICE_MODE_LOGS_DIR = Paths.get("/var/log", APPLICATION_NAME);
    public static final Path UNIX_SERVICE_MODE_PID_FILE = Paths.get("/var/run", APPLICATION_NAME, APPLICATION_NAME + "-" + SHORTENED_CHAIN_ID + ".pid");

    @Test
    public void testUserModeDirProvider() {
        UserModeDirProvider dirProvider = new UserModeDirProvider(APPLICATION_NAME, CHAIN_ID);
        assertEquals(USER_MODE_PID_FILE, dirProvider.getPIDFile());
        assertEquals(USER_MODE_DB_DIR, dirProvider.getDbDir());
        assertEquals(USER_MODE_KEYSTORE_DIR, dirProvider.getVaultKeystoreDir());
        assertEquals(USER_MODE_2FA_DIR, dirProvider.get2FADir());
        assertEquals(USER_MODE_LOGS_DIR, dirProvider.getLogsDir());
    }
    @Test
    public void testServiceModeDirProvider() {
        ServiceModeDirProvider dirProvider = new ServiceModeDirProvider(APPLICATION_NAME, CHAIN_ID);
        assertEquals(SERVICE_MODE_PID_FILE, dirProvider.getPIDFile());
        assertEquals(SERVICE_MODE_DB_DIR, dirProvider.getDbDir());
        assertEquals(SERVICE_MODE_KEYSTORE_DIR, dirProvider.getVaultKeystoreDir());
        assertEquals(SERVICE_MODE_2FA_DIR, dirProvider.get2FADir());
        assertEquals(SERVICE_MODE_LOGS_DIR, dirProvider.getLogsDir());
    }

    @Test
    public void testUnixServiceModeDirProvider() {
        UnixServiceModeDirProvider dirProvider = new UnixServiceModeDirProvider(APPLICATION_NAME, CHAIN_ID);
        assertEquals(UNIX_SERVICE_MODE_PID_FILE, dirProvider.getPIDFile());
        assertEquals(SERVICE_MODE_DB_DIR, dirProvider.getDbDir());
        assertEquals(SERVICE_MODE_KEYSTORE_DIR, dirProvider.getVaultKeystoreDir());
        assertEquals(SERVICE_MODE_2FA_DIR, dirProvider.get2FADir());
        assertEquals(UNIX_SERVICE_MODE_LOGS_DIR, dirProvider.getLogsDir());
    }

    @Test
    public void testUserModeDirProviderWithPredefinedDirLocations() {
        Path basePath = Paths.get("");
        Path customLogPath = basePath.resolve("log");
        Path customDbPath = basePath.resolve("db");
        PredefinedDirLocations predefinedDirLocations = new PredefinedDirLocations(customDbPath.toString(),
                customLogPath.toString(), null, null, null);

        UserModeDirProvider dirProvider = new UserModeDirProvider(APPLICATION_NAME, CHAIN_ID, predefinedDirLocations);
        assertEquals(USER_MODE_PID_FILE, dirProvider.getPIDFile());
        assertEquals(customDbPath, dirProvider.getDbDir());
        assertEquals(USER_MODE_KEYSTORE_DIR, dirProvider.getVaultKeystoreDir());
        assertEquals(USER_MODE_2FA_DIR, dirProvider.get2FADir());
        assertEquals(customLogPath, dirProvider.getLogsDir());
    }
    @Test
    public void testServiceModeDirProviderWithPredefinedDirLocations() {
        Path basePath = Paths.get("");
        Path customLogPath = basePath.resolve("log");
        Path customPidPath = basePath.resolve("pid");
        PredefinedDirLocations predefinedDirLocations = new PredefinedDirLocations(null,
                customLogPath.toString(), null, customPidPath.toString(), null);

        ServiceModeDirProvider dirProvider = new ServiceModeDirProvider(APPLICATION_NAME, CHAIN_ID, predefinedDirLocations);
        assertEquals(customPidPath, dirProvider.getPIDFile());
        assertEquals(SERVICE_MODE_DB_DIR, dirProvider.getDbDir());
        assertEquals(SERVICE_MODE_KEYSTORE_DIR, dirProvider.getVaultKeystoreDir());
        assertEquals(SERVICE_MODE_2FA_DIR, dirProvider.get2FADir());
        assertEquals(customLogPath, dirProvider.getLogsDir());
    }

    @Test
    public void testUnixServiceModeDirProviderWithPredefinedLogAndPidLocation() {
        Path basePath = Paths.get("");
        Path customLogPath = basePath.resolve("log");
        Path customPidPath = basePath.resolve("pid");
        PredefinedDirLocations predefinedDirLocations = new PredefinedDirLocations(null,
                customLogPath.toString(), null, customPidPath.toString(), null);

        UnixServiceModeDirProvider dirProvider = new UnixServiceModeDirProvider(APPLICATION_NAME, CHAIN_ID, predefinedDirLocations);
        assertEquals(customPidPath, dirProvider.getPIDFile());
        assertEquals(SERVICE_MODE_DB_DIR, dirProvider.getDbDir());
        assertEquals(SERVICE_MODE_KEYSTORE_DIR, dirProvider.getVaultKeystoreDir());
        assertEquals(SERVICE_MODE_2FA_DIR, dirProvider.get2FADir());
        assertEquals(customLogPath, dirProvider.getLogsDir());
        assertEquals(customLogPath, dirProvider.getLogsDir());
    }
}
