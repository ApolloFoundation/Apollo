/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.apl.comm.sv.msg;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Hello message to be sent by any client on connection. It is needed to say
 * server client's address in header and other info
 *
 * @author alukin@gmail.con
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SvBusHello extends SvBusRequest {

    public Long clientPID;
    public String clientExePath;
    public String clientAddr;
    public String clientInfo;
}
