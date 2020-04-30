
package com.apollocurrency.aplwallet.apl.util.supervisor.client;

import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusMessage;




/**
 * Public interface to message dispatcher.
 * Though dispatcher is a core of messaging subsystem, this interface contains
 * registration of processing routines only.
 * @author alukin@gmail.com
 */
public interface MessageDispatcher {
    /**
     * path to error handler.
     * If not handler is not defined, default logging handler is used
     */
    public static final String ERROR_PATH="/error";
    
    
    public void registerRqHandler(String pathSpec, Class<? extends SvBusMessage> rqMapping, Class<? extends SvBusMessage> respMapping, SvRequestHandler handler);    
    public void unregisterRqHandler(String pathSpec);
}
