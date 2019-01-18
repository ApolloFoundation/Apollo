/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Test;

public class ConfigDirProviderTest {

    public static final String APPLICATION_NAME = "test";
    public static final String USER_HOME_CONFIG_DIRECTORY = System.getProperty("user.home") + File.separator + APPLICATION_NAME + File.separator + "conf";
    public static final String INSTALLATION_CONFIG_DIR = ConfigDirProviderTest.class.getClassLoader().getResource("").getPath() + "conf";
    public static final String SYSTEM_CONFIG_DIR = "/etc/" + APPLICATION_NAME;

    @Test
    public void testUnixUserModeConfigDirProvider() {
        UnixConfigDirProvider unixConfigDirProvider = new UnixConfigDirProvider(APPLICATION_NAME, false);
        assertEquals(SYSTEM_CONFIG_DIR, unixConfigDirProvider.getSysConfigDirectory());
        assertEquals(USER_HOME_CONFIG_DIRECTORY, unixConfigDirProvider.getUserConfigDirectory());
        assertEquals(INSTALLATION_CONFIG_DIR, unixConfigDirProvider.getInstallationConfigDirectory());
    }
    @Test
    public void testUnixServiceModeConfigDirProvider() {
        UnixConfigDirProvider unixConfigDirProvider = new UnixConfigDirProvider(APPLICATION_NAME, true);
        assertEquals(SYSTEM_CONFIG_DIR, unixConfigDirProvider.getSysConfigDirectory());
        assertEquals(INSTALLATION_CONFIG_DIR, unixConfigDirProvider.getUserConfigDirectory());
        assertEquals(INSTALLATION_CONFIG_DIR, unixConfigDirProvider.getInstallationConfigDirectory());
    }

    @Test
    public void testDefaultConfigDirProviderInUserMode() {
        DefaultConfigDirProvider defaultConfigDirProvider =  new DefaultConfigDirProvider(APPLICATION_NAME, false);
        assertEquals(INSTALLATION_CONFIG_DIR, defaultConfigDirProvider.getSysConfigDirectory());
        assertEquals(USER_HOME_CONFIG_DIRECTORY,defaultConfigDirProvider.getUserConfigDirectory());
        assertEquals(INSTALLATION_CONFIG_DIR, defaultConfigDirProvider.getInstallationConfigDirectory());
    }
    @Test
    public void testDefaultConfigDirProviderInServiceMode() {
        DefaultConfigDirProvider defaultConfigDirProvider =  new DefaultConfigDirProvider(APPLICATION_NAME, true);
        assertEquals(INSTALLATION_CONFIG_DIR, defaultConfigDirProvider.getSysConfigDirectory());
        assertEquals(INSTALLATION_CONFIG_DIR,defaultConfigDirProvider.getUserConfigDirectory());
        assertEquals(INSTALLATION_CONFIG_DIR, defaultConfigDirProvider.getInstallationConfigDirectory());
    }

}
