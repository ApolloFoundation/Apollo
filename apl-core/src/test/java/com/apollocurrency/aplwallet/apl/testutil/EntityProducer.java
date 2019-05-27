/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.apl.testutil;

import com.apollocurrency.aplwallet.apl.core.http.APIEnum;
import com.apollocurrency.aplwallet.apl.core.peer.BlockchainState;
import com.apollocurrency.aplwallet.apl.core.peer.Hallmark;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.util.Version;
import java.math.BigInteger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.Set;
import java.util.UUID;

public class EntityProducer {


    public static Peer createPeer(final String host, final String announcedAddress, boolean active, final long supportServices){
        final Peer.State state = active ? Peer.State.CONNECTED: Peer.State.NON_CONNECTED;

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
            public State getState() {
                return state;
            }

            @Override
            public Version getVersion() {
                return new Version(1,30,17);
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
            public void deactivate() {

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
            public boolean isInboundWebSocket() {
                return false;
            }

            @Override
            public boolean isOutboundWebSocket() {
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
            public JSONObject send(JSONStreamAware request, UUID chainId, int maxResponseSize, boolean firstConnect) {
                return null;
            }

            @Override
            public String getHostWithPort() {
                return null;
            }

            @Override
            public void handshake(UUID targetChainId) {
               
            }

            @Override
            public boolean isTrusted() {
                return false;
            }

            @Override
            public Peer.TrustLevel getTrustLevel() {
                return Peer.TrustLevel.NOT_TRUSTED;
            }

        };
        return peer;
    }

}
