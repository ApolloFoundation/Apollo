
/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeMode;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeParams;
import com.apollocurrency.aplwallet.apl.util.env.ServerStatus;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.CDI;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


/**
 * Runtime environment for AplCores (singleton)
 * @author alukin@gmail.com
 */
public class AplCoreRuntime {
    //probably it is temprary solution, we should move WebUI serving out of core
    public final static String WEB_UI_DIR="webui";
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

    public Path getDbDir() {
        return dirProvider.getDbDir();
    }

    public Path getVaultKeystoreDir() {
        return dirProvider.getVaultKeystoreDir();
    }

    public Path get2FADir() {
        return dirProvider.get2FADir();
    }

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
        };
        for (String property : loggedProperties) {
            LOG.debug("{} = {}", property, System.getProperty(property));
        }
        LOG.debug("availableProcessors = {}", Runtime.getRuntime().availableProcessors());
        LOG.debug("maxMemory = {}", Runtime.getRuntime().maxMemory());
        LOG.debug("processId = {}", RuntimeParams.getProcessId());
    } 
    
    public String findWebUiDir(){
// if we decide to unzip in runtime
//        String dir = dirProvider.getAppHomeDir()+File.separator+WEB_UI_DIR;
        String dir = dirProvider.getBinDirectory()+File.separator+WEB_UI_DIR;
        dir=dir+File.separator+"build";
        File res = new File(dir);
        if(!res.exists()){ //we are in develop IDE or tests
            dir=dirProvider.getBinDirectory()+"/apl-exec/target/"+WEB_UI_DIR+"/build";
            res=new File(dir);
        }
        return res.getAbsolutePath();
    }
}
