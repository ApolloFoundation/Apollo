/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.pathmap.UriTemplatePathSpec;
import org.eclipse.jetty.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.websocket.server.JettyServerUpgradeResponse;
import org.eclipse.jetty.websocket.server.JettyWebSocketCreator;

import java.util.stream.Collectors;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
@NoArgsConstructor
public class SmcEventSocketCreator implements JettyWebSocketCreator {
    private static final String PATH_SPEC = "org.eclipse.jetty.http.pathmap.PathSpec";
    @Inject
    private SmcEventSocketListener eventServer;

    @Inject
    public SmcEventSocketCreator(SmcEventSocketListener eventServer) {
        this.eventServer = eventServer;
    }

    @Override
    public Object createWebSocket(JettyServerUpgradeRequest req, JettyServerUpgradeResponse resp) {
        String errorMessage;
        var spec = req.getHttpServletRequest().getAttribute(PATH_SPEC);
        if (spec != null) {
            try {
                var pathParams = ((UriTemplatePathSpec) spec).getPathParams(req.getRequestURI().getPath());
                log.debug("Params: {}", pathParams.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.joining(",")));
                var address = pathParams.get("address");
                if (address != null) {
                    return new SmcEventSocket(address, eventServer);
                } else {//should never happen
                    errorMessage = "Wrong path param.";
                }
            } catch (Exception e) {
                errorMessage = e.getClass().getSimpleName() + ":" + e.getMessage();
            }
        } else {
            errorMessage = "Unknown value of the '" + PATH_SPEC + "' request attribute.";
        }
        log.error(errorMessage);
        return new OneErrorSocket(errorMessage);
    }
}
