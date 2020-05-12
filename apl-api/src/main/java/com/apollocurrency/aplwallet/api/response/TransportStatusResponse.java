/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)


@Getter
@Setter
public class TransportStatusResponse extends ResponseBase {
    @Schema(name = "controlconnection", description = "Connection between core and transport status")
    public Boolean controlconnection;
    @Schema(name = "remoteconnectionstatus", description = "Connection between core and transport status")
    public String remoteConnectionStatus;
    @Schema(name = "remoteip", description = "remote server that I am connected to")
    public String remoteip;
    @Schema(name = "remoteport", description = "port to connect to")
    public int remoteport;
    @Schema(name = "tunaddr", description = "tunnel address to connect to")
    public String tunaddr;
    @Schema(name = "tunnetmask", description = "netmask for the tunnel interface")
    public String tunnetmask;
}