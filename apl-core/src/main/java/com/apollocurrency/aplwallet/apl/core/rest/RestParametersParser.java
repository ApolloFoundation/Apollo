/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.model.TwoFactorAuthParameters;
import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterException;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.core.interception.jaxrs.PostMatchContainerRequestContext;
import org.jboss.resteasy.spi.HttpRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.apollocurrency.aplwallet.apl.core.rest.ApiErrors.INCORRECT_VALUE;

@Slf4j
@Singleton
public class RestParametersParser {
    public static final String TWO_FACTOR_AUTH_PARAMETERS_ATTRIBUTE_NAME = "twoFactorAuthParameters";

    public static final String HEIGHT_PARAM_NAME = "height";
    public static final String PASSPHRASE_PARAM_NAME="passphrase";
    public static final String SECRET_PHRASE_PARAM_NAME ="secretPhrase";
    public static final String CODE2FA_PARAM_NAME="code2FA";
    private ObjectMapper mapper = new ObjectMapper();

    private final Blockchain blockchain;

    @Inject
    public RestParametersParser(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    public Map<String, String> parseRequestParameters(ContainerRequestContext requestContext, String ... params) {
        Map<String, String> parsedParams = new HashMap<>();
        HttpRequest httpRequest = ((PostMatchContainerRequestContext) requestContext).getHttpRequest();
        MultivaluedMap<String, String> requestParams = httpRequest.getDecodedFormParameters();
        requestParams.putAll(requestContext.getUriInfo().getQueryParameters(true));
        Arrays.stream(params).forEach(p -> parsedParams.put(p, requestParams.getFirst(p)));
        return parsedParams;
    }

    public static TwoFactorAuthParameters get2FARequestAttribute(org.jboss.resteasy.spi.HttpRequest request) {
        TwoFactorAuthParameters params2FA = (TwoFactorAuthParameters) request.getAttribute(RestParametersParser.TWO_FACTOR_AUTH_PARAMETERS_ATTRIBUTE_NAME);
        if(params2FA == null){
            throw new RestParameterException(ApiErrors.INTERNAL_SERVER_EXCEPTION, "Can't locate the 2FA request attribute.");
        }
        return params2FA;
    }

    public int validateHeight(Integer heightParam){
        int height = null == heightParam? -1: heightParam;
        if ( height > blockchain.getHeight() ){
            throw new RestParameterException(INCORRECT_VALUE, heightParam, HEIGHT_PARAM_NAME);
        }
        return height;
    }

    public int parseHeight(String heightValue, int maxHeight) throws RestParameterException {
        if (heightValue == null){
            return -1;
        }
        int height = parseInt(heightValue, 0, maxHeight, HEIGHT_PARAM_NAME);
        return height;
    }

    public int parseInt(String intStrValue, int min, int max, String paramName) throws RestParameterException {
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

    public int parseInt(int intValue, int min, int max, String paramName) throws RestParameterException {
        if (intValue < min || intValue > max) {
            throw new RestParameterException(ApiErrors.INCORRECT_VALUE, paramName, intValue);
        }
        return intValue;
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
