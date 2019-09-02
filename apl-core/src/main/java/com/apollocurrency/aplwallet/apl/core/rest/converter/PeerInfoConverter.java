package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.PeerDTO;
import com.apollocurrency.aplwallet.api.p2p.PeerInfo;
import com.apollocurrency.aplwallet.apl.core.peer.PeerState;

import java.util.ArrayList;
import java.util.List;

public class PeerInfoConverter implements Converter<PeerInfo, PeerDTO> {

    @Override
    public PeerDTO apply(PeerInfo peer) {
        PeerDTO dto = new PeerDTO();
        dto.setAddress(peer.getAddress());
        dto.setPort(peer.getPort());
        dto.setState(PeerState.CONNECTED.ordinal());
        dto.setAnnouncedAddress(peer.getAnnouncedAddress());
        dto.setSharedAddress(peer.getShareAddress());
        dto.setWeight(peer.getWeight());
        dto.setDownloadedVolume(peer.getDownloadedVolume());
        dto.setUploadedVolume(peer.getUploadedVolume());
        dto.setApplication(peer.getApplication());
        dto.setVersion(peer.getVersion() == null ? null : peer.getVersion());
        dto.setPlatform(peer.getPlatform());
        if (peer.getApiPort() != 0) {
            dto.setApiPort(peer.getApiPort());
        }
        if (peer.getApiSSLPort() != 0) {
            dto.setApiSSLPort(peer.getApiSSLPort());
        }
        dto.setBlacklisted(false);
        dto.setLastUpdated(peer.getLastUpdated());
        dto.setLastConnectAttempt(peer.getLastConnectAttempt());
        dto.setInbound(false);
        dto.setInboundWebSocket(false);
        dto.setOutboundWebSocket(false);

        List<String> availableServices = new ArrayList<>();
        if (!availableServices.isEmpty()) {
            dto.setServices(availableServices);
        }

        dto.setBlockchainState(null);
        dto.setChainId(peer.getChainId() == null ? null : peer.getChainId());
        return dto;

    }
}
