/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.util.env.config.ResourceLocator;
import com.apollocurrency.aplwallet.apl.util.env.config.UserResourceLocator;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ResourceLocatorProducer {
    @Inject
    ConfigDirProvider configDirProvider;

    @Produces
    public ResourceLocator locator() {
        return new UserResourceLocator(configDirProvider);
    }
}
