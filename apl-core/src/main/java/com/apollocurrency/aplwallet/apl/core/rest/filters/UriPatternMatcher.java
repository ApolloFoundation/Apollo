/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.filters;

import jakarta.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.List;

/**
 * Tests if the requested path matches the pattern.
 * <p>
 * Patterns may have formats: * , {@code *<uri> }, {@code <uri>*}, {@code <uri>}
 */
public class UriPatternMatcher implements RequestUriMatcher {

    private List<String> patterns;

    public UriPatternMatcher(String... patterns) {
        this(Arrays.asList(patterns));
    }

    public UriPatternMatcher(List<String> patterns) {
        this.patterns = patterns;
    }

    @Override
    public boolean matches(UriInfo uriInfo) {
        String path = uriInfo.getPath(true);
        for (String pattern : patterns) {
            if (matchUriRequestPattern(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchUriRequestPattern(String pattern, String path) {
        if (pattern.equals("*")) {
            return true;
        } else {
            if (pattern.endsWith("*")) {
                return path.startsWith(pattern.substring(0, pattern.length() - 1));
            } else if (pattern.startsWith("*")) {
                return path.endsWith(pattern.substring(1));
            } else {
                return path.equals(pattern);
            }
        }
    }
}
