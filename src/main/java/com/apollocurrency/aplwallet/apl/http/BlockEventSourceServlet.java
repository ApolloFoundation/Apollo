/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import static org.slf4j.LoggerFactory.getLogger;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ThreadPoolExecutor;

import org.eclipse.jetty.servlets.EventSource;
import org.slf4j.Logger;

public class BlockEventSourceServlet extends org.eclipse.jetty.servlets.EventSourceServlet {
    public static final Logger LOG = getLogger(BlockEventSourceServlet.class);

    @Override
    protected EventSource newEventSource(HttpServletRequest request) {
        long accountId;
        try {
            accountId = ParameterParser.getAccountId(request, true);

        } catch (ParameterException e) {
            return null;

        }
        return new BlockEventSource((ThreadPoolExecutor) request
                .getServletContext().getAttribute("executor"), accountId);
    }
}