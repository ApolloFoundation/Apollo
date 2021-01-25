package com.apollocurrency.aplwallet.apl.core.config;

import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProviderFactory;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProviderFactory;
import com.apollocurrency.aplwallet.apl.util.injectable.ChainsConfigHolder;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class EnvConfig {

    @Produces
    @Singleton
    public DirProvider dirProvider() {
        return DirProviderFactory.getProvider();
    }

//    @Produces
//    @Singleton
    public ConfigDirProvider configDirProvider() {
        return ConfigDirProviderFactory.getConfigDirProvider();
    }

    @Produces
    @Singleton
    public ChainsConfigHolder chainsConfigHolder() {
        return new ChainsConfigHolder();
    }

    @Produces
    @Singleton
    public PropertiesHolder propertiesHolder() {
        return new PropertiesHolder();
    }

    @Produces
    @Named("applicationDirName")
    public String applicationDirName() {
        return Constants.APPLICATION_DIR_NAME;
    }
}
