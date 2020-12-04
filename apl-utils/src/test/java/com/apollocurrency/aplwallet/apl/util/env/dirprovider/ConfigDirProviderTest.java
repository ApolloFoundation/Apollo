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
        String r = unixConfigDirProvider.getSysConfigLocation() + "/" + unixConfigDirProvider.getConfigName();
        assertEquals(SYSTEM_CONFIG_DIR, r);
        r = unixConfigDirProvider.getConfigLocation() + File.separator + unixConfigDirProvider.getConfigName();
        assertEquals(USER_HOME_CONFIG_DIRECTORY, r);
        r = unixConfigDirProvider.getInstallationConfigLocation() + File.separator + unixConfigDirProvider.getConfigName();
        assertEquals(INSTALLATION_CONFIG_DIR, r);
    }

    @Test
    public void testUnixServiceModeConfigDirProvider() {
        UnixConfigDirProvider unixConfigDirProvider = new UnixConfigDirProvider(APPLICATION_NAME, true, 0, "");
        String r = unixConfigDirProvider.getSysConfigLocation() + "/" + unixConfigDirProvider.getConfigName();
        assertEquals(SYSTEM_CONFIG_DIR, r);
        r = unixConfigDirProvider.getConfigLocation() + "/" + unixConfigDirProvider.getConfigName();
        assertEquals(SYSTEM_CONFIG_DIR, r);
        r = unixConfigDirProvider.getInstallationConfigLocation() + File.separator + unixConfigDirProvider.getConfigName();
        assertEquals(INSTALLATION_CONFIG_DIR, r);
    }

    @Test
    public void testDefaultConfigDirProviderInUserMode() {
        DefaultConfigDirProvider defaultConfigDirProvider = new DefaultConfigDirProvider(APPLICATION_NAME, false, 0, "");
        String r = defaultConfigDirProvider.getSysConfigLocation() + File.separator + defaultConfigDirProvider.getConfigName();
        assertEquals(INSTALLATION_CONFIG_DIR, r);
        r = defaultConfigDirProvider.getConfigLocation() + File.separator + defaultConfigDirProvider.getConfigName();
        assertEquals(USER_HOME_CONFIG_DIRECTORY, r);
        r = defaultConfigDirProvider.getInstallationConfigLocation() + File.separator + defaultConfigDirProvider.getConfigName();
        assertEquals(INSTALLATION_CONFIG_DIR, r);
    }

    @Test
    public void testDefaultConfigDirProviderInServiceMode() {
        DefaultConfigDirProvider defaultConfigDirProvider = new DefaultConfigDirProvider(APPLICATION_NAME, true, 0, "");
        String r = defaultConfigDirProvider.getSysConfigLocation() + File.separator + defaultConfigDirProvider.getConfigName();
        assertEquals(INSTALLATION_CONFIG_DIR, r);
        r = defaultConfigDirProvider.getConfigLocation() + File.separator + defaultConfigDirProvider.getConfigName();
        assertEquals(INSTALLATION_CONFIG_DIR, r);
        r = defaultConfigDirProvider.getInstallationConfigLocation() + File.separator + defaultConfigDirProvider.getConfigName();
        assertEquals(INSTALLATION_CONFIG_DIR, r);
    }

    @Test
    public void testDefaultUserModeConfigDirProviderUUID() {
        DefaultConfigDirProvider defaultConfigDirProvider = new DefaultConfigDirProvider(APPLICATION_NAME, false, -1, "b5d7b697-f359-4ce5-a619-fa34b6fb01a5");
        String r = defaultConfigDirProvider.getConfigName();
        assertEquals("conf", r);
        defaultConfigDirProvider = new DefaultConfigDirProvider(APPLICATION_NAME, false, -1, "7654b697-f359-4ce5-a619-fa34b6fb01a5");
        r = defaultConfigDirProvider.getConfigName();
        assertEquals("configs/7654b697-f359-4ce5-a619-fa34b6fb01a5", r);
    }

}
