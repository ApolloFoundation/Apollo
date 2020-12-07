/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.config;

import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class UserResourceLocator implements ResourceLocator {

    private final ConfigDirProvider dirProvider;
    private final String configDir;

    public UserResourceLocator(ConfigDirProvider dirProvider, String configDir) {
        this.dirProvider = dirProvider;
        this.configDir = configDir;
    }

    @Override
    public Optional<InputStream> locate(final String resourceName) {
        Objects.requireNonNull(resourceName);
        return locateInDirectories(resourceName).or(() -> locateInResources(resourceName));
    }

    private Optional<InputStream> locateInResources(String resourceName) {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();

        InputStream is = classloader.getResourceAsStream(resourceName);
        if (is != null) {
            log.info("Located in resources, resource={}", resourceName);
        }

        return Optional.ofNullable(is);
    }

    private Optional<InputStream> locateInDirectories(String resourceName) {
        List<String> searchDirs = new ArrayList<>();
        if (!StringUtils.isBlank(configDir)) { //load just from confDir
            searchDirs.add(configDir);
        } else { //go trough standard search order and load all
            searchDirs.add(dirProvider.getInstallationConfigLocation() + File.separator + dirProvider.getConfigName());
            searchDirs.add(dirProvider.getSysConfigLocation() + File.separator + dirProvider.getConfigName());
            searchDirs.add(dirProvider.getUserConfigLocation() + File.separator + dirProvider.getConfigName());
        }
        if (log.isTraceEnabled()) {
            log.trace("The directory list:");
            searchDirs.forEach(s -> log.trace("  {}", s));
        }
        FileInputStream is = null;
        for (String dir : searchDirs) {
            String p = dir + File.separator + resourceName;
            try {
                is = new FileInputStream(p);
                log.info("Located in directories, resource={}", p);
                break;
            } catch (FileNotFoundException ignored) {
                log.info("File not found: " + p); // do not use logger (we should init it before using)
            }
        }
        return Optional.ofNullable(is);
    }
}
