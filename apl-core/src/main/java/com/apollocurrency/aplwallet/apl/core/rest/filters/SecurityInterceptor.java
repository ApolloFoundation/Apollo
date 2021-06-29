/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.filters;

import com.apollocurrency.aplwallet.apl.core.http.AdminPasswordVerifier;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.firstbridge.kms.infrastructure.web.resource.WorkMode;
import io.firstbridge.kms.security.KmsMainConfig;
import io.firstbridge.kms.security.TokenProvider;
import io.firstbridge.kms.security.context.JWTSecurityContext;
import io.firstbridge.kms.security.provider.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.core.interception.jaxrs.PostMatchContainerRequestContext;

import javax.annotation.Priority;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.apollocurrency.aplwallet.apl.core.http.AdminPasswordVerifier.ADMIN_ROLE;

@Slf4j
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

    private TokenProvider tokenProvider;
    private KmsMainConfig kmsMainConfig;
    private boolean isRemoteServerMode;

/*    @Inject
    public SecurityInterceptor(@Context ResourceInfo info,
                               @Context UriInfo uriInfo,
                               @Context HttpServletRequest request,
                               AdminPasswordVerifier apw,
                               @Named("excludeProtection") RequestUriMatcher excludeProtectionUriMatcher,
                               TokenProvider tokenProvider,
                               KmsMainConfig kmsMainConfig) {
//        this.info = info;
//        this.uriInfo = uriInfo;
//        this.request = request;
        this.apw = apw;
        this.excludeProtectionUriMatcher = excludeProtectionUriMatcher;
        this.tokenProvider = tokenProvider;
        this.kmsMainConfig = kmsMainConfig;
    }*/

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (excludeProtectionUriMatcher.matches(uriInfo)) { //resource is not protected
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

        boolean isKmsController = ((PostMatchContainerRequestContext) requestContext).getResourceMethod().getResourceClass().getCanonicalName().contains(".kms.");
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (isKmsController && authorizationHeader != null && !authorizationHeader.isEmpty()) {
            if (tokenProvider == null) {
                this.kmsMainConfig = CDI.current().select(KmsMainConfig.class).get(); // KMS
                this.tokenProvider = new JwtTokenProvider(this.kmsMainConfig);
                this.isRemoteServerMode = kmsMainConfig.getRemoteKmsConfig().getRemoteServerModeOn().equals(WorkMode.REMOTE_SERVER);
            }
            // skip token check for remote KMS server
            String token = authorizationHeader.substring(tokenProvider.authSchema().length() + 1);//including one extra space symbol
            try {
                DecodedJWT decodedJWT = tokenProvider.verify(token);
                requestContext.setSecurityContext(new JWTSecurityContext(decodedJWT, tokenProvider, request.isSecure()));
            } catch (JWTVerificationException ex) {
                log.warn("{}, wrong/unverified token. Exception: {}", ACCESS_DENIED.getEntity().toString(), ex.getMessage());
                requestContext.abortWith(ACCESS_DENIED);
            }
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
