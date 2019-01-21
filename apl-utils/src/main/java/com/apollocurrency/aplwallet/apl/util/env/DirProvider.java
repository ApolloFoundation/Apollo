/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    * Path to directory where aproject is installed or top project dir if we're in IDE or tests
    * TODO: maybe find some better solution
    * @return File denoting path to directory with main executable jar
    */
    public File getBinDirectory() {
        String res="./";
        try { //get location of this class
            String path = DirProvider.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            res = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException ex) {          
        }
        // remove jar name or "classes". Should be location jar directory
        res = new File(res).getParentFile().getAbsolutePath();
        File ret;
        if(res.endsWith("target"+File.separator+"lib")){ //we are in dev env or IDE
            ret = new File(res).getParentFile().getParentFile().getParentFile();
        }else if (res.endsWith("target")){
            ret = new File(res).getParentFile().getParentFile(); //we are in dev env or IDE
        }else{ //we are installed
            ret = new File(res).getParentFile();
        }
        return ret;
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
