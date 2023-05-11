/*
 * Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.conf;


import com.apollocurrency.aplwallet.apl.util.Version;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Config Properies known by Apollo application
 * @author Oleksiy Lukin alukin@gmail.com
 */
@Slf4j
public class ConfigVerifier {
    private final static String FIRST_RELEASE = "1.0.0";
    private final static String DEX_RELEASE = "1.36.0";
    private final static String SHARDING_RELEASE = "1.35.0";
    private final static String MARIADB_RELEASE = "1.48.0";

    private ConfigVerifier() {
    }

    /**
     * property name mapped to parameters
     */
    @Getter
    private Map<String,ConfigRecord> knownProps;

    public boolean isSupported(String propName) {
        return knownProps.containsKey(propName);
    }

    public ConfigRecord get(String propName){
        ConfigRecord res =  knownProps.get(propName);
        return res;
    }

    private ConfigRecord getOrAdd(String propName){
        ConfigRecord res = knownProps.get(propName);
        if (res == null) {
            res = ConfigRecord.builder()
                .name(propName)
                .build();
            knownProps.put(propName, res);
        }
        return res;
    }

    public List<ConfigRecord> listDeprecated(Version currVersion){
        List<ConfigRecord> res = new ArrayList<>();
        knownProps.values().stream().filter(pr -> (pr.deprecatedSince.lessThan(currVersion))).forEachOrdered(pr -> {
            res.add(pr);
        });
        return res;
    }

    public List<ConfigRecord> listNewAfter(Version ver){
        List<ConfigRecord> res = new ArrayList<>();
        knownProps.values().stream().filter(pr -> (pr.sinceRelease.greaterThan(ver))).forEachOrdered(pr -> {
            res.add(pr);
        });
        return res;
    }

 /**
  * Dumps all known properties with comment lines
  * containing all available information about config property
  * @param pos output stream where to dump
  */
    public void dumpToProperties(OutputStream pos) throws IOException{
        Writer w = new OutputStreamWriter(pos);
        for(ConfigRecord cr: knownProps.values()){
            w.write("# "+cr.description);
            w.write("# Command line option: "+cr.cmdLineOpt+" Environment variable: "+cr.envVar);
            w.write(cr.name+"="+cr.defaultValue);
        }
    }


/**
 * Parse properties file comparing to known properties and filling undefined with defaults
 * @param config Properties file from resource or disk.
 * Unknown properties will be logged with WARN level; missing required properties will
 * be filled with default and warning will be logged
 * @param currentVersion
 * @return ready to use properties
 */
    public Properties parse(Properties config, Version currentVersion){
        //go through supplied config and check it, warn on deprecated and on unknown
        for(Object key: config.keySet()){
            String name = (String)key;
            String value = config.getProperty(name);
            ConfigRecord cr = knownProps.get(name);
            if (cr == null) {
                log.warn("Unknown config property: '{}' with value: '{}'. It probably will be ignored", name, value);
            } else if (currentVersion.greaterThan(cr.deprecatedSince)) {
                log.warn("Config property: '{}' is deprecated since version '{}'", name, cr.deprecatedSince);
            }

        }
        //define required properties, warn on undefined
        for(ConfigRecord pr: knownProps.values()){
            String val = config.getProperty(pr.name);
            if (pr.isRequired && (val == null || val.isEmpty())) {
                config.put(pr.name, pr.defaultValue);
                log.warn("Required property: '{}' is not defined in config. Putting default value: '{}'", pr.name, pr.defaultValue);
            }
        }
        return config;
    }

    /**
     * All known properties must be initiated in this method; All known properties are defined
     * in resource file conf/apl-blockchain.properties or in files in corresponding testnet config
     * directories.TODO: keywords for deprecation, command line options and env variables
     * @param resourcePath path to properties file in resources
     * @return created properties fully initiated with default values
     */
    public static ConfigVerifier create( String resourcePath ) throws IOException {
        ConfigVerifier kp = new ConfigVerifier();
        DefaultConfig dc = null;
        if (resourcePath == null || resourcePath.isEmpty()) {
            resourcePath="conf/apl-blockchain.properties";
        }
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        log.debug("Trying to load resource by path = '{}' ...", resourcePath);
        try (InputStream is = classloader.getResourceAsStream(resourcePath)) {
             dc = DefaultConfig.fromStream(is);
        }

        kp.knownProps = dc.getKnownProperties();

    //TODO: hardcode command line and env if so

        kp.getOrAdd("apl.customDbDir").sinceRelease = new Version(MARIADB_RELEASE);

        kp.getOrAdd("apl.customVaultKeystoreDir").sinceRelease = (new Version(DEX_RELEASE));

        kp.getOrAdd("apl.customPidFile").sinceRelease = new Version(MARIADB_RELEASE);

        return kp;
    }
}
