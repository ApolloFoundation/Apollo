/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author alukin@gmail.com
 */

@Schema(name = "NodeNetworkingInfo", description = "Information about backend networkinig")
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NodeNetworkingInfo {
    @Schema(name = "MyPeerInfo ", description = "Info about this P2P node")
    public PeerDTO myPeerInfo;
    @Schema(name = "IboundPeers", description = "Number of inbound peers")
    public Integer inboundPeers = 0;
    @Schema(name = "OutboundPeers", description = "Number of outbound peers")
    public Integer outboundPeers = 0;
}
