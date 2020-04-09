/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

/**
 * Provides paths to application config directories
 */
public interface ConfigDirProvider {
    /**
     * Path to directory where main executable jar file is placed
     *
     * @return File denoting path to directory with main executable jar
     */

    public String getInstallationConfigDirectory();

    /**
     * Path to system config directory, depends on OS
     *
     * @return Path to system config directory
     */
    public String getSysConfigDirectory();


    /**
     * Path to user's config directory, depends on OS
     *
     * @return Path to user's config directory
     */
    public String getUserConfigDirectory();

    /**
     * Just name of config directory depending of initialization of implementation
     *
     * @return
     */
    public String getConfigDirectoryName();

    /**
     * Gets config directory depending on run mode. In user mode it is getUserConfigDirectory()
     * In service mode it is getSysConfigDirectory()
     */
    public String getConfigDirectory();

}
