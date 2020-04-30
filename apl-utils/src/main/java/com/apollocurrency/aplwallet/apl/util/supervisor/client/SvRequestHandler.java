
package com.apollocurrency.aplwallet.apl.util.supervisor.client;

import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusRequest;
import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusResponse;
import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvChannelHeader;

/**
 *
 * @author al
 */
public interface SvRequestHandler {
    /** handle command synchronously
     * 
     * @param request
     * @param header
     * @return some reply message
     */
    public SvBusResponse handleRequest(SvBusRequest request, SvChannelHeader header);
}
