/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.utils;

import com.apollocurrency.aplwallet.api.dto.auth.TwoFactorAuthParameters;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.exception.ApiErrors;
import com.apollocurrency.aplwallet.apl.util.exception.RestParameterException;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.core.interception.jaxrs.PostMatchContainerRequestContext;

import javax.enterprise.inject.Vetoed;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.apollocurrency.aplwallet.vault.service.auth.Account2FAService.TWO_FACTOR_AUTH_PARAMETERS_ATTRIBUTE_NAME;

@Slf4j
@Vetoed
public class RestParametersParser {

    private RestParametersParser() {
    }

    public static Map<String, String> parseRequestParameters(ContainerRequestContext requestContext, String... params) {
        Map<String, String> parsedParams = new HashMap<>();
        MultivaluedMap<String, String> requestParams;
        requestParams = ((PostMatchContainerRequestContext) requestContext).getHttpRequest().getDecodedFormParameters();
        requestParams.putAll(requestContext.getUriInfo().getQueryParameters(true));

        Arrays.stream(params).forEach(p -> parsedParams.put(p, requestParams.getFirst(p)));

        return parsedParams;
    }

    public static TwoFactorAuthParameters get2FARequestAttribute(org.jboss.resteasy.spi.HttpRequest request) {
        TwoFactorAuthParameters params2FA = (TwoFactorAuthParameters) request.getAttribute(TWO_FACTOR_AUTH_PARAMETERS_ATTRIBUTE_NAME);
        if (params2FA == null) {
            throw new RestParameterException(ApiErrors.INTERNAL_SERVER_EXCEPTION, "Can't locate the 2FA request attribute.");
        }
        return params2FA;
    }

    public static int parseInt(String intStrValue, int min, int max, String paramName) throws RestParameterException {
        if (intStrValue != null && !intStrValue.isBlank()) {
            try {
                int intValue = Integer.parseInt(intStrValue);
                return validateInt(intValue, min, max, paramName);
            } catch (NumberFormatException e) {
                throw new RestParameterException(ApiErrors.INCORRECT_VALUE, paramName, intStrValue);
            }
        }
        return min;
    }

    public static Integer parseInt(Integer intValue, int min, int max, String paramName) throws RestParameterException {
        if (intValue != null) {
            if (intValue < min || intValue > max) {
                throw new RestParameterException(ApiErrors.INCORRECT_VALUE, paramName, intValue);
            }
            return intValue;
        }
        return min;
    }

    public static int parseInt(String paramValue, String paramName, int min, int max, boolean isMandatory) throws RestParameterException {
        if (paramValue == null || paramValue.isEmpty()) {
            if (isMandatory) {
                throw new RestParameterException(ApiErrors.MISSING_PARAM, paramName);
            }
//            return 0;
            return min; // let's return minimal permitted
        }
        try {
            int value = Integer.parseInt(paramValue);
            if (value < min || value > max) {
                throw new RestParameterException(ApiErrors.OUT_OF_RANGE_NAME_VALUE, paramName, value, min, max);
            }
            return value;
        } catch (RuntimeException e) {
            throw new RestParameterException(ApiErrors.INCORRECT_VALUE, paramName, paramValue);
        }
    }

    public static int validateInt(int intValue, int min, int max, String paramName) throws RestParameterException {
        if (intValue < min || intValue > max) {
            throw new RestParameterException(ApiErrors.INCORRECT_VALUE, paramName, intValue);
        }
        return intValue;
    }

    public static long parseAccountId(String account) throws RestParameterException {
        long accountId;
        if (account == null) {
            throw new RestParameterException(ApiErrors.MISSING_PARAM, "account");
        }
        try {
            accountId = Convert.parseAccountId(account);
            if (accountId == 0) {
                throw new NumberFormatException();
            }
        } catch (Exception e) {
            throw new RestParameterException(ApiErrors.UNKNOWN_VALUE, "account", account);
        }
        return accountId;
    }

}
