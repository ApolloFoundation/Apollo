
/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import javax.enterprise.inject.spi.CDI;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeMode;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeParams;
import com.apollocurrency.aplwallet.apl.util.env.ServerStatus;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Runtime environment for AplCores (singleton)
 * @author alukin@gmail.com
 */
public class AplCoreRuntime {
    private static Logger LOG = LoggerFactory.getLogger(AplCoreRuntime.class);
    private List<AplCore> cores = new ArrayList<>();
 
    private  RuntimeMode runtimeMode;
    private DirProvider dirProvider;
    private BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();

    private static class AplCoreRuntimeHolder {
        private static final AplCoreRuntime INSTANCE = new AplCoreRuntime();
    } 
    
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
        runtimeMode.setServerStatus(status, wallet, dirProvider.getLogsDir().toFile());
    }
    
    public void shutdown(){
        for(AplCore c: cores){
            c.shutdown();
        }
        runtimeMode.shutdown();
    }

//    public String getDbDir(String dbDir, UUID chainId, boolean chainIdFirst) {
//        return dirProvider.getDbDir(dbDir, chainId, chainIdFirst);
//    }

//    public  String getDbDir(String dbDir, boolean chainIdFirst) {
//        return dirProvider.getDbDir(dbDir, blockchainConfig.getChain().getChainId(), chainIdFirst);
//    }

    //    public  String getDbDir(String dbDir) {
//        return dirProvider.getDbDir(dbDir, blockchainConfig.getChain().getChainId(), false);
//    }
    public Path getDbDir() {
        return dirProvider.getDbDir();
    }

    public Path getVaultKeystoreDir() {
        return dirProvider.getVaultKeystoreDir();
    }

    //    public  Path getKeystoreDir(String keystoreDir) {
//        return dirProvider.getKeystoreDir(keystoreDir).toPath();
//    }
    public Path get2FADir() {
        return dirProvider.get2FADir();
    }
//    public  Path get2FADir(String dir2FA) {
//        return Paths.get(dirProvider.getAppHomeDir(), dir2FA);
//    }

    public String getUserHomeDir() {
        return dirProvider.getAppBaseDir().toString();
    }

    public RuntimeMode getRuntimeMode(){
        return runtimeMode;
    }
    public DirProvider getDirProvider(){
        return dirProvider;
    }
    public static void logSystemProperties() {
        String[] loggedProperties = new String[] {
                "java.version",
                "java.vm.version",
                "java.vm.name",
                "java.vendor",
                "java.vm.vendor",
                "java.home",
                "java.library.path",
                "java.class.path",
                "os.arch",
                "sun.arch.data.model",
                "os.name",
                "file.encoding",
                "java.security.policy",
                "java.security.manager",
                RuntimeEnvironment.RUNTIME_MODE_ARG,
                RuntimeEnvironment.DIRPROVIDER_ARG
        };
        for (String property : loggedProperties) {
            LOG.debug("{} = {}", property, System.getProperty(property));
        }
        LOG.debug("availableProcessors = {}", Runtime.getRuntime().availableProcessors());
        LOG.debug("maxMemory = {}", Runtime.getRuntime().maxMemory());
        LOG.debug("processId = {}", RuntimeParams.getProcessId());
    } 
    
}
