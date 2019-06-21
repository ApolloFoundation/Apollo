
/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.app.mint.MintWorker;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeMode;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeParams;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;


/**
 * Runtime environment for AplCores (singleton)
 * TODO: make it injectable singleton
 * @author alukin@gmail.com
 */

@Singleton
public class AplCoreRuntime {
    //probably it is temprary solution, we should move WebUI serving out of core

    private static final Logger LOG = LoggerFactory.getLogger(AplCoreRuntime.class);
    private final List<AplCore> cores = new ArrayList<>();
 
    private  RuntimeMode runtimeMode;

    //TODO: may be it is better to take below variables from here instead of getting it from CDI
    // in every class?
    private BlockchainConfig blockchainConfig;
    private PropertiesHolder propertiesHolder;
    
     //TODO:  check and debug minting    
    private MintWorker mintworker;
    private Thread mintworkerThread;
    


    public AplCoreRuntime( ) {
    }

    public void init(RuntimeMode runtimeMode, BlockchainConfig blockchainConfig, PropertiesHolder propertiesHolder) {
        this.blockchainConfig = blockchainConfig;
        this.propertiesHolder = propertiesHolder;
        this.runtimeMode =runtimeMode;
    }
    
    public void addCoreAndInit(){        
        AplCore core = CDI.current().select(AplCore.class).get();
        addCore(core);
        core.init();
    }
    
    public void addCore(AplCore core){
        cores.add(core);
    }
    
    public void shutdown(){
        for(AplCore c: cores){
            c.shutdown();
        }
        if(mintworker!=null){
            mintworker.stop();
            try {
                mintworkerThread.join(200);
            } catch (InterruptedException ex) {              
            }
        }
        runtimeMode.shutdown();
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

    public void startMinter() {
        mintworker = new MintWorker(propertiesHolder, blockchainConfig);
        mintworkerThread = new Thread(mintworker);
        mintworkerThread.setDaemon(true);
        mintworkerThread.start();        
    }
    
    public void stopMinter() {
        if(mintworker!=null){
            mintworker.stop();
        }
    }

}
