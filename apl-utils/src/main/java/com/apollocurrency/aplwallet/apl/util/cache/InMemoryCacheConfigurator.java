/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.cache;

import java.util.List;

public interface InMemoryCacheConfigurator {

    /**
     * @return the total amount of memory currently available for configured cache objects, measured in bytes.
     */
    long getAvailableMemory();

    /**
     * The list of cache configurations. The total capacity of all caches is less then value returned by the
     * <code>getAvailableMemory</code> method.
     * @return the list of cache configurations
     */
    List<CacheConfiguration> getConfiguredCaches();

}
