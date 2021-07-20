/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.config;

import com.apollocurrency.aplwallet.apl.core.rest.filters.RequestUriMatcher;
import com.apollocurrency.aplwallet.apl.core.rest.filters.UriPatternMatcher;

import javax.enterprise.inject.Produces;
import javax.inject.Named;

public class RestSecurityConfig {

    @Produces
    @Named("excludeProtection")
    public RequestUriMatcher createExcludePathMatcher() {
        return new UriPatternMatcher("*/openapi.json");
    }

    @Produces
    @Named("includeProtection")
    public RequestUriMatcher createIncludePathMatcher() {
        return new UriPatternMatcher("/failed-txs/*", "/failed-txs");
    }

}
