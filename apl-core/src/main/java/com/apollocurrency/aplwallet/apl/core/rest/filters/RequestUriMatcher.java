/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.filters;

import javax.ws.rs.core.UriInfo;

/**
 * Strategy to match an URI
 *
 * @author andrew.zinchenko@gmail.com
 */
public interface RequestUriMatcher {

    /**
     * Implements the match strategy to the supplied URI
     *
     * @param uriInfo
     * @return true if the URI matches, false otherwise
     */
    boolean matches(UriInfo uriInfo);

}
