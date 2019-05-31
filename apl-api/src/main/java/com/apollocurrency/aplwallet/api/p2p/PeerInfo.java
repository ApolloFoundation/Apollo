/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.p2p;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Info about peer that is requested on connection
 * @author alukin@gmail.com
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PeerInfo extends BaseP2PResonse{
    /**
     * Application name. Used to check if
     * this application is compatible to other similar applications.
     * Should be matched exactly.
     */
    public String application;
    /**
     * Coma separated list of services provided by application.
     */
    public String services;
    /**
     * Current version of application. Rule of version numbering
     * is standard 3-number. Example: 1.3.5
     * First one is major release number, second - minor release
     * number, third - bugfix edition. 
     */
    public String version;
    
    /**
     * Platform that application is running on
     */
    public String platform;
    
    /**
     * Is this application address should be visible externally?
     */
    public Boolean shareAddress;
    
    /**
     * Address, which we get from undelying connection layer.
     */
    public String address;
    
    /**
     * Address which node announced. Announced address should match address which we get
     * from undelying connection layer. Otherwise this node should be blacklisted.     
     */
    public String announcedAddress; 
    
    /**
     * Weight is used for blockchain downloading, getting more peers and retrieving 
     * transactions from peers. See Peers.getWeightedPeer or Peers.getAnyPeer
     */
    public Integer weight;

    /**
     * Hallmark is some kind of signature that verifies bost belongings.
     * Should not be used.
     */
    public String hallmark;
    
    /**
     * Port of API accessible via HTTP protocol.
     * If <-0, then port is unavailable.
     */
    public Integer apiPort=0;
    
    /**
     * Port of API accessible via HTTPS protocol.
     * If <-0, then port is unavailable.
     */
    public Integer apiSSLPort=0;
    
    /**
     * Coma-separated list of APIs, dosabled on peer.
     */
    public String disabledAPIs;
    
    /**
     * 
     */
    public Integer apiServerIdleTimeout=0;
    
    /**
     * Bytes, already downloaded from network
     */
    public Long downloadedVolume=0L;
    
    /**
     * Bytes, already uploaded to network
     */
    public Long uploadedVolume=0L;
    
    /**
     * 
     */
    public Integer lastUpdated=0;
    
    /**
     * 
     */
    public Boolean blacklisted;
    
    /**
     * 
     */
    public String blacklistingCause;
    
    /**
     * 
     */
    public Integer state=0;
    
    /**
     * Flag indicating that this peer has inbound connection to our peer.
     */
    public Boolean inbound;
    
    /**
     * Flag, indication that inbound connection of peer is using Web socket
     */
    public Boolean inboundWebSocket;
    
    /**
     * Flag indication that outbound connection is made via Web socket
     */
    public Boolean outboundWebSocket;
    
    /**
     * Last connection attempt time
     */
    public Integer lastConnectAttempt=0;

    /**
     * State of blockchain
     */
    public Integer blockchainState=0;
    
    /**
     * Default block generation time from config.
     * TODO: do we need this at all? May be move to 
     */
    public Integer blockTime;
    
    /**
     * ID of blockchain. This ID used to distinguish and separate
     * networks with the same application
     * New!
     */    
    public String chainId=null;

    /**
     * PEM-encoded X.509 Certificate of host as String, including
     * BEGIN-CETIFICATE and END-CERTIFICATE marks. Certificate 
     * should be verified and used for public key cryptography. Certificate
     * also contains some important attributes, that is used widely in communications.
     * New!
     */
    public String X509_cert;

    @Override
    public String toString() {
        return "PeerInfo{" +
                "application='" + application + '\'' +
                ", services='" + services + '\'' +
                ", version='" + version + '\'' +
                ", platform='" + platform + '\'' +
                ", address='" + address + '\'' +
                ", weight=" + weight +
                ", apiPort=" + apiPort +
                ", apiSSLPort=" + apiSSLPort +
                ", lastUpdated=" + lastUpdated +
                ", blacklisted=" + blacklisted +
                ", state=" + state +
                ", blockchainState=" + blockchainState +
                ", blockTime=" + blockTime +
                ", chainId=" + chainId +
                '}';
    }
}
