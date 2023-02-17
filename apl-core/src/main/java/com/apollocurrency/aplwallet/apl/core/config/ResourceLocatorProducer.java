/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.config;

import com.apollocurrency.aplwallet.apl.util.env.config.ResourceLocator;
import com.apollocurrency.aplwallet.apl.util.env.config.UserResourceLocator;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Produces singleton CDI-managed instance of the {@link UserResourceLocator}
 * @author Andrii Boiarskyi
 * @see ResourceLocator
 * @see UserResourceLocator
 * @since 1.48.4
 */
@Singleton
public class ResourceLocatorProducer {

    @Inject
    ConfigDirProvider configDirProvider;

    @Produces
    @Singleton
    public ResourceLocator urlResourceLocator() {
        return new UserResourceLocator(configDirProvider);
    }
}
