/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.Block;
import com.apollocurrency.aplwallet.apl.Blockchain;
import org.eclipse.jetty.servlets.EventSource;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

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
        BlockEventSource blockEventSource = new BlockEventSource((ThreadPoolExecutor) request
                .getServletContext().getAttribute("executor"), accountId);
        return blockEventSource;
    }
}