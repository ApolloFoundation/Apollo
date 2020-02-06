/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterException;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.core.interception.jaxrs.PostMatchContainerRequestContext;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Singleton
public class RestParametersParser {
    public static final String TWO_FCTOR_AUTH_ATTRIBUTE = "twoFactorAuthParameters";

    public static final String PASSPHRASE_PARAM_NAME="passphrase";
    public static final String SECRET_PHRASE_PARAM_NAME ="secretPhrase";
    public static final String CODE2FA_PARAM_NAME="code2FA";

    private final Blockchain blockchain;

    @Inject
    public RestParametersParser(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    public Map<String, String> parseRequestParameters(ContainerRequestContext requestContext, String ... params){
        Map<String, String> parsedParams = new HashMap<>();
        MultivaluedMap<String, String> requestParams;
        requestParams = ((PostMatchContainerRequestContext) requestContext).getHttpRequest().getDecodedFormParameters();
        requestParams.putAll(requestContext.getUriInfo().getQueryParameters(true));

        Arrays.stream(params).forEach(p -> parsedParams.put(p, requestParams.getFirst(p)));

        return parsedParams;
    }

    public Integer parseHeight(String heightValue, int maxHeight) throws RestParameterException {
        if (heightValue == null){
            return -1;
        }
        Integer height = parseInt(heightValue, 0, maxHeight, "height");
        return height;
    }

    public Integer parseInt(String intStrValue, int min, int max, String paramName) throws RestParameterException {
        if (intStrValue != null) {
            try {
                int intValue = Integer.parseInt(intStrValue);
                return parseInt(intValue, min, max, paramName);
            } catch (NumberFormatException e) {
                throw new RestParameterException(ApiErrors.INCORRECT_VALUE, paramName, intStrValue);
            }
        }
        return min;
    }

    public Integer parseInt(Integer intValue, int min, int max, String paramName) throws RestParameterException {
        if (intValue != null) {
            if (intValue < min || intValue > max) {
                throw new RestParameterException(ApiErrors.INCORRECT_VALUE, paramName, intValue);
            }
            return intValue;
        }
        return min;
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
