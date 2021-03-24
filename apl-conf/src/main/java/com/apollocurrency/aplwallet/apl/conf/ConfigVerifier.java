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
    private final static String DEX_RELEASE = "1.35.0";
    private final static String SHARDING_RELEASE = "1.36.0";
    private final static String MARIADB_RELEASE = "1.36.0";

    private ConfigVerifier() {
    }
    
    /**
     * propery nanme mapoped to parameters
     */
    @Getter
    private Map<String,ConfigRecord> knownProps;
    
    public boolean isSupported(String propName){
        ConfigRecord rec = knownProps.get(propName);
        return rec!=null;    
    }    
    
    public ConfigRecord get(String propName){
        ConfigRecord res =  knownProps.get(propName);
        return res;
    }

    private ConfigRecord getOrAdd(String propName){
        ConfigRecord res =  knownProps.get(propName);
        if(res==null){
            res = ConfigRecord.builder()
                    .name(propName)
                    .build();
            knownProps.put(propName,res);
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
  * containing all available information about config propery
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
 * Parse properties file comparing to known properties and fillimng undefined with defaults
 * @param config Properties file from resource or disk.
 * Unknown proerties will be logged with WARN level; missing required properties will
 * be filled with default and warning will be logged
 * @param currentVer
 * @return reaqdy to use properties
 */
    public Properties parse(Properties config, Version currentVer){
        //go trough suppied config and check it: warn on deprecated and on unknown
        for(Object key: config.keySet()){
            String name = (String)key;
            String value = config.getProperty(name);
            ConfigRecord cr = knownProps.get(name);
            if(cr==null){
                log.warn("Unknown config property: "+name+" with value: "+value + ". It propbably will be ignored");
            }else if (currentVer.greaterThan(cr.deprecatedSince)){
                log.warn("Config property: "+name+" is deprecated since version "+cr.deprecatedSince);
            }
            
        }
        //define required properties, warn on undefined
        for(ConfigRecord pr: knownProps.values()){
            String val = config.getProperty(pr.name);
            if(pr.isRequired && (val==null || val.isEmpty())){
                config.put(pr.name, pr.defaultValue);
                log.warn("Required property: "+pr.name+" is not defined in config. Putting default value: "+pr.defaultValue);
            }
        }
        return config;
    }

/**
 * All known properties must be inited in this method; All known properties are defined
 * in resource file conf/apl-blockchain.properties or in files in corresponding testnet config
 * directories.TODO: keywords for deprecation, command line options and env variables
 * @param respurcePath path to properties file in resources
 * @return created properties fully inited with default values
 */
    public static ConfigVerifier create( String respurcePath ) throws IOException {
        ConfigVerifier kp = new ConfigVerifier();
        DefaultConfig dc=null;
        if(respurcePath==null || respurcePath.isEmpty()){
            respurcePath="conf/apl-blockchain.properties";
        }
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        
        try (InputStream is = classloader.getResourceAsStream(respurcePath)) {
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
