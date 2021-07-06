/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PeerDTO extends BaseDTO {
    private String address;
    private Integer port;
    private Integer state;
    private String announcedAddress;
    private Boolean sharedAddress;
    private String hallmark;
    private Integer weight;
    private Long downloadedVolume;
    private Long uploadedVolume;
    private String application;
    private String version;
    private String platform;
    private Integer apiPort;
    private Integer apiSSLPort;
    private Boolean blacklisted;
    private Integer lastUpdated;
    private Integer lastConnectAttempt;
    private Boolean inbound;
    private Boolean inboundWebSocket;
    private Boolean outboundWebSocket;
    private String blacklistingCause;
    private List<String> services;
    private String blockchainState;
    private String chainId;
    private Boolean isNewlyAdded;
}
