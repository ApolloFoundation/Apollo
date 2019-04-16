package com.apollocurrency.aplwallet.apl.util.env.config;

import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.StringValidator;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractConfigLoader<T> implements ConfigLoader<T> {

    public static final String DEFAULT_CONFIG_DIR = "conf";
    private ConfigDirProvider dirProvider;
    private boolean ignoreResources;
    private boolean ignoreUserConfig;
    private String configDir;
    private T config;
    private String resourceName;

    public AbstractConfigLoader(boolean ignoreResources, String configDir, String resourceName) {
        this(null, ignoreResources, configDir, resourceName);
    }

    public AbstractConfigLoader(ConfigDirProvider dirProvider, boolean ignoreResources, String configDir, String resourceName) {
        StringValidator.requireNonBlank(resourceName, "Resource name is blank or empty");
        this.ignoreUserConfig = dirProvider == null && StringUtils.isBlank(configDir);
        if (ignoreUserConfig && ignoreResources) {
                throw new IllegalArgumentException("No locations for config loading provided. Resources and user defined configs ignored");
        }
        this.dirProvider = dirProvider;
        this.ignoreResources = ignoreResources;
        this.configDir = configDir;
        this.resourceName = resourceName;
    }

    public AbstractConfigLoader(String resourceName) {
        this(null, false, resourceName);
    }

    public AbstractConfigLoader(ConfigDirProvider dirProvider, boolean ignoreResources, String resourceName) {
        this(dirProvider, ignoreResources, null, resourceName);
    }

    @Override
    public T load() {
        if (!ignoreResources) {
            loadFromResources();
        } else {
            System.out.println("Will ignore resources!");
        }
        if (!ignoreUserConfig) {
            loadFromUserDefinedDirectory();
        } else {
            System.out.println("Will ignore user defined config!");
        }
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
            System.err.println("Can not load resource: " + fn);
            e.printStackTrace();
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
            catch (FileNotFoundException ignored) {
                System.err.println("File not found: " + p); // do not use logger (we should init it before using)
            }
            catch (IOException e) {
                System.err.println("Config IO error " + e.toString());
            }
        }
    }

    protected abstract T merge(T oldValue, T newValue);

}
