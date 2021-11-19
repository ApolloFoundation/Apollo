/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbConfig;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeMode;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProviderFactory;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.ChainsConfigHolder;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;


/**
 * Runtime environment for AplCores (singleton)
 *
 * @author alukin@gmail.com
 */
@Singleton
public class AplCoreRuntime {
    private static final Logger LOG = LoggerFactory.getLogger(AplCoreRuntime.class);
    private final List<AplCore> cores = new ArrayList<>();
    private RuntimeMode runtimeMode;
    private TaskDispatchManager taskDispatchManager;

    private PropertiesHolder propertieHolder;
    private ChainsConfigHolder chainsConfigHolder;
    private DbConfig dbConfig;
    private DirProvider dirProvider;
    private BlockchainConfig blockchainConfig;


    public AplCoreRuntime() {
    }


    @Produces @ApplicationScoped
    public PropertiesHolder getPropertieHolder() {
        return propertieHolder;
    }

    @Produces @ApplicationScoped
    public ChainsConfigHolder getChainsConfigHolder() {
        return chainsConfigHolder;
    }

    @Produces @ApplicationScoped
    public DbConfig getDbConfig() {
        return dbConfig;
    }

    @Produces @Singleton
    public DirProvider getDirProvider() {
        return dirProvider;
    }
    @Produces @Singleton
    public ConfigDirProvider configDirProvider() {
        return ConfigDirProviderFactory.getConfigDirProvider();
    }
    @Produces   @ApplicationScoped
    public TaskDispatchManager getTaskDispatchManager(){
        return taskDispatchManager;
    }
    @Produces @ApplicationScoped
    public BlockchainConfig getBlockchainConfig(){
        return blockchainConfig;
    }

    public void init(RuntimeMode runtimeMode,
                     DirProvider dirProvider,
                     Properties properties,
                     Map<UUID, Chain> chains
                     )
    {
        this.runtimeMode = runtimeMode;
        this.propertieHolder = new PropertiesHolder(properties);
        this.taskDispatchManager = new TaskDispatchManager(propertieHolder);
        this.dirProvider = dirProvider;
        this.chainsConfigHolder = new ChainsConfigHolder(chains);
        Chain chain = chainsConfigHolder.getActiveChain();
        this.dbConfig = new DbConfig(propertieHolder, chainsConfigHolder);
        this.blockchainConfig = new BlockchainConfig(chain, propertieHolder);

    }


    public void addCoreAndInit() {
        AplCore core = CDI.current().select(AplCore.class).get();
        addCore(core);
        core.init();
    }

    public void addCore(AplCore core) {
        cores.add(core);
    }

    public void shutdown() {
        for (AplCore c : cores) {
            c.shutdown();
        }
        runtimeMode.shutdown();
    }

}
