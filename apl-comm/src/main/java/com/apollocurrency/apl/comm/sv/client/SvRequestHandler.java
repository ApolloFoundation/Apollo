/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.apl.comm.sv.client;

import com.apollocurrency.apl.comm.sv.msg.SvBusRequest;
import com.apollocurrency.apl.comm.sv.msg.SvBusResponse;
import com.apollocurrency.apl.comm.sv.msg.SvChannelHeader;

/**
 * incoming message (request) handler prototype
 *
 * @author alukin@gmail.com
 */
public interface SvRequestHandler {

    /**
     * handle command synchronously
     *
     * @param request
     * @param header
     * @return some reply message
     */
    public SvBusResponse handleRequest(SvBusRequest request, SvChannelHeader header);
}
