package com.apollocurrency.aplwallet.apl.core.http;

import lombok.Setter;
import org.jboss.resteasy.core.ResourceMethodInvoker;

import java.lang.reflect.Method;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class SecurityInterceptor implements javax.ws.rs.container.ContainerRequestFilter
{
    @Inject
    @Setter
    private AdminPasswordVerifier adminPasswordVerifier;
    @Context
    private HttpServletRequest request;

    @Override
    public void filter(ContainerRequestContext requestContext)
    {
        ResourceMethodInvoker methodInvoker = (ResourceMethodInvoker) requestContext.getProperty("org.jboss.resteasy.core.ResourceMethodInvoker");
        Method method = methodInvoker.getMethod();
        if (method.isAnnotationPresent(AdminSecured.class)) {
            Response errorResponse = adminPasswordVerifier.verifyPasswordWithoutException(request);
            if (errorResponse != null) {
                requestContext.abortWith(errorResponse);
            }
        }
    }
}