package com.apollocurrency.aplwallet.apl.util.env.config;

import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.StringValidator;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractConfigLoader<T> implements ConfigLoader<T> {
    public static final String DEFAULT_CONFIG_DIR = "conf";
    private ConfigDirProvider dirProvider;
    private boolean ignoreResources;
    private String configDir = "";
    private T config;
    private String resourceName;

    public AbstractConfigLoader(ConfigDirProvider dirProvider, boolean ignoreResources, String configDir, String resourceName) {
        StringValidator.requireNonBlank(resourceName, "Resource name is blank or empty");

        this.dirProvider = dirProvider;
        if (!StringUtils.isBlank(configDir)) {
            this.configDir = configDir;
        }
        this.ignoreResources = ignoreResources;
        this.resourceName = resourceName;
    }

    public T load() {
        if (!ignoreResources) {
            loadFromResources();
        }
        loadFromUserDefinedDirectory();
        return config;
    }

    protected abstract T read(InputStream is) throws IOException;


    private void loadFromResources() {
        // using '/' as separator instead of platform dependent File.separator
        String fn = DEFAULT_CONFIG_DIR + "/" + resourceName;
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        try (InputStream is = classloader.getResourceAsStream(fn)) {
            T defaultConfig = read(is);
            config = merge(config, defaultConfig);
        }
        catch (IOException e) {
            System.err.println("Can not find resource: " + fn);
        }
    }

    private void loadFromUserDefinedDirectory() {
        List<String> searchDirs = new ArrayList<>();
        if (!StringUtils.isBlank(configDir)) { //load just from confDir
            searchDirs.add(configDir);
        } else { //go trough standard search order and load all
            searchDirs.add(dirProvider.getInstallationConfigDirectory());
            searchDirs.add(dirProvider.getSysConfigDirectory());
            searchDirs.add(dirProvider.getUserConfigDirectory());
        }
        for (String dir : searchDirs) {
            String p = dir + File.separator + resourceName;
            try (FileInputStream is = new FileInputStream(p)) {
                T userConfig = read(is);
                config = merge(config, userConfig);
            }
            catch (Exception ignored) {
            }
        }
    }

    protected abstract T merge(T oldValue, T newValue);

}
