/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import org.slf4j.Logger;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class ChainIdFilter implements javax.servlet.Filter {
    private static final Logger log = getLogger(ChainIdFilter.class);

    private BlockchainConfig config;

    public ChainIdFilter(BlockchainConfig config) {
        this.config = config;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("Init chainid filter");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }

    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String peerChainId = request.getParameter("chainId");
        log.debug("Peer chainId: {}", peerChainId);
        if (peerChainId == null) {
            response.sendError(422, "Missing chainId parameter");
            return;
        }
        String ourChainId = config.getChain().getChainId().toString();
        if (!ourChainId.equalsIgnoreCase(peerChainId)) {
            response.sendError(422, "Chain id is not correct");
            return;
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}
