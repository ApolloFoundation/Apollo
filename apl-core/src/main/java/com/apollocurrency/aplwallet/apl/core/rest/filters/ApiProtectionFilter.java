/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.filters;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Some endpoints should be available to loopback
 * interface only (localhost) so this filter also checks request source
 *
 * @author alukin@gmail.com
 */
public class ApiProtectionFilter implements Filter {
    String[] protectedPaths = {"control"};

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest rq = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = rq.getPathInfo();
        String addr = rq.getRemoteAddr();
        String host = rq.getRemoteHost();
        if (isProtected(path) && !isLocal(host, addr)) {
            resp.sendError(Response.Status.FORBIDDEN.getStatusCode(), "Assess is allowed from localhost only!");
        }
        chain.doFilter(request, resp);
    }

    private boolean isProtected(String path) {
        boolean res = false;
        for (String p : protectedPaths) {
            if (path != null && path.endsWith(p)) {
                res = true;
                break;
            }
        }
        return res;
    }

    private boolean isLocal(String host, String ipAddr) {
        boolean res = false;
        if (host != null && ipAddr != null && (host.equalsIgnoreCase("localhost")
            || ipAddr.equalsIgnoreCase("127.0.0.1")
            || ipAddr.endsWith("0:1")
            || ipAddr.endsWith("::1")
        )) {
            res = true;
        }
        return res;
    }

    @Override
    public void destroy() {
    }

}
