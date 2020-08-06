/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

/**
 * Provides paths to application config directories
 */
public interface ConfigDirProvider {

    public static final String CONFIGS_DIR_NAME = "configs";

    /**
     * Path to directory where main executable jar file is placed
     *
     * @return File denoting path to directory with main executable jar
     */
    public String getInstallationConfigLocation();

    /**
     * Path to system config directory, depends on OS
     *
     * @return Path to system config directory
     */
    public String getSysConfigLocation();

    /**
     * Path to user's config directory, depends on OS
     *
     * @return Path to user's config directory
     */
    public String getUserConfigLocation();

    /**
     * Just name of config directory of zip file depending of initialization
     *
     * @return name of config for defined network
     */
    public String getConfigName();

    /**
     * Directory where config directories or zip files for different networks
     * reside
     *
     * @return
     */
    public String getConfigLocation();

}
