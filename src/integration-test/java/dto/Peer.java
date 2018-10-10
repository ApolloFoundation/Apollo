/*
 * Copyright © 2018 Apollo Foundation
 */

package dto;

import java.util.List;

public class Peer {
    private Long downloadedVolume;
    private String address; //ip
    private Boolean inbound;
    private com.apollocurrency.aplwallet.apl.peer.Peer.BlockchainState blockchainState;
    private Long weight;
    private Long uploadedVolume;
    private List<com.apollocurrency.aplwallet.apl.peer.Peer.Service> services;
    private String version;
    private String platform;
    private Boolean inboundWebSocket;
    private Long lastUpdated;
    private Boolean blacklisted;
    private String announcedAddress;
    private Integer apiPort;
    private String application;
    private Integer port;
    private Boolean outboundWebSocket;
    private Long lastConnectAttempt;
    private Long state;
    private Boolean shareAddress;

    public Long getDownloadedVolume() {
        return downloadedVolume;
    }

    public void setDownloadedVolume(Long downloadedVolume) {
        this.downloadedVolume = downloadedVolume;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Boolean getInbound() {
        return inbound;
    }

    public void setInbound(Boolean inbound) {
        this.inbound = inbound;
    }

    public com.apollocurrency.aplwallet.apl.peer.Peer.BlockchainState getBlockchainState() {
        return blockchainState;
    }

    public void setBlockchainState(com.apollocurrency.aplwallet.apl.peer.Peer.BlockchainState blockchainState) {
        this.blockchainState = blockchainState;
    }

    public Long getWeight() {
        return weight;
    }

    public void setWeight(Long weight) {
        this.weight = weight;
    }

    public Long getUploadedVolume() {
        return uploadedVolume;
    }

    public void setUploadedVolume(Long uploadedVolume) {
        this.uploadedVolume = uploadedVolume;
    }

    public List<com.apollocurrency.aplwallet.apl.peer.Peer.Service> getServices() {
        return services;
    }

    public void setServices(List<com.apollocurrency.aplwallet.apl.peer.Peer.Service> services) {
        this.services = services;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public Boolean getInboundWebSocket() {
        return inboundWebSocket;
    }

    public void setInboundWebSocket(Boolean inboundWebSocket) {
        this.inboundWebSocket = inboundWebSocket;
    }

    public Long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Boolean getBlacklisted() {
        return blacklisted;
    }

    public void setBlacklisted(Boolean blacklisted) {
        this.blacklisted = blacklisted;
    }

    public String getAnnouncedAddress() {
        return announcedAddress;
    }

    @Override
    public String toString() {
        return "Peer{" +
                "address='" + address + '\'' +
                ", blockchainState=" + blockchainState +
                ", lastUpdated=" + lastUpdated +
                ", blacklisted=" + blacklisted +
                ", announcedAddress='" + announcedAddress + '\'' +
                ", apiPort=" + apiPort +
                ", port=" + port +
                '}';
    }

    public void setAnnouncedAddress(String announcedAddress) {
        this.announcedAddress = announcedAddress;
    }

    public Integer getApiPort() {
        return apiPort;
    }

    public void setApiPort(Integer apiPort) {
        this.apiPort = apiPort;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Boolean getOutboundWebSocket() {
        return outboundWebSocket;
    }

    public void setOutboundWebSocket(Boolean outboundWebSocket) {
        this.outboundWebSocket = outboundWebSocket;
    }

    public Long getLastConnectAttempt() {
        return lastConnectAttempt;
    }

    public void setLastConnectAttempt(Long lastConnectAttempt) {
        this.lastConnectAttempt = lastConnectAttempt;
    }

    public Long getState() {
        return state;
    }

    public void setState(Long state) {
        this.state = state;
    }

    public Boolean getShareAddress() {
        return shareAddress;
    }

    public void setShareAddress(Boolean shareAddress) {
        this.shareAddress = shareAddress;
    }
}
