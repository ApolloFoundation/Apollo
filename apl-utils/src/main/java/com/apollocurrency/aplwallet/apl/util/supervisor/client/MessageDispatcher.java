/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.supervisor.client;

import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusMessage;

/**
 * Public interface to message dispatcher. Though dispatcher is a core of
 * messaging subsystem, this interface contains registration of processing
 * routines only.
 *
 * @author alukin@gmail.com
 */
public interface MessageDispatcher {

    /**
     * path to error handler. If not handler is not defined, default logging
     * handler is used
     */
    public static final String ERROR_PATH = "/error";

    /**
     * Register handler for incoming messages
     *
     * @param pathSpec path specification as for JAX-RX REST services
     * @param rqMapping mapping class for request
     * @param respMapping mapping class for response
     * @param handler handler routine
     */
    public void registerRqHandler(String pathSpec, Class<? extends SvBusMessage> rqMapping, Class<? extends SvBusMessage> respMapping, SvRequestHandler handler);

    /**
     * Unregister handler for incoming messages
     *
     * @param pathSpec exactly as in registration call
     */
    public void unregisterRqHandler(String pathSpec);
}
