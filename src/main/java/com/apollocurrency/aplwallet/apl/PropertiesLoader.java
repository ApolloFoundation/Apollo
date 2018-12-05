/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.apollocurrency.aplwallet.apl.env.DirProvider;
import com.apollocurrency.aplwallet.apl.util.Convert;
import java.net.URL;
import java.util.Enumeration;
import org.slf4j.Logger;

public class PropertiesLoader {
    private static final Logger LOG = getLogger(PropertiesLoader.class);
    public static final String DEFAULT_APL_DEFAULT_PROPERTIES_FILE_NAME = "apl-default.properties";
    public static final String DEFAULT_APL_PROPERTIES_FILE_NAME = "apl.properties";
    public static final String DEFAULT_APL_INSTALLER_PROPERTIES_FILE_NAME = "apl-installer.properties";
    public static final String DEFAULT_CONFIG_DIR = "conf";
    private final Properties defaultProperties = new Properties();
    private Properties properties = new Properties();
    private final DirProvider dirProvider;
    private final String defaultPropertiesFileName;
    private final String propertiesFileName;
    private final String installerPropertiesFileName;
    private final String configDir;
    private final Properties customProperties;

    private PropertiesLoader(Builder builder) {
        this.dirProvider = builder.dirProvider;
        this.defaultPropertiesFileName = builder.defaultPropertiesFileName;
        this.propertiesFileName = builder.propertiesFileName;
        this.installerPropertiesFileName = builder.installerPropertiesFileName;
        this.configDir = builder.configDir;
        this.customProperties = builder.customProperties;
    }
    public static class Builder {
        private DirProvider dirProvider;
        private String defaultPropertiesFileName = DEFAULT_APL_DEFAULT_PROPERTIES_FILE_NAME;
        private String propertiesFileName = DEFAULT_APL_PROPERTIES_FILE_NAME;
        private String installerPropertiesFileName = DEFAULT_APL_INSTALLER_PROPERTIES_FILE_NAME;
        private String configDir = DEFAULT_CONFIG_DIR;
        private Properties customProperties = new Properties();

        public Builder(DirProvider dirProvider) {
            this.dirProvider = dirProvider;
        }

        public Builder defaultPropertiesFileName(String defaultPropertiesFileName) {
            this.defaultPropertiesFileName = defaultPropertiesFileName;
            return this;
        }
        public Builder propertiesFileName(String propertiesFileName) {
            this.propertiesFileName = propertiesFileName;
            return this;
        }
        public Builder installerPropertiesFileName(String installerPropertiesFileName) {
            this.installerPropertiesFileName = installerPropertiesFileName;
            return this;
        }
        public Builder configDir(String configDir) {
            this.configDir = configDir;
            return this;
        }
        public Builder customProperties(Properties properties, boolean append) {
            if (append) {
                this.customProperties.putAll(properties);
            } else {
                this.customProperties = properties;
            }
            return this;
        }
        public Builder customProperties(Properties properties) {
            return customProperties(properties, false);
        }

        public PropertiesLoader build() {
            return new PropertiesLoader(this);
        }
    }

    public void init() {
        loadProperties(defaultProperties, defaultPropertiesFileName, true, dirProvider, configDir);
        properties.putAll(defaultProperties);
        loadProperties(properties, installerPropertiesFileName, true, dirProvider, configDir);
        loadProperties(properties, propertiesFileName, false, dirProvider, configDir);
        if (customProperties != null && !customProperties.isEmpty()) {
            properties.putAll(customProperties);
        }
    }

    public Properties getDefaultProperties() {
        return defaultProperties;
    }

    public void loadSystemProperties(List<String> propertiesNamesList) {
        propertiesNamesList.forEach(propertyName -> {
                String propertyValue;
                if ((propertyValue = System.getProperty(propertyName)) != null) {
                    Object oldPropertyValue = properties.setProperty(propertyName, propertyValue);
                    if (oldPropertyValue == null) {
                        LOG.info("System property set: {} -{}", propertyName, propertyValue);
                    } else {
                        LOG.warn("Replace property {} - {} by new system property: {}", propertyName, oldPropertyValue, propertyValue);
                    }
                }
                else {
                    LOG.debug("System property {} not defined", propertyName);
                }
        });
    }

    protected static Properties loadProperties(Properties properties, String propertiesFile, boolean isDefault, DirProvider dirProvider, String configDir) {
        try {
            // Load properties from location specified as command line parameter
            String configFile = System.getProperty(propertiesFile);
            if (configFile != null) {
                System.out.printf("Loading %s from %s\n", propertiesFile, configFile);
                try (InputStream fis = new FileInputStream(configFile)) {
                    properties.load(fis);
                    return properties;
                } catch (IOException e) {
                    throw new IllegalArgumentException(String.format("Error loading %s from %s", propertiesFile, configFile));
                }
            } else {
                try (InputStream is = ClassLoader.getSystemResourceAsStream(configDir+"/"+propertiesFile)) {
                    // When running apl.exe from a Windows installation we always have apl.properties in the classpath but this is not the apl properties file
                    // Therefore we first load it from the classpath and then look for the real apl.properties in the user folder.
                    if (is != null) {
                        System.out.printf("Loading %s from classpath\n", propertiesFile);
                        properties.load(is);
                        if (isDefault) {
                            return properties;
                        }
                    }
                    // load non-default properties files from the user folder
                    if (!dirProvider.isLoadPropertyFileFromUserDir()) {
                        return properties;
                    }
                    String homeDir = dirProvider.getUserHomeDir();
                    if (!Files.isReadable(Paths.get(homeDir))) {
                        System.out.printf("Creating dir %s\n", homeDir);
                        try {
                            Files.createDirectory(Paths.get(homeDir));
                        } catch(Exception e) {
                            if (!(e instanceof NoSuchFileException)) {
                                throw e;
                            }
                            // Fix for WinXP and 2003 which does have a roaming sub folder
                            Files.createDirectory(Paths.get(homeDir).getParent());
                            Files.createDirectory(Paths.get(homeDir));
                        }
                    }
                    Path confDir = Paths.get(homeDir, configDir);
                    if (!Files.isReadable(confDir)) {
                        System.out.printf("Creating dir %s\n", confDir);
                        Files.createDirectory(confDir);
                    }
                    Path propPath = Paths.get(confDir.toString()).resolve(Paths.get(propertiesFile));
                    if (Files.isReadable(propPath)) {
                        System.out.printf("Loading %s from dir %s\n", propertiesFile, confDir);
                        properties.load(Files.newInputStream(propPath));
                    } else {
                        System.out.printf("Creating property file %s\n", propPath);
                        Files.createFile(propPath);
                        Files.write(propPath, Convert.toBytes("# use this file for workstation specific " + propertiesFile));
                    }
                    return properties;
                } catch (IOException e) {
                    throw new IllegalArgumentException("Error loading " + propertiesFile, e);
                }
            }
        } catch(IllegalArgumentException e) {
            e.printStackTrace(); // make sure we log this exception
            throw e;
        }
    }

    public int getIntProperty(String name) {
        return getIntProperty(name, 0);
    }

    public int getIntProperty(String name, int defaultValue) {
        try {
            int result = Integer.parseInt(properties.getProperty(name));
            LOG.info(name + " = \"" + result + "\"");
            return result;
        } catch (NumberFormatException e) {
            LOG.info(name + " not defined or not numeric, using default value " + defaultValue);
            return defaultValue;
        }
    }

    public String getStringProperty(String name) {
        return getStringProperty(name, null, false);
    }

    public String getStringProperty(String name, String defaultValue) {
        return getStringProperty(name, defaultValue, false);
    }

    public String getStringProperty(String name, String defaultValue, boolean doNotLog) {
        return getStringProperty(name, defaultValue, doNotLog, null);
    }

    public String getStringProperty(String name, String defaultValue, boolean doNotLog, String encoding) {
        String value = properties.getProperty(name);
        if (value != null && ! "".equals(value)) {
            LOG.info(name + " = \"" + (doNotLog ? "{not logged}" : value) + "\"");
        } else {
            LOG.info(name + " not defined");
            value = defaultValue;
        }
        if (encoding == null || value == null) {
            return value;
        }
        try {
            return new String(value.getBytes("ISO-8859-1"), encoding);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getStringListProperty(String name) {
        String value = getStringProperty(name);
        if (value == null || value.length() == 0) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String s : value.split(";")) {
            s = s.trim();
            if (s.length() > 0) {
                result.add(s);
            }
        }
        return result;
    }

    public boolean getBooleanProperty(String name) {
        return getBooleanProperty(name, false);
    }

    public boolean getBooleanProperty(String name, boolean defaultValue) {
        String value = properties.getProperty(name);
        if (Boolean.TRUE.toString().equals(value)) {
            LOG.info(name + " = \"true\"");
            return true;
        } else if (Boolean.FALSE.toString().equals(value)) {
            LOG.info(name + " = \"false\"");
            return false;
        }
        LOG.info(name + " not defined, using default " + defaultValue);
        return defaultValue;
    }
}
