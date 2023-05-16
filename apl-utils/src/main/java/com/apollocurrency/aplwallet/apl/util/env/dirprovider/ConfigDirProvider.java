/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import java.util.UUID;

/**
 * Provides paths to application config directories
 */
public interface ConfigDirProvider {

    String CONFIGS_DIR_NAME = "configs";

    /**
     * Path to directory where main executable jar file is placed
     *
     * @return File denoting path to directory with main executable jar
     */
    String getInstallationConfigLocation();

    /**
     * Path to system config directory, depends on OS
     *
     * @return Path to system config directory
     */
    String getSysConfigLocation();

    /**
     * Path to user's config directory, depends on OS
     *
     * @return Path to user's config directory
     */
    String getUserConfigLocation();

    /**
     * Just name of config directory of zip file depending of initialization
     *
     * @return name of config for defined network
     */
    String getConfigName();

    /**
     * Directory where config directories or zip files for different networks
     * reside
     *
     * @return
     */
    String getConfigLocation();

    /**
     * Each network has it's chain ID and it must be known to ConfigDirProvider
     * If it can not be resolved in constructor, it must be resolved later
     *
     * @return
     */
    UUID getChainId();

    /**
     * In some cases, e.g.after loading from configs from custom directories,
     * chainID must be set programmatic
     *
     * @param newID chain ID to set
     */
    void setChainID(UUID newID);

    /**
     * Command line may specify chainID partially so we can use this information
     * later to resolve full UUID using configurations already present on local
     * system in standard config locations
     *
     * @return few chain ID UUID first bytes in hex
     */
    String getChainIdPart();

}
