/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.apl.comm.sv.msg;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Date;

/**
 * This class represents "envelope" header format for all messages going trough
 * messaging systems of Apollo Supervisor Null fields ignored on serializing.
 *
 * @author alukinb@gmail.conm
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SvChannelHeader {

    /**
     * Each message has random unique ID
     */
    public Long messageId;
    /**
     * If message is not request but response it should have this set to message
     * Id of request. If it is null, message is request or just message that
     * does not require response.
     */
    public Long inResponseTo;
    /**
     * Often we have to route request to some REST API or use publish/subscribe
     * magic for messages. Type of request or message going to this path is
     * fixed and we interpret it inside of appropriate processing routine. with
     * path parameters and request parameters
     */
    public String path;

    public String from;
    public String to;
    public String routedBy;
    public Date from_timestamp;
    public Date route_timestamp;

}
