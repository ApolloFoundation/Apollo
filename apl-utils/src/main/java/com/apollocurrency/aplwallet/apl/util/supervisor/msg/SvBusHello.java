package com.apollocurrency.aplwallet.apl.util.supervisor.msg;

/**
 * Hello message to be sent by any client on connection.
 * It is needed to say server client's address in header and other info
 * @author alukin@gmail.con
 */
public class SvBusHello extends SvBusRequest{
    public Long clientPID;
    public String clientExePath; 
    public String clientAddr;
    public String clientInfo;
}
