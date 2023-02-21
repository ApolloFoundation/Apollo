/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.filters;

import com.apollocurrency.aplwallet.apl.core.http.AdminPasswordVerifier;
import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.ServerResponse;

import jakarta.annotation.Priority;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.apollocurrency.aplwallet.apl.core.http.AdminPasswordVerifier.ADMIN_ROLE;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class SecurityInterceptor implements ContainerRequestFilter {
    private static final ServerResponse ACCESS_DENIED = new ServerResponse("Access denied for this resource", Response.Status.UNAUTHORIZED.getStatusCode(), new Headers<Object>());
    private static final ServerResponse ACCESS_FORBIDDEN = new ServerResponse("Nobody can access this resource", Response.Status.FORBIDDEN.getStatusCode(), new Headers<Object>());
    @Context
    ResourceInfo info;
    @Context
    private UriInfo uriInfo;
    @Context
    private HttpServletRequest request;

    @Inject
    private AdminPasswordVerifier apw;

    @Inject
    @Named("excludeProtection")
    private RequestUriMatcher excludeProtectionUriMatcher;

    @Inject
    @Named("includeProtection")
    private RequestUriMatcher includeProtectionUriMatcher;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (excludeProtectionUriMatcher.matches(uriInfo)) { //resource is not protected
            return;
        }
        if (includeProtectionUriMatcher.matches(uriInfo)) { // protect resources by url
            Response response = apw.verifyPasswordWithoutException(request);
            if (response != null) {
                requestContext.abortWith(response);
            }
            return;
        }

        Method method = info.getResourceMethod();
        Class<?> resourceClass = info.getResourceClass();
        /*if (true){ return; }*/ // don't remove, use for debugging

        //Access allowed for all
        if (resourceClass.isAnnotationPresent(PermitAll.class)) {
            return;
        }
        //Access denied for all
        if (resourceClass.isAnnotationPresent(DenyAll.class)) {
            requestContext.abortWith(ACCESS_FORBIDDEN);
            return;
        }

        //Access allowed for all
        if (method.isAnnotationPresent(PermitAll.class)) {
            return;
        }
        //Access denied for all
        if (method.isAnnotationPresent(DenyAll.class)) {
            requestContext.abortWith(ACCESS_FORBIDDEN);
            return;
        }

        if (apw.isDisabledAdminPassword()) {
            //Access allowed for all
            return;
        }

        //Verify user access
        if (method.isAnnotationPresent(RolesAllowed.class)) {
            RolesAllowed rolesAnnotation = method.getAnnotation(RolesAllowed.class);
            String[] roles = rolesAnnotation.value();
            Set<String> rolesSet = new HashSet<String>();
            Arrays.stream(roles).forEach(role -> rolesSet.add(role.toLowerCase()));
            Response response = apw.verifyPasswordWithoutException(request);
            if (response != null) {
                requestContext.abortWith(response);
                return;
            }
            if (!rolesSet.contains(ADMIN_ROLE)) {
                requestContext.abortWith(ACCESS_DENIED);
                return;
            }
        }
    }

}
