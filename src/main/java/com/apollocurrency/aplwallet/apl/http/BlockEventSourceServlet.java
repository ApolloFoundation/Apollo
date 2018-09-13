/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import org.eclipse.jetty.servlets.EventSource;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

public class BlockEventSourceServlet extends org.eclipse.jetty.servlets.EventSourceServlet
{
    public static final Logger LOG = getLogger(BlockEventSourceServlet.class);
    @Override
    protected EventSource newEventSource(HttpServletRequest request)
    {
        BlockEventSource blockEventSource = new BlockEventSource();
        return blockEventSource;
    }


}