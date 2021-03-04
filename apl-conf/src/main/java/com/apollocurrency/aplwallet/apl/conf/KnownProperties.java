/*
 * Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.conf;


import com.apollocurrency.aplwallet.apl.util.Version;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Config Properies known by Apollo application
 * @author Oleksiy Lukin alukin@gmail.com
 */
@Slf4j
public class KnownProperties {
    private final static String FIRST_RELEASE = "1.0.0";
    private final static String DEX_RELEASE = "1.35.0";
    private final static String SHARDING_RELEASE = "1.36.0";
    private final static String MARIADB_RELEASE = "1.36.0";

    private KnownProperties() {
    }
    
    /**
     * propery nanme mapoped to parameters
     */
    @Getter
    private Map<String,PropertyRecord> knownProps;
    
    public boolean isSupported(String propName){
        PropertyRecord rec = knownProps.get(propName);
        return rec!=null;    
    }    
    
    public PropertyRecord get(String propName){
        return knownProps.get(propName);
    }
    
    private void put(String name, PropertyRecord pr){
        pr.name = name;
        knownProps.put(name, pr);
    }
    
    public List<PropertyRecord> listDeprecated(Version currVersion){
        List<PropertyRecord> res = new ArrayList<>();
        knownProps.values().stream().filter(pr -> (pr.deprecatedSince.lessThan(currVersion))).forEachOrdered(pr -> {
            res.add(pr);
        });
        return res;
    }
    
    public List<PropertyRecord> listNewAfter(Version ver){
        List<PropertyRecord> res = new ArrayList<>();
        knownProps.values().stream().filter(pr -> (pr.sinceRelease.greaterThan(ver))).forEachOrdered(pr -> {
            res.add(pr);
        });
        return res;
    }
    
    public void dumpToPropertiesFile(Path pathToDump){
        //TODO: implement
    }

    public static KnownProperties create(){
        KnownProperties kp = new KnownProperties();
        
        kp.put("apl.shareMyAddress", 
            PropertyRecord.builder()
                  .defaultValue("true")
                  .description("Announce my IP address/hostname to peers and allow them to share it with other peers. If disabled, peer networking servlet will not be started at all.")
                  .sinceRelease(new Version(FIRST_RELEASE))  
            .build()
        );
        
    //TODO: hardcode all known properties with ddescription from properies in resources        
        kp.put("apl.customDbDir", 
            PropertyRecord.builder()
                  .defaultValue("")
                  .description("Directory where database is located. Default location is $HOME/.apl-blockchain/apl-blockchain-db. Could be overrided by  env vars and cmd args")
                  .sinceRelease(new Version(MARIADB_RELEASE))  
            .build()
        ); 
        
        kp.put("apl.customVaultKeystoreDir", 
            PropertyRecord.builder()
                  .defaultValue("")
                  .description("Absolute path to custom keystore dir. Could be overrided by env vars and cmd args")
                  .sinceRelease(new Version(DEX_RELEASE))  
            .build()
        );
        
        kp.put("apl.customPidFile", 
            PropertyRecord.builder()
                  .defaultValue("")
                  .description("Absolute path to  PID file. Could be overrided by env vars and cmd args")
                  .sinceRelease(new Version(MARIADB_RELEASE))  
            .build()
        );      
        
        kp.put("apl.dir2FA", 
            PropertyRecord.builder()
                  .defaultValue("")
                  .description("Absolute path to 2FAdata dir. Could be overrided by env vars and cmd args")
                  .sinceRelease(new Version(DEX_RELEASE))  
            .build()
        );   
        
        kp.put("apl.customDataExportDir", 
            PropertyRecord.builder()
                  .defaultValue("")
                  .description("Absolute path to custom data export dir (shard files and other data). Could be overrided by env vars and cmd args")
                  .sinceRelease(new Version(SHARDING_RELEASE))  
            .build()
        );   
        
        kp.put("apl.customDexStorageDir", 
            PropertyRecord.builder()
                  .defaultValue("")
                  .description("Absolute path to custom DEX storage. Could be overrided by env vars and cmd args")
                  .sinceRelease(new Version(DEX_RELEASE))  
            .build()
        );        
        return kp;
    }
}
