/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigDirProviderTest {

    public static final String APPLICATION_NAME = "test";

    public static final String USER_HOME_CONFIG_DIRECTORY
            = System.getProperty("user.home") + File.separator + "." + APPLICATION_NAME + File.separator + "conf";

    public static final String INSTALLATION_CONFIG_DIR = getInstallationConfigDir();
    public static final String SYSTEM_CONFIG_DIR = "/etc/" + APPLICATION_NAME + "/conf";

    private static String getInstallationConfigDir() {
        try {
            return Paths.get(ConfigDirProviderTest.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent().getParent().getParent().resolve("conf").toAbsolutePath().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @BeforeEach
    public void setUp() {
        RuntimeEnvironment.getInstance().setMain(this.getClass());
    }

    @Test
    public void testUnixUserModeConfigDirProvider() {
        UnixConfigDirProvider unixConfigDirProvider = new UnixConfigDirProvider(APPLICATION_NAME, false, 0, "");
        assertEquals(SYSTEM_CONFIG_DIR, unixConfigDirProvider.getSysConfigLocation());
        assertEquals(USER_HOME_CONFIG_DIRECTORY, unixConfigDirProvider.getConfigLocation());
        assertEquals(INSTALLATION_CONFIG_DIR, unixConfigDirProvider.getInstallationConfigLocation());
    }

    @Test
    public void testUnixServiceModeConfigDirProvider() {
        UnixConfigDirProvider unixConfigDirProvider = new UnixConfigDirProvider(APPLICATION_NAME, true, 0, "");
        assertEquals(SYSTEM_CONFIG_DIR, unixConfigDirProvider.getSysConfigLocation());
        assertEquals(SYSTEM_CONFIG_DIR, unixConfigDirProvider.getConfigLocation());
        assertEquals(INSTALLATION_CONFIG_DIR, unixConfigDirProvider.getInstallationConfigLocation());
    }

    @Test
    public void testDefaultConfigDirProviderInUserMode() {
        DefaultConfigDirProvider defaultConfigDirProvider = new DefaultConfigDirProvider(APPLICATION_NAME, false, 0, "");
        assertEquals(INSTALLATION_CONFIG_DIR, defaultConfigDirProvider.getSysConfigLocation());
        assertEquals(USER_HOME_CONFIG_DIRECTORY, defaultConfigDirProvider.getConfigLocation());
        assertEquals(INSTALLATION_CONFIG_DIR, defaultConfigDirProvider.getInstallationConfigLocation());
    }

    @Test
    public void testDefaultConfigDirProviderInServiceMode() {
        DefaultConfigDirProvider defaultConfigDirProvider = new DefaultConfigDirProvider(APPLICATION_NAME, true, 0, "");
        assertEquals(INSTALLATION_CONFIG_DIR, defaultConfigDirProvider.getSysConfigLocation());
        assertEquals(INSTALLATION_CONFIG_DIR, defaultConfigDirProvider.getConfigLocation());
        assertEquals(INSTALLATION_CONFIG_DIR, defaultConfigDirProvider.getInstallationConfigLocation());
    }

}
