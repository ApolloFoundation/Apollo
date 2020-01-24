/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.filters;

import com.apollocurrency.aplwallet.apl.core.http.TwoFactorAuthParameters;
import com.apollocurrency.aplwallet.apl.core.rest.utils.Account2FAHelper;

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

import static com.apollocurrency.aplwallet.apl.core.rest.RestParameters.CODE2FA_PARAM_NAME;
import static com.apollocurrency.aplwallet.apl.core.rest.RestParameters.PASSPHRASE_PARAM_NAME;
import static com.apollocurrency.aplwallet.apl.core.rest.RestParameters.SECRET_PHRASE_PARAM_NAME;
import static com.apollocurrency.aplwallet.apl.core.rest.RestParameters.TWO_FCTOR_AUTH_ATTRIBUTE;
import static com.apollocurrency.aplwallet.apl.core.rest.RestParameters.parseRequestParameters;


@Secured2FA
@Provider
@Priority(Priorities.AUTHORIZATION)
public class Secured2FAInterceptor implements ContainerRequestFilter {

    @Inject
    private Account2FAHelper faHelper;

    @Context
    ResourceInfo info;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (info.getResourceMethod().isAnnotationPresent(Secured2FA.class)) {
            Secured2FA secured2FA = info.getResourceMethod().getAnnotation(Secured2FA.class);
            String vault = secured2FA.value();

            Map<String, String> params = parseRequestParameters( requestContext,
                    vault,
                    PASSPHRASE_PARAM_NAME,
                    SECRET_PHRASE_PARAM_NAME,
                    CODE2FA_PARAM_NAME
            );

            Integer code2FA = null;
            try {
                code2FA = Integer.parseInt(params.get(CODE2FA_PARAM_NAME));
            }catch (NumberFormatException e){

            }

            TwoFactorAuthParameters twoFactorAuthParameters = faHelper.verify2FA(
                    params.get(vault),
                    params.get(PASSPHRASE_PARAM_NAME),
                    params.get(SECRET_PHRASE_PARAM_NAME),
                    code2FA
            );

            requestContext.setProperty(TWO_FCTOR_AUTH_ATTRIBUTE, twoFactorAuthParameters);
        }

        return;

    }


}
