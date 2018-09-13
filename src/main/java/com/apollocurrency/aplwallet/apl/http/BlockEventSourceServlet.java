/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import org.eclipse.jetty.servlets.EventSource;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class BlockEventSourceServlet extends org.eclipse.jetty.servlets.EventSourceServlet
{
    @Override
    protected EventSource newEventSource(HttpServletRequest request)
    {
        BlockEventSource blockEventSource = new BlockEventSource();
        return blockEventSource;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.addHeader("Access-Control-Allow-Origin", "*");
        super.doGet(request, response);
    }
}