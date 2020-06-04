package com.apollocurrency.aplwallet.apl.core.service.appdata.impl;

import com.apollocurrency.aplwallet.apl.core.service.appdata.PropertyStorageService;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Properties;

@Slf4j
@Singleton
public class PropertyStorageServiceImpl implements PropertyStorageService {

    private ConfigDirProvider configDirProvider;
    private String realConfigDir;

    @Inject
    public PropertyStorageServiceImpl(ConfigDirProvider configDirProvider) {
        this.configDirProvider = configDirProvider;
        this.realConfigDir = configDirProvider.getConfigDirectory();
    }

    @PostConstruct
    public void init() {
        validateAndCreateIfNotExist(false);
    }

    @Override
    public boolean storeProperties(Properties props) {
        validateAndCreateIfNotExist(true);

        try (OutputStream output = new FileOutputStream(getTargetFile(), false)) {
            props.store(output, null);
        } catch (IOException e) {
            log.error("Exception writing props file = '{}'", getTargetFile().getPath(), e);
            return false;
        }
        return true;
    }

    @Override
    public Properties loadProperties() {
        try (InputStream inputStream = new FileInputStream(getTargetFile())) {
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties;
        } catch (IOException e) {
            log.error("Exception read props file = '{}'", getTargetFile().getPath(), e);
        }

        return null;
    }

    private File getTargetFile() {
        return Path.of(this.realConfigDir, EXPORTED_DATA_FILE_NAME).toFile(); // full target file
    }

    private void validateAndCreateIfNotExist(boolean isCreate) {
        String[] configDirList = new String[]{configDirProvider.getConfigDirectory(), configDirProvider.getInstallationConfigDirectory()};
        for (String configDir : configDirList) {
            File folder = new File(configDir);
            if (!folder.exists()) {
                if (isCreate) {
                    boolean result = folder.mkdirs();
                    if (!result) {
                        log.error("Error, config folder does not exist: configDir = '{}', GO to NEXT DIR...", configDir);
                    } else {
                        this.realConfigDir = configDir;
                        log.debug("Config folder CREATED '{}': configDir = '{}'", result, this.realConfigDir);
                        return;
                    }
                }
            } else {
                this.realConfigDir = configDir;
                log.debug("Config folder FOUND: configDir = '{}'", this.realConfigDir);
                return;
            }
        }

    }

    public boolean isExist() {
        return getTargetFile().exists();
    }
}
