/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.apl.comm.sv.client;

import com.apollocurrency.apl.comm.sv.msg.SvBusMessage;
import com.apollocurrency.apl.comm.sv.msg.SvBusResponse;

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
    String ERROR_PATH = "/error";

    /**
     * Register handler for incoming messages
     *
     * @param pathSpec path specification as for JAX-RX REST services
     * @param rqMapping mapping class for request
     * @param respMapping mapping class for response
     * @param handler handler routine
     */
    void registerRqHandler(String pathSpec, Class<? extends SvBusMessage> rqMapping, Class<? extends SvBusMessage> respMapping, SvRequestHandler handler);

    /**
     * Register response mapping class for specified path
     * @param pathSpec path for which given response class should be mapped
     * @param respClass class to which response for given path should be mapped
     */
    void registerResponseMapping(String pathSpec, Class<? extends SvBusResponse> respClass);

    /**
     * Register response mapping class for specifed path, which is parametrized by another class
     * @param pathSpec path for which given response mapping is intended
     * @param responseClass base parametrized mapping class
     * @param paramClass parameter class for base class
     */
    void registerParametrizedResponseMapping(String pathSpec, Class<? extends SvBusResponse> responseClass, Class<?> paramClass);

    /**
     * Unregister handler for incoming messages
     *
     * @param pathSpec exactly as in registration call
     */
     void unregisterRqHandler(String pathSpec);
}
