
/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.util.env.DirProvider;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeMode;
import com.apollocurrency.aplwallet.apl.util.env.ServerStatus;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;


/**
 * Runtime environment for AplCores (singleton)
 * @author alukin@gmail.com
 */
public class AplCoreRuntime {
    private List<AplCore> cores = new ArrayList<>();
 
    private  RuntimeMode runtimeMode;
    private  DirProvider dirProvider;
    
    private AplCoreRuntime() {
    }
    
    public void setup(RuntimeMode runtimeMode, DirProvider dirProvider){
        this.runtimeMode =runtimeMode;
        this.dirProvider = dirProvider;
    }
    
    public void addCore(AplCore core){
        cores.add(core);
    }
    
    public static AplCoreRuntime getInstance() {
        return AplCoreRuntimeHolder.INSTANCE;
    }

    void setServerStatus(ServerStatus status, URI wallet) {
        runtimeMode.setServerStatus(status, wallet, dirProvider.getLogFileDir());
    }
    
    private static class AplCoreRuntimeHolder {
        private static final AplCoreRuntime INSTANCE = new AplCoreRuntime();
    }
    public void shutdown(){
        for(AplCore c: cores){
            c.shutdown();
        }
        runtimeMode.shutdown();
    }



    public String getDbDir(String dbDir, UUID chainId, boolean chainIdFirst) {
        return dirProvider.getDbDir(dbDir, chainId, chainIdFirst);
    }

    public  String getDbDir(String dbDir, boolean chainIdFirst) {
        return dirProvider.getDbDir(dbDir, AplGlobalObjects.getChainConfig().getChain().getChainId(), chainIdFirst);
    }

    public  String getDbDir(String dbDir) {
        return dirProvider.getDbDir(dbDir, AplGlobalObjects.getChainConfig().getChain().getChainId(), false);
    }

    public  Path getKeystoreDir(String keystoreDir) {
        return dirProvider.getKeystoreDir(keystoreDir).toPath();
    }

    public  Path get2FADir(String dir2FA) {
        return Paths.get(dirProvider.getUserHomeDir(), dir2FA);
    }


    public void updateLogFileHandler(Properties loggingProperties) {
        dirProvider.updateLogFileHandler(loggingProperties);
    }

    public String getUserHomeDir() {
        return dirProvider.getUserHomeDir();
    }

    public  File getConfDir() {
        return dirProvider.getConfDir();
    }
  
    public File getLogDir() {
        return dirProvider.getLogFileDir();
    }
    public RuntimeMode getRuntimeMode(){
        return runtimeMode;
    }
    public DirProvider getDirProvider(){
        return dirProvider;
    }
}
