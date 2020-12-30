/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http;

import com.apollocurrency.aplwallet.apl.util.exception.ParameterException;
import org.eclipse.jetty.servlets.EventSource;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ThreadPoolExecutor;

import static org.slf4j.LoggerFactory.getLogger;

public class BlockEventSourceServlet extends org.eclipse.jetty.servlets.EventSourceServlet {
    public static final Logger LOG = getLogger(BlockEventSourceServlet.class);

    @Override
    protected EventSource newEventSource(HttpServletRequest request) {
        long accountId;
        try {
            accountId = HttpParameterParserUtil.getAccountId(request, true);

        } catch (ParameterException e) {
            return null;

        }
        return new BlockEventSource((ThreadPoolExecutor) request
            .getServletContext().getAttribute("executor"), accountId);
    }
}
