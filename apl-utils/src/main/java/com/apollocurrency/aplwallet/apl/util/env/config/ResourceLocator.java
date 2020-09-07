/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.env.config;

import java.io.InputStream;
import java.util.Optional;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
public interface ResourceLocator {
    Optional<InputStream> locate(String resourceName);
}
