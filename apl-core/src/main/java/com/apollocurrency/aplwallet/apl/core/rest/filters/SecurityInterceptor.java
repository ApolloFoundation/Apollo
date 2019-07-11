/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.filters;

import com.apollocurrency.aplwallet.apl.core.http.AdminPasswordVerifier;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterException;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.core.interception.PostMatchContainerRequestContext;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.apollocurrency.aplwallet.apl.core.http.AdminPasswordVerifier.ADMIN_ROLE;

@Provider
public class SecurityInterceptor implements ContainerRequestFilter {
    private static final ServerResponse ACCESS_DENIED = new ServerResponse("Access denied for this resource", 401, new Headers<Object>());;
    private static final ServerResponse ACCESS_FORBIDDEN = new ServerResponse("Nobody can access this resource", 403, new Headers<Object>());;

    @Context
    private UriInfo uriInfo;

    @Context
    ResourceInfo info;

    @Inject
    private AdminPasswordVerifier apw;

    @Inject @Named("excludeProtection")
    private RequestUriMatcher excludeProtectionUriMatcher;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (excludeProtectionUriMatcher.matches(uriInfo)){ //resource is not protected
            return;
        }

        Method method = info.getResourceMethod();
        /*if (true){ return; }*/ // don't remove, use for debugging

        //Access allowed for all
        if( method.isAnnotationPresent(PermitAll.class)) {
            return;
        }
        //Access denied for all
        if(method.isAnnotationPresent(DenyAll.class)) {
            requestContext.abortWith(ACCESS_FORBIDDEN);
            return;
        }

        if(apw.isDisableAdminPassword()){
            //Access allowed in properties file
            return;
        }

        if(apw.isBlankAdminPassword()) {
            throw new RestParameterException(ApiErrors.INTERNAL_SERVER_EXCEPTION, "The admin password is undefined, set the 'apl.adminPassword' property value.");
        }

        String remoteHost = null;
        //Get remoteHost
        if ( apw.getForwardedForHeader() != null ) {
            remoteHost = requestContext.getHeaders().getFirst(apw.getForwardedForHeader());
        }
        if (remoteHost == null){
            remoteHost = uriInfo.getRequestUri().getHost();
        }

        //Get parameters, retrieve password param
        MultivaluedMap<String, String> params;
        String username = "user";
        String password = null;
        params = ((PostMatchContainerRequestContext) requestContext).getHttpRequest().getDecodedFormParameters();
        params.putAll(requestContext.getUriInfo().getQueryParameters(true));

        List<String> passwords = params.get(AdminPasswordVerifier.ADMIN_PASSWORD_PARAMETER_NAME);
        if (!passwords.isEmpty()){
            password = passwords.get(0);
        }

        //If no authorization information present; block access
        if(StringUtils.isBlank(password)) {
            requestContext.abortWith(ACCESS_DENIED);
            return;
        }

        //Verify user access
        if(method.isAnnotationPresent(RolesAllowed.class)){
            RolesAllowed rolesAnnotation = method.getAnnotation(RolesAllowed.class);
            String[] roles = rolesAnnotation.value();
            Set<String> rolesSet = new HashSet<String>();
            Arrays.stream(roles).forEach(role-> rolesSet.add(role.toLowerCase()));
            if( ! isUserAllowed(remoteHost, username, password, rolesSet)){
                requestContext.abortWith(ACCESS_DENIED);
                return;
            }
        }
    }

    private boolean isUserAllowed(final String remoteHost, final String username, final String password, final Set<String> rolesSet)
    {
        boolean isAllowed;
        try {
            apw.checkOrLockPassword(password, remoteHost);
            isAllowed = rolesSet.contains(ADMIN_ROLE);
        } catch (ParameterException e) {
            return false;
        }

        return isAllowed;
    }

}
