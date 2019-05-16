/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *  
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.PeerDTO;
import com.apollocurrency.aplwallet.api.response.ResponseBase;
import com.apollocurrency.aplwallet.apl.core.http.APIEnum;
import com.apollocurrency.aplwallet.apl.core.peer.Hallmark;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PeerConverterTest {

    private static ObjectMapper mapper = new ObjectMapper();

    @Test
    void givenPeerDTO_includeNonNullAnnotation_whenDo_thenCorrect() throws JsonProcessingException {

        Peer peer = createPeer("192.168.2.68", "10.10.10.10");
        assertNotNull(peer);

        PeerConverter converter = new PeerConverter();

        PeerDTO dto  = converter.convert(peer);
        assertNotNull(dto);

        convertToJson(dto);
    }

    @Test
    void givenResponseBase_includeNonNullAnnotation_whenDo_thenCorrect() throws JsonProcessingException {

        ResponseBase response = new ResponseBase();
        assertNotNull(response);

        convertToJson(response);

    }

    private void convertToJson(Object object) throws JsonProcessingException {

        String stringJSON = mapper.writeValueAsString(object);

        assertFalse(stringJSON.contains("null"), String.format("Given value contains NULL: %s",stringJSON));
    }

    private Peer createPeer(final String host, final String announcedAddress){
        Peer peer = new Peer() {
            @Override
            public int compareTo(Peer o) {
                return 0;
            }

            @Override
            public boolean providesService(Service service) {
                return false;
            }

            @Override
            public boolean providesServices(long services) {
                return false;
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
                return State.CONNECTED;
            }

            @Override
            public Version getVersion() {
                return null;//new Version(1,30,17);
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
                return false;
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
        };
        return peer;
    }
}