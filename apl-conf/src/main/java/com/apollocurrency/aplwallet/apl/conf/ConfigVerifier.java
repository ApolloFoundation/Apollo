/*
 * Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.conf;


import com.apollocurrency.aplwallet.apl.util.Version;
import java.io.OutputStream;
import java.nio.file.Path;
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
        return knownProps.get(propName);
    }
    
    private void put(String name, ConfigRecord pr){
        pr.name = name;
        knownProps.put(name, pr);
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
    public void dumpToProperties(OutputStream pos){
        //TODO: implement
    }

    
/**
 * Parse properties file comparing to known properties and fillimng undefined with defaults
 * @param config Properties file from resource or disk.
 * Unknown proerties will be logged with WARN level; missing required properties will
 * be filled with default and warning will be logged
 * @return reaqdy to use properties
 */
    public Properties parse(Properties config){
        //go trough suppied config and check it: warn on deprecated and on unknown
        for(Object propery: config.entrySet()){
            
        }
        //define required properties, warn on undefined
        for(ConfigRecord pr: knownProps.values()){
            
        }
        return config;
    }
/**
 * All known properties must be inited in this method; 
 * @return created properties fully inited with default values
 */
    public static ConfigVerifier create(){
        ConfigVerifier kp = new ConfigVerifier();
        
        kp.put("apl.shareMyAddress", 
            ConfigRecord.builder()
                  .defaultValue("true")
                  .description("Announce my IP address/hostname to peers and allow them to share it with other peers. If disabled, peer networking servlet will not be started at all.")
                  .sinceRelease(new Version(FIRST_RELEASE))  
            .build()
        );
        
    //TODO: hardcode all known properties with ddescription from properies in resources
    
    //
        kp.put("apl.customDbDir", 
            ConfigRecord.builder()
                  .defaultValue("")
                  .description("Directory where database is located. Default location is $HOME/.apl-blockchain/apl-blockchain-db. Could be overrided by  env vars and cmd args")
                  .sinceRelease(new Version(MARIADB_RELEASE))  
            .build()
        ); 
        
        kp.put("apl.customVaultKeystoreDir", 
            ConfigRecord.builder()
                  .defaultValue("")
                  .description("Absolute path to custom keystore dir. Could be overrided by env vars and cmd args")
                  .sinceRelease(new Version(DEX_RELEASE))  
            .build()
        );
        
        kp.put("apl.customPidFile", 
            ConfigRecord.builder()
                  .defaultValue("")
                  .description("Absolute path to  PID file. Could be overrided by env vars and cmd args")
                  .sinceRelease(new Version(MARIADB_RELEASE))  
            .build()
        );      
        
        kp.put("apl.dir2FA", 
            ConfigRecord.builder()
                  .defaultValue("")
                  .description("Absolute path to 2FAdata dir. Could be overrided by env vars and cmd args")
                  .sinceRelease(new Version(DEX_RELEASE))  
            .build()
        );   
        
        kp.put("apl.customDataExportDir", 
            ConfigRecord.builder()
                  .defaultValue("")
                  .description("Absolute path to custom data export dir (shard files and other data). Could be overrided by env vars and cmd args")
                  .sinceRelease(new Version(SHARDING_RELEASE))  
            .build()
        );   
        
        kp.put("apl.customDexStorageDir", 
            ConfigRecord.builder()
                  .defaultValue("")
                  .description("Absolute path to custom DEX storage. Could be overrided by env vars and cmd args")
                  .sinceRelease(new Version(DEX_RELEASE))  
            .build()
        );        
        return kp;
    }
}
