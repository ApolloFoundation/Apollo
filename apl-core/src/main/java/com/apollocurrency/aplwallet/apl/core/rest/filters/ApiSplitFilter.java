/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.filters;

import com.apollocurrency.aplwallet.apl.core.rest.NewApiRegistry;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;

/**
 * This filter routes request calls to new jax-rs based endpoints or to old ones
 * if new is not yet implemented.
 *
 * @author alukin@gmail.com
 */
@Slf4j
public class ApiSplitFilter implements Filter {

    /**
     * this is just a "fuse" to disable API calls while core is starting. Should
     * be removed as soon as all API will be on RestEasy
     */
    public static boolean isCoreReady = false;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest rq = (HttpServletRequest) request;

        //logRequest(rq);
        HttpServletResponse resp = (HttpServletResponse) response;
        // Set response values now in case we create an asynchronous context
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);
        //to fix CORS
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");

        String rqType = request.getParameter("requestType");
        if (log.isTraceEnabled()) {
            log.trace("========= RequestType IS {} ==========", null == rqType ? "EMPTY!" : rqType);
        }

        String forwardUri = NewApiRegistry.getRestPath(rqType);
        //forward to new API, it should be ready always because it is on CDI and
        //does not require completion of old static init() methods
        if (forwardUri != null && !forwardUri.isEmpty()) {
            log.trace("Request " + rqType + " forwarded to: " + forwardUri);
            rq.getRequestDispatcher(forwardUri).forward(request, response);
            return;
        }
        if (!isCoreReady) {
            // Core is not signaled that is is ready to serve requests,
            // so old API implementation shoud wait
            rq.getRequestDispatcher("/starting.html").forward(request, response);
            resp.sendError(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), "Application is starting, please wait!");
            return;
        }

        chain.doFilter(request, resp);
    }

    @Override
    public void destroy() {

    }

    private void logRequest(HttpServletRequest rq) {
        if (log.isTraceEnabled()) {
            log.trace("Request from: {} Method: {} User: {}\n\t Request URI: {} \n\t Request session ID: {}",
                    rq.getRemoteAddr(), rq.getMethod(), rq.getRemoteUser(), rq.getRequestURI(), rq.getRequestedSessionId());
            //print all headers
            Enumeration<String> hdre = rq.getHeaderNames();
            StringBuilder hdrs = new StringBuilder();
            while (hdre.hasMoreElements()) {
                String name = hdre.nextElement();
                hdrs.append("\n\tName:>").append(name)
                        .append("< Value:>").append(rq.getHeader(name)).append("<");
            }
            log.trace("HTTP request headers:{}", hdrs);
            //print all request parameters
            Map<String, String[]> params = rq.getParameterMap();
            String ps = "";
            ps = params.keySet().stream().map(k -> "\n\t" + "Name:>" + k + "< Value: >" + Arrays.toString(params.get(k))).reduce(ps, String::concat) + "<";
            log.trace("Request parameters: {}", ps);
        }
    }

}
