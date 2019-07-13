/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest;

import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterException;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
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

    public static int parseHeight(String heightValue, long maxHeight) throws RestParameterException {
        if (heightValue != null) {
            try {
                int height = Integer.parseInt(heightValue);
                if (height < 0 || height > maxHeight) {
                    throw new NumberFormatException();
                }
                return height;
            } catch (NumberFormatException e) {
                throw new RestParameterException(ApiErrors.INCORRECT_VALUE, "height", heightValue);
            }
        }
        return -1;
    }

    public static long parseAccountId(String account) throws RestParameterException {
        long accountId;
        if (account == null) {
            throw new RestParameterException( ApiErrors.MISSING_PARAM, "account");
        }
        try{
            accountId = Convert.parseAccountId(account);
            if (accountId == 0){
                throw new NumberFormatException();
            }
        }catch (Exception e){
            throw new RestParameterException(ApiErrors.UNKNOWN_VALUE, "account", account);
        }
        return accountId;
    }


}
