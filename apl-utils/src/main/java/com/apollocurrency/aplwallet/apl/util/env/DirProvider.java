/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;

public class DirProvider {
    public static String APPLICATION_NAME="apollo";
    public static String APPLICATION_DIR_NAME="."+APPLICATION_NAME;
    
    private boolean serviceMode;
    
    public DirProvider(boolean isServiceMode) {
        serviceMode = isServiceMode;
    }
    
    public boolean isServiceMode(){
        return serviceMode;
    }
    
    public void updateLogFileHandler(Properties loggingProperties){};

    public String getDbDir(String dbRelativeDir, UUID chainId, boolean chainIdFirst) {
        String chainIdDir = chainId == null ? "" : String.valueOf(chainId);
        Path dbDirRelativePath = Paths.get(dbRelativeDir);
        Path userHomeDirPath = Paths.get(getAppHomeDir());
        Path dbPath;
        if (chainIdFirst) {
            dbPath = userHomeDirPath
                    .resolve(chainIdDir)
                    .resolve(dbDirRelativePath);
        } else {
            dbPath = userHomeDirPath
                    .resolve(dbDirRelativePath)
                    .resolve(chainIdDir);
        }
        return dbPath.toString();
    }
    
    /**
     * Directory for log files for certain platform and run mode
     * @return File denoting path to directory where logs should go
     */
    public File getLogFileDir() {
        return new File(getAppHomeDir(), "logs");
    }
    
    /**
     * Directory where keys are stored
     * @param keystoreDir relative path from default key store directory
     * @return File denoting path to key store directory
     */
    public File getKeystoreDir(String keystoreDir) {
        return new File(getAppHomeDir(), keystoreDir);
    } 
    
    /**
     * Application hone inside of user home directory on all platforms.
     * @return Path to application dir inside of user home
     */
    public String getAppHomeDir() {
        return System.getProperty("user.home") + File.separator + APPLICATION_DIR_NAME;
    }
     /**
     * User home directory on all platforms. It is user home and NEVER nothing else.
     * @return File denoting path to user home
     */   
    public String getUserHomeDir() {
       return System.getProperty("user.home"); 
    }
   /**
    * Path to directory where main executable jar file is placed
    * @return File denoting path to directory with main executable jar
    */
    public File getBinDirectory() {
        String res = this.getClass().getClassLoader().getResource("").getPath();
        return new File(res);
    }
    /**
     * Path to system config directory, depends on OS
     * @return Path to system config directory
     */
    public String getSysConfigDirectory() {
        return "/etc/"+APPLICATION_NAME;
    }
    /**
     * Path to user's config directory, depends on OS
     * @return Path to user's config directory
     */
    public String getUserConfigDirectory() {
        return getAppHomeDir()+"/conf";
    }
}
