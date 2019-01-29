package com.apollocurrency.aplwallet.apl.util.env.config;

import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesConfigLoader extends AbstractConfigLoader<Properties> {
    private static final String DEFAULT_PROPERTIES_FILENAME = "apl-blockchain.properties";

    public PropertiesConfigLoader(ConfigDirProvider dirProvider, boolean ignoreResources, String configDir, String resourceName) {
        super(dirProvider, ignoreResources, configDir, resourceName);
    }
    public PropertiesConfigLoader(ConfigDirProvider dirProvider, boolean ignoreResources, String configDir) {
        super(dirProvider, ignoreResources, configDir, DEFAULT_PROPERTIES_FILENAME);
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
        res.putAll(oldProperties);
        res.putAll(newProperties);
        return res;
    }
}
