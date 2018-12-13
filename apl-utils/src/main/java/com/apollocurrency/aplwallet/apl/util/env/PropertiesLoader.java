
package com.apollocurrency.aplwallet.apl.util.env;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Loads properties from different places
 * @author alukin@gmail.com
 */
public class PropertiesLoader {

    public static final String DEFAULT_APL_DEFAULT_PROPERTIES_FILE_NAME = "apl-default.properties";
    public static final String DEFAULT_APL_PROPERTIES_FILE_NAME = "apl.properties";
    public static final String DEFAULT_APL_INSTALLER_PROPERTIES_FILE_NAME = "apl-installer.properties";
    public static final String DEFAULT_CONFIG_DIR = "conf";
    private final String defaultPropertiesFileName = DEFAULT_APL_DEFAULT_PROPERTIES_FILE_NAME;
    private final String propertiesFileName= DEFAULT_APL_PROPERTIES_FILE_NAME;
    private final String installerPropertiesFileName=DEFAULT_APL_INSTALLER_PROPERTIES_FILE_NAME;
    private final String configDir = DEFAULT_CONFIG_DIR;    
    

       private DirProvider dirProvider;
       private static final Logger LOG = getLogger(PropertiesLoader.class);   
       private Properties properties = new Properties();
       private final Properties defaultProperties = new Properties();
       private  Properties customProperties;

    public PropertiesLoader(DirProvider dirProvider) {
        this.dirProvider = dirProvider;
        init();
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
 

    protected Properties loadProperties(Properties properties, String propertiesFile, boolean isDefault, DirProvider dirProvider, String configDir) {
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
    
}
