/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.apl.testutil;

import com.apollocurrency.aplwallet.api.p2p.request.BaseP2PRequest;
import com.apollocurrency.aplwallet.apl.core.http.APIEnum;
import com.apollocurrency.aplwallet.apl.core.peer.BlockchainState;
import com.apollocurrency.aplwallet.apl.core.peer.Hallmark;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.Peer2PeerTransport;
import com.apollocurrency.aplwallet.apl.core.peer.PeerNotConnectedException;
import com.apollocurrency.aplwallet.apl.core.peer.PeerState;
import com.apollocurrency.aplwallet.apl.core.peer.PeerTrustLevel;
import com.apollocurrency.aplwallet.apl.core.peer.parser.JsonReqRespParser;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import com.apollocurrency.aplwallet.apl.util.Version;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import java.util.Set;
import java.util.UUID;

public class EntityProducer {

    public static Peer createPeer(final String host, final String announcedAddress, boolean active, final long supportServices) {
        final PeerState state = active ? PeerState.CONNECTED : PeerState.NON_CONNECTED;

        Peer peer = new Peer() {
            @Override
            public int compareTo(Peer o) {
                return 0;
            }

            @Override
            public boolean providesService(Service service) {
                return providesServices(service.getCode());
            }

            @Override
            public boolean providesServices(long services) {
                return (services & supportServices) == services;
            }

            @Override
            public String getHost() {
                return host;
            }

            @Override
            public int getPort() {
                return 0;
            }

            @Override
            public String getAnnouncedAddress() {
                return announcedAddress;
            }

            @Override
            public PeerState getState() {
                return state;
            }

            @Override
            public Version getVersion() {
                return new Version(1, 30, 17);
            }

            @Override
            public String getApplication() {
                return null;
            }

            @Override
            public String getPlatform() {
                return null;
            }

            @Override
            public String getSoftware() {
                return null;
            }

            @Override
            public int getApiPort() {
                return 0;
            }

            @Override
            public int getApiSSLPort() {
                return 0;
            }

            @Override
            public Set<APIEnum> getDisabledAPIs() {
                return null;
            }

            @Override
            public int getApiServerIdleTimeout() {
                return 0;
            }

            @Override
            public BlockchainState getBlockchainState() {
                return BlockchainState.UP_TO_DATE;
            }

            @Override
            public Hallmark getHallmark() {
                return null;
            }

            @Override
            public int getWeight() {
                return 0;
            }

            @Override
            public boolean shareAddress() {
                return false;
            }

            @Override
            public boolean isBlacklisted() {
                return false;
            }

            @Override
            public void blacklist(Exception cause) {

            }

            @Override
            public void blacklist(String cause) {

            }

            @Override
            public void unBlacklist() {

            }

            @Override
            public void deactivate(String reason) {

            }

            @Override
            public void remove() {

            }

            @Override
            public long getDownloadedVolume() {
                return 0;
            }

            @Override
            public long getUploadedVolume() {
                return 0;
            }

            @Override
            public int getLastUpdated() {
                return 0;
            }

            @Override
            public int getLastConnectAttempt() {
                return 0;
            }

            @Override
            public boolean isInbound() {
                return false;
            }

            @Override
            public boolean isOpenAPI() {
                return true;
            }

            @Override
            public boolean isApiConnectable() {
                return false;
            }

            @Override
            public UUID getChainId() {
                return null;
            }

            @Override
            public StringBuilder getPeerApiUri() {
                return null;
            }

            @Override
            public String getBlacklistingCause() {
                return null;
            }

            @Override
            public JSONObject send(JSONStreamAware request, UUID chainId) {
                return null;
            }

            @Override
            public JSONObject send(BaseP2PRequest request) throws PeerNotConnectedException {
                return null;
            }

            @Override
            public <T> T send(BaseP2PRequest request, JsonReqRespParser<T> parser) throws PeerNotConnectedException {
                return null;
            }

            @Override
            public String getHostWithPort() {
                return null;
            }

            @Override
            public boolean isTrusted() {
                return false;
            }

            @Override
            public PeerTrustLevel getTrustLevel() {
                return PeerTrustLevel.NOT_TRUSTED;
            }

            @Override
            public void sendAsync(BaseP2PRequest request) {

            }

            @Override
            public boolean isOutbound() {
                return false;
            }

            @Override
            public long getServices() {
                return 0L;
            }

            @Override
            public long getLastActivityTime() {
                return 0;
            }

            @Override
            public Peer2PeerTransport getP2pTransport() {
                return null;
            }

            @Override
            public boolean processError(JSONObject request) {
                return false;
            }

            @Override
            public void setServices(long code) {

            }

            @Override
            public void setLastUpdated(int time) {

            }

            @Override
            public boolean isInboundSocket() {
                return false;
            }

            @Override
            public boolean isOutboundSocket() {
                return false;
            }

        };
        return peer;
    }

    @Produces
    @Named("CREATOR_ID")
    public long getCreatorId() { //only for tests, instead of using the GenesisImporter component.
        return AccountTestData.CREATOR_ID;
    }

}
