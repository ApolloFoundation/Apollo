/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest;

import org.jboss.resteasy.core.interception.PostMatchContainerRequestContext;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RestParameters {
    public static final String TWO_FCTOR_AUTH_ATTRIBUTE = "twoFactorAuthParameters";

    public static final String PASSPHRASE_PARAM_NAME="passphrase";
    public static final String SECRET_PHRASE_PARAM_NAME ="secretPhrase";
    public static final String CODE2FA_PARAM_NAME="code2FA";

    public static Map<String, String> parseRequestParameters(ContainerRequestContext requestContext, String ... params){
        Map<String, String> parsedParams = new HashMap<>();
        MultivaluedMap<String, String> requestParams;
        requestParams = ((PostMatchContainerRequestContext) requestContext).getHttpRequest().getDecodedFormParameters();
        requestParams.putAll(requestContext.getUriInfo().getQueryParameters(true));

        Arrays.stream(params).forEach(p -> parsedParams.put(p, requestParams.getFirst(p)));

        return parsedParams;
    }



}
