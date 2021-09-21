/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.pathmap.UriTemplatePathSpec;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

import java.util.stream.Collectors;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class SmcEventSocketCreator implements WebSocketCreator {
    private static final String PATH_SPEC = "org.eclipse.jetty.http.pathmap.PathSpec";
    private final SmcEventServer eventServer;

    public SmcEventSocketCreator(SmcEventServer eventServer) {
        this.eventServer = eventServer;
    }

    @Override
    public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
        var spec = req.getHttpServletRequest().getAttribute(PATH_SPEC);
        if (spec != null) {
            var pathParams = ((UriTemplatePathSpec) spec).getPathParams(req.getRequestURI().getPath());
            log.debug("Params: {}", pathParams.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.joining(",")));
            var address = pathParams.get("address");
            return new SmcEventSocket(address, req.getHttpServletRequest(), eventServer);
        } else {
            final String msg = "Unknown value of the '" + PATH_SPEC + "' request attribute.";
            log.error(msg);
            throw new WebSocketException(msg);
        }
    }
}
