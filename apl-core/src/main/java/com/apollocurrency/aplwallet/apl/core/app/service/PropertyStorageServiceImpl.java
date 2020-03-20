package com.apollocurrency.aplwallet.apl.core.app.service;

import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import lombok.extern.slf4j.Slf4j;

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

    @Inject
    public PropertyStorageServiceImpl(ConfigDirProvider configDirProvider) {
        this.configDirProvider = configDirProvider;
    }

    @Override
    public boolean storeProperties(Properties props) {
        log.trace("storeProperties : props() = {}", props);
        createFolderIfNotExist();

        try (OutputStream output = new FileOutputStream(getTargetFile(), false) ) {
            props.store(output, null);
            log.trace("storeProperties DONE : props() = [{}]", props.size());
        } catch (IOException e) {
            log.error("Exception writing temp props file = '{}'", getTargetFile().getPath(), e);
            return false;
        }

        return true;
    }

    @Override
    public Properties loadProperties() {
        File targetFile = getTargetFile();
        log.trace("loadProperties : getTargetFile() = '{}'", targetFile);
        try (InputStream inputStream = new FileInputStream(targetFile) ) {
            Properties properties = new Properties();
            properties.load(inputStream);
            log.trace("inputStream props = {}", properties);
            return properties;
        } catch (IOException e) {
            log.error("Exception writing temp props file = '{}'", targetFile.getPath(), e);
        }
        return null;
    }

    private File getTargetFile(){
        String configDir = configDirProvider.getConfigDirectory();
        return Path.of(configDir, EXPORTED_DATA_FILE_NAME).toFile(); // full target file
    }

    private void createFolderIfNotExist(){
        String configDir = configDirProvider.getConfigDirectory();
        File folder = new File(configDir);
        if (!folder.exists()) {
            boolean result = folder.mkdirs();
            if (!result) {
                log.error("Error, config folder was not created: configDir = '{}'", configDir);
                throw new RuntimeException("Error, config folder was not created: configDir = " + configDir);
            }
            log.trace("Config folder was created '{}': configDir = '{}'", result, folder);
        }
        log.trace("Config folder: configDir = '{}'", folder);
    }

    public boolean isExist(){
        return getTargetFile().exists();
    }
}
