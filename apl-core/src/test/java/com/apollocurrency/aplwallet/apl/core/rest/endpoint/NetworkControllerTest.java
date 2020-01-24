/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.PeerDTO;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.rest.converter.PeerConverter;
import com.apollocurrency.aplwallet.apl.core.rest.service.NetworkService;
import com.apollocurrency.aplwallet.apl.testutil.EntityProducer;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class NetworkControllerTest extends AbstractEndpointTest {
    private static final String PEER_ADDRESS = "192.168.2.68";
    private static final String WRONG_PEER_ADDRESS = "10.0.0.1";
    private static final String ANNOUNCED_ADDRESS = "10.10.10.10";

    private static Peer peer = EntityProducer.createPeer(PEER_ADDRESS, ANNOUNCED_ADDRESS, true, 0);

    private NetworkService service = mock(NetworkService.class);

    @BeforeEach
    void setUp() {
        super.setUp();
        NetworkController endpoint = new NetworkController(new PeerConverter(), service);
        dispatcher.getRegistry().addSingletonResource(endpoint);
    }

    @AfterEach
    void tearDown() {

    }

    @Test
    void getPeer_whenCallWithoutMandatoryParameter_thenGetError_2003() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/networking/peer");

        checkMandatoryParameterMissingErrorCode(response, 2003);
    }

    @Test
    void getPeer_whenCallWithUnknownPeer_thenGetError_2005() throws URISyntaxException, IOException {
        when(service.findPeerByAddress(WRONG_PEER_ADDRESS)).thenReturn(null);

        MockHttpResponse response = sendGetRequest("/networking/peer?peer="+ WRONG_PEER_ADDRESS);

        checkMandatoryParameterMissingErrorCode(response, 2005);
    }

    @Test
    void getPeer_whenCallWithParameter_thenGetPeer() throws URISyntaxException, IOException {
        when(service.findPeerByAddress(PEER_ADDRESS)).thenReturn(peer);

        MockHttpResponse response = sendGetRequest("/networking/peer?peer="+ PEER_ADDRESS);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        Map result = mapper.readValue(content, Map.class);
        assertEquals(PEER_ADDRESS, result.get("address"));
    }

    @Test
    void getPeersList_whenGetWithWrongState_thenGetError_2004() throws URISyntaxException, IOException {
        when(service.getPeersByStateAndService(false, null, 0)).thenReturn(List.of(peer));

        MockHttpResponse response = sendGetRequest("/networking/peer/all?state=BAD_STATE_OF_PEER&includePeerInfo=true");

        checkMandatoryParameterMissingErrorCode(response, 2004);
    }

    @Test
    void getPeersList_whenSetActive_thenGetEmptyList() throws URISyntaxException, IOException {
        when(service.getPeersByStateAndService(true, null, 0)).thenReturn(Collections.EMPTY_LIST);

        MockHttpResponse response = sendGetRequest("/networking/peer/all?active=true");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        JsonNode root = mapper.readTree(content);
        //check an empty array
        assertTrue(root.get("peers").isArray());
        assertFalse(root.withArray("peers").elements().hasNext());
    }

    @Test
    void getPeersList_whenSetIncludePeerInfo_thenGetPeersList() throws URISyntaxException, IOException {
        when(service.getPeersByStateAndService(false, null, 0)).thenReturn(List.of(peer));

        MockHttpResponse response = sendGetRequest("/networking/peer/all?includePeerInfo=true");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        JsonNode root = mapper.readTree(content);
        assertTrue(root.get("peers").isArray());
        assertEquals(PEER_ADDRESS, root.withArray("peers").get(0).get("address").asText());
    }

    @Test
    void getPeersList_whenUnSetIncludePeerInfo_thenGetHostsList() throws URISyntaxException, IOException {
        when(service.getPeersByStateAndService(false, null, 0)).thenReturn(List.of(peer));

        MockHttpResponse response = sendGetRequest("/networking/peer/all");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        JsonNode root = mapper.readTree(content);
        assertTrue(root.get("peers").isArray());
        assertEquals(PEER_ADDRESS, root.withArray("peers").get(0).asText());
    }

    @Test
    void addOrReplacePeer() throws URISyntaxException, IOException {
        when(service.findOrCreatePeerByAddress(PEER_ADDRESS)).thenReturn(peer);

        MockHttpResponse response = sendPostRequest("/networking/peer", "peer="+PEER_ADDRESS);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:"+result.get("newErrorCode"));

        PeerDTO dto = mapper.readValue(content, PeerDTO.class);
        assertNotNull(dto);
        assertEquals(PEER_ADDRESS, dto.getAddress());
        assertEquals(ANNOUNCED_ADDRESS, dto.getAnnouncedAddress());
    }

    @Test
    void addOrReplacePeer_whenCallWithoutMandatoryParam_thenGetError_2003() throws URISyntaxException, IOException {
        MockHttpResponse response = sendPostRequest("/networking/peer", "missedParam=value");

        checkMandatoryParameterMissingErrorCode(response, 2003);
    }

    @Test
    void addOrReplacePeer_whenCallWithWrongPeer_thenGetError_2008() throws URISyntaxException, IOException {
        when(service.findOrCreatePeerByAddress(WRONG_PEER_ADDRESS)).thenReturn(null);

        MockHttpResponse response = sendPostRequest("/networking/peer", "peer="+WRONG_PEER_ADDRESS);

        checkMandatoryParameterMissingErrorCode(response, 2008);
    }

    @Test
    void getInboundPeersList_whenSetIncludePeerInfo_thenGetPeersList() throws URISyntaxException, IOException {
        when(service.getInboundPeers()).thenReturn(List.of(peer));

        MockHttpResponse response = sendGetRequest("/networking/peer/inbound?includePeerInfo=true");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        JsonNode root = mapper.readTree(content);
        assertTrue(root.get("peers").isArray());
        assertEquals(PEER_ADDRESS, root.withArray("peers").get(0).get("address").asText());
    }

    @Test
    void getInboundPeersList_whenUnSetIncludePeerInfo_thenGetPeersList() throws URISyntaxException, IOException {
        when(service.getInboundPeers()).thenReturn(List.of(peer));

        MockHttpResponse response = sendGetRequest("/networking/peer/inbound");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        JsonNode root = mapper.readTree(content);
        assertTrue(root.get("peers").isArray());
        assertEquals(PEER_ADDRESS, root.withArray("peers").get(0).asText());
    }

    @Test
    void addPeerInBlackList() throws URISyntaxException, IOException {
        when(service.putPeerInBlackList(PEER_ADDRESS)).thenReturn(peer);

        MockHttpResponse response = sendPostRequest("/networking/peer/blacklist", "peer="+PEER_ADDRESS);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertEquals(0, result.get("newErrorCode"));
        assertTrue(result.containsKey("done"));
        assertEquals(true, result.get("done"));
    }

    @Test
    void addPeerInBlackList_whenCallWithoutMandatoryParam_thenGetError_2003() throws URISyntaxException, IOException {
        MockHttpResponse response = sendPostRequest("/networking/peer/blacklist", "missedParam=value");

        checkMandatoryParameterMissingErrorCode(response, 2003);
    }

    @Test
    void addAPIProxyPeerInBlackList() throws URISyntaxException, IOException {
        when(service.findOrCreatePeerByAddress(PEER_ADDRESS)).thenReturn(peer);

        MockHttpResponse response = sendPostRequest("/networking/peer/proxyblacklist", "peer="+PEER_ADDRESS);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertEquals(0, result.get("newErrorCode"));
        assertTrue(result.containsKey("done"));
    }

    @Test
    void addAPIProxyPeerInBlackList_whenCallWithoutMandatoryParam_thenGetError_2003() throws URISyntaxException, IOException {
        MockHttpResponse response = sendPostRequest("/networking/peer/proxyblacklist", "missedParam=value");

        checkMandatoryParameterMissingErrorCode(response, 2003);
    }

    @Test
    void addAPIProxyPeerInBlackList_whenCallWithWrongPeer_thenGetError_2005() throws URISyntaxException, IOException {
        when(service.findOrCreatePeerByAddress(WRONG_PEER_ADDRESS)).thenReturn(null);

        MockHttpResponse response = sendPostRequest("/networking/peer/proxyblacklist", "peer="+WRONG_PEER_ADDRESS);

        checkMandatoryParameterMissingErrorCode(response, 2005);
    }

    @Test
    void setAPIProxyPeer() throws URISyntaxException, IOException {
        when(service.findPeerByAddress(PEER_ADDRESS)).thenReturn(peer);

        MockHttpResponse response = sendPostRequest("/networking/peer/setproxy", "peer="+PEER_ADDRESS);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:"+result.get("newErrorCode"));

        PeerDTO dto = mapper.readValue(content, PeerDTO.class);
        assertNotNull(dto);
        assertEquals(PEER_ADDRESS, dto.getAddress());
        assertEquals(ANNOUNCED_ADDRESS, dto.getAnnouncedAddress());
    }

    @Test
    void setAPIProxyPeer_whenCallWithoutMandatoryParam_thenGetError_2003() throws URISyntaxException, IOException {
        MockHttpResponse response = sendPostRequest("/networking/peer/setproxy", "missedParam=value");

        checkMandatoryParameterMissingErrorCode(response, 2003);
    }

}