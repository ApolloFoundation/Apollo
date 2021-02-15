/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.PeerDTO;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;

import java.util.ArrayList;
import java.util.List;

public class PeerConverter implements Converter<Peer, PeerDTO> {

    @Override
    public PeerDTO apply(Peer peer) {
        PeerDTO dto = new PeerDTO();
        dto.setAddress(peer.getHost());
        dto.setPort(peer.getPort());
        dto.setState(peer.getState().ordinal());
        dto.setAnnouncedAddress(peer.getAnnouncedAddress());
        dto.setSharedAddress(peer.shareAddress());
        if (peer.getHallmark() != null) {
            dto.setHallmark(peer.getHallmark().getHallmarkString());
        }
        dto.setWeight(peer.getWeight());
        dto.setDownloadedVolume(peer.getDownloadedVolume());
        dto.setUploadedVolume(peer.getUploadedVolume());
        dto.setApplication(peer.getApplication());
        dto.setVersion(peer.getVersion() == null ? null : peer.getVersion().toString());
        dto.setPlatform(peer.getPlatform());
        if (peer.getApiPort() != 0) {
            dto.setApiPort(peer.getApiPort());
        }
        if (peer.getApiSSLPort() != 0) {
            dto.setApiSSLPort(peer.getApiSSLPort());
        }
        dto.setBlacklisted(peer.isBlacklisted());
        dto.setLastUpdated(peer.getLastUpdated());
        dto.setLastConnectAttempt(peer.getLastConnectAttempt());
        dto.setInbound(peer.isInbound());
        dto.setInboundWebSocket(peer.isInboundSocket());
        dto.setOutboundWebSocket(peer.isOutboundSocket());
        if (peer.isBlacklisted()) {
            dto.setBlacklistingCause(peer.getBlacklistingCause());
        }

        List<String> availableServices = new ArrayList<>();
        for (Peer.Service service : Peer.Service.values()) {
            if (peer.providesService(service)) {
                availableServices.add(service.name());
            }
        }
        if (!availableServices.isEmpty()) {
            dto.setServices(availableServices);
        }

        dto.setBlockchainState(peer.getBlockchainState().name());
        dto.setChainId(peer.getChainId() == null ? null : peer.getChainId().toString());
        return dto;

    }
}
