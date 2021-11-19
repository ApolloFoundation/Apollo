/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.filters;

import com.apollocurrency.aplwallet.api.dto.auth.TwoFactorAuthParameters;
import com.apollocurrency.aplwallet.apl.core.rest.utils.RestParametersParser;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.exception.ApiErrors;
import com.apollocurrency.aplwallet.apl.util.exception.RestParameterException;
import com.apollocurrency.aplwallet.vault.service.auth.Account2FAService;
import lombok.Setter;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Map;

import static com.apollocurrency.aplwallet.vault.service.auth.Account2FAService.CODE2FA_PARAM_NAME;
import static com.apollocurrency.aplwallet.vault.service.auth.Account2FAService.PASSPHRASE_PARAM_NAME;
import static com.apollocurrency.aplwallet.vault.service.auth.Account2FAService.PUBLIC_KEY_PARAM_NAME;
import static com.apollocurrency.aplwallet.vault.service.auth.Account2FAService.SECRET_PHRASE_PARAM_NAME;
import static com.apollocurrency.aplwallet.vault.service.auth.Account2FAService.TWO_FACTOR_AUTH_PARAMETERS_ATTRIBUTE_NAME;

@Secured2FA
@Provider
@Priority(Priorities.AUTHORIZATION)
public class Secured2FAInterceptor implements ContainerRequestFilter {

    @Context
    ResourceInfo info;
    @Inject
    @Setter
    private Account2FAService faHelper;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (info.getResourceMethod().isAnnotationPresent(Secured2FA.class)) {
            Secured2FA secured2FA = info.getResourceMethod().getAnnotation(Secured2FA.class);
            String vault = secured2FA.value();
            Map<String, String> params = RestParametersParser.parseRequestParameters(requestContext,
                vault,
                SECRET_PHRASE_PARAM_NAME,
                CODE2FA_PARAM_NAME,
                PUBLIC_KEY_PARAM_NAME
                );
            parsePassphraseParam(requestContext, secured2FA, params);
            String code2FAStr = params.get(CODE2FA_PARAM_NAME);
            Integer code2FA = null;
            try {
                code2FA = Integer.parseInt(code2FAStr);
            } catch (NumberFormatException ignored) {
            }

            try {
                TwoFactorAuthParameters twoFactorAuthParameters = faHelper.verify2FA(params.get(vault),
                    params.get(PASSPHRASE_PARAM_NAME),
                    params.get(SECRET_PHRASE_PARAM_NAME),
                    params.get(PUBLIC_KEY_PARAM_NAME),
                    code2FA);
                twoFactorAuthParameters.setCode2FA(code2FA);
                requestContext.setProperty(TWO_FACTOR_AUTH_PARAMETERS_ATTRIBUTE_NAME, twoFactorAuthParameters);
            } catch (RestParameterException e) {
                throw e;
            } catch (Exception e) {
                throw new RestParameterException(ApiErrors.ACCOUNT_2FA_ERROR,
                    String.format("Two factor authentication error, %s:%s",
                        e.getClass().getSimpleName(),
                        e.getMessage()));
            }
        }

    }

    private void parsePassphraseParam(ContainerRequestContext requestContext, Secured2FA secured2FA, Map<String, String> params) {
        String[] passphraseParamNames = secured2FA.passphraseParamNames();
        Map<String, String> passphraseParams = RestParametersParser.parseRequestParameters(requestContext, passphraseParamNames);
        for (String paramName : passphraseParamNames) {
            String passphraseValue = passphraseParams.get(paramName);
            if (StringUtils.isNotBlank(passphraseValue)) {
                params.put(PASSPHRASE_PARAM_NAME, passphraseValue);
                break;
            }
        }
    }

}
