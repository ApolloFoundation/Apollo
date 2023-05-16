/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.p2p;

import com.apollocurrency.aplwallet.api.p2p.response.BaseP2PResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Info about peer that is requested on connection
 *
 * @author alukin@gmail.com
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PeerInfo extends BaseP2PResponse {
    /**
     * Application name. Used to check if
     * this application is compatible to other similar applications.
     * Should be matched exactly.
     */
    private String application;
    /**
     * Coma separated list of services provided by application.
     */
    private String services;
    /**
     * Current version of application. Rule of version numbering
     * is standard 3-number. Example: 1.3.5
     * First one is major release number, second - minor release
     * number, third - bugfix edition.
     */
    private String version;

    /**
     * Platform that application is running on
     */
    private String platform;

    /**
     * Is this application address should be visible externally?
     */
    private Boolean shareAddress;

    /**
     * Address, which we get from undelying connection layer.
     */
    private String address;

    /**
     * Address which node announced. Announced address should match address which we get
     * from undelying connection layer. Otherwise this node should be blacklisted.
     */
    private String announcedAddress;

    /**
     * Weight is used for blockchain downloading, getting more peers and retrieving
     * transactions from peers. See Peers.getWeightedPeer or Peers.getAnyPeer
     */
    private Integer weight;

    /**
     * Hallmark is some kind of signature that verifies bost belongings.
     * Should not be used.
     */
    private String hallmark;

    /**
     * Port of API accessible via HTTP protocol.
     * If <-0, then port is unavailable.
     */
    private Integer apiPort = 0;

    /**
     * Port of API accessible via HTTPS protocol.
     * If <-0, then port is unavailable.
     */
    private Integer apiSSLPort = 0;

    /**
     * Coma-separated list of APIs, dosabled on peer.
     */
    private String disabledAPIs;

    /**
     *
     */
    private Integer apiServerIdleTimeout = 0;

    /**
     * Bytes, already downloaded from network
     */
    private Long downloadedVolume = 0L;

    /**
     * Bytes, already uploaded to network
     */
    private Long uploadedVolume = 0L;

    /**
     *
     */
    private Integer lastUpdated = 0;

    /**
     *
     */
    private Boolean blacklisted;

    /**
     *
     */
    private String blacklistingCause;

    /**
     *
     */
    private Integer state = 0;

    /**
     * Flag indicating that this peer has inbound connection to our peer.
     */
    private Boolean inbound;

    /**
     * Flag, indication that inbound connection of peer is using Web socket
     */
    private Boolean inboundWebSocket;

    /**
     * Flag indication that outbound connection is made via Web socket
     */
    private Boolean outboundWebSocket;

    /**
     * Last connection attempt time
     */
    private Integer lastConnectAttempt = 0;

    /**
     * State of blockchain
     */
    private Integer blockchainState = 0;

    /**
     * Default block generation time from config.
     * TODO: do we need this at all? May be move to
     */
    private Integer blockTime;

    /**
     * ID of blockchain. This ID used to distinguish and separate
     * networks with the same application
     * New!
     */
    private String chainId = null;

    /**
     * PEM-encoded X.509 Certificate of host as String, including
     * BEGIN-CETIFICATE and END-CERTIFICATE marks. Certificate
     * should be verified and used for private key cryptography. Certificate
     * also contains some important attributes, that is used widely in communications.
     * New!
     */
    private String X509_cert;
    /**
     * blacklist cause
     */
    private String cause;
    /**
     * port for external connection
     */
    private Integer port;
}
