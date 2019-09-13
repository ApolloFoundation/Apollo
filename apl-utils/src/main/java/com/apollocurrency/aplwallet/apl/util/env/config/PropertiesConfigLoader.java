package com.apollocurrency.aplwallet.apl.util.env.config;

import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class PropertiesConfigLoader extends AbstractConfigLoader<Properties> {
    private static final String DEFAULT_PROPERTIES_FILENAME = "apl-blockchain.properties";

    private List<String> systemPropertiesNames;


    public PropertiesConfigLoader(ConfigDirProvider dirProvider, boolean ignoreResources, String configDir, String resourceName, List<String> systemPropertiesNames) {
        super(dirProvider, ignoreResources, configDir, resourceName);
        this.systemPropertiesNames = systemPropertiesNames == null ? new ArrayList<>() : systemPropertiesNames;
    }


    public PropertiesConfigLoader(ConfigDirProvider dirProvider, boolean ignoreResources, String configDir, List<String> systemPropertiesNames) {
        this(dirProvider, ignoreResources, configDir, DEFAULT_PROPERTIES_FILENAME, systemPropertiesNames);
    }


    @Override
    protected Properties read(InputStream is) throws IOException {
        Properties properties = new Properties();
        properties.load(is);
        return properties;
    }

    @Override
    protected Properties merge(Properties oldProperties, Properties newProperties) {
        Properties res = new Properties();
        if (oldProperties != null) {
            res.putAll(oldProperties);
        }
        if (newProperties != null) {
            res.putAll(newProperties);
        }
        return res;
    }

    @Override
    public Properties load() {
        Properties loadedProps = super.load();
        loadedProps.putAll(loadSystemProperties());
        return loadedProps;
    }

    private Map<String, String> loadSystemProperties() {
        Map<String, String> systemProps = new HashMap<>();
        for (String propertyName : systemPropertiesNames) {
            String value = System.getProperty(propertyName);
            if (value != null) {
                systemProps.put(propertyName, value);
            }
        }
        return systemProps;
    }
}
