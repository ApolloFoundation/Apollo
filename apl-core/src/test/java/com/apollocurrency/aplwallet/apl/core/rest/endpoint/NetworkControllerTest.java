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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.spi.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jboss.resteasy.mock.MockHttpRequest.get;
import static org.jboss.resteasy.mock.MockHttpRequest.post;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NetworkControllerTest {
    private static final String PEER_ADDRESS = "192.168.2.68";
    private static final String WRONG_PEER_ADDRESS = "10.0.0.1";
    private static final String ANNOUNCED_ADDRESS = "10.10.10.10";

    private static ObjectMapper mapper = new ObjectMapper();
    private static Dispatcher dispatcher;

    @BeforeAll
    static void setupClass(){
        dispatcher = MockDispatcherFactory.createDispatcher();

        NetworkController endpoint = new NetworkController();
        endpoint.setPeerConverter(new PeerConverter());
        dispatcher.getRegistry().addSingletonResource(endpoint);

        NetworkService service = mock(NetworkService.class);
        endpoint.setService(service);

        Peer peer = EntityProducer.createPeer(PEER_ADDRESS, ANNOUNCED_ADDRESS, true, 0);

        when(service.findPeerByAddress(PEER_ADDRESS)).thenReturn(peer);
        when(service.findPeerByAddress(WRONG_PEER_ADDRESS)).thenReturn(null);

        when(service.findOrCreatePeerByAddress(PEER_ADDRESS)).thenReturn(peer);
        when(service.findOrCreatePeerByAddress(WRONG_PEER_ADDRESS)).thenReturn(null);

        when(service.putPeerInBlackList(PEER_ADDRESS)).thenReturn(peer);

        when(service.getInboundPeers()).thenReturn(List.of(peer));

        when(service.getPeersByStateAndService(true, null, 0)).thenReturn(Collections.EMPTY_LIST);
        when(service.getPeersByStateAndService(false, null, 0)).thenReturn(List.of(peer));

    }

    @BeforeEach
    void setUp() {
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
        MockHttpResponse response = sendGetRequest("/networking/peer?peer="+ WRONG_PEER_ADDRESS);

        checkMandatoryParameterMissingErrorCode(response, 2005);
    }

    @Test
    void getPeer_whenCallWithParameter_thenGetPeer() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/networking/peer?peer="+ PEER_ADDRESS);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        Map result = mapper.readValue(content, Map.class);
        assertEquals(PEER_ADDRESS, result.get("address"));
    }

    @Test
    void getPeersList_whenSetActive_thenGetEmptyList() throws URISyntaxException, IOException {
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
        MockHttpResponse response = sendPostRequest("/networking/peer", "");

        checkMandatoryParameterMissingErrorCode(response, 2003);
    }

    @Test
    void addOrReplacePeer_whenCallWithWrongPeer_thenGetError_2008() throws URISyntaxException, IOException {
        MockHttpResponse response = sendPostRequest("/networking/peer", "peer="+WRONG_PEER_ADDRESS);

        checkMandatoryParameterMissingErrorCode(response, 2008);
    }

    @Test
    void getInboundPeersList_whenSetIncludePeerInfo_thenGetPeersList() throws URISyntaxException, IOException {
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
        MockHttpResponse response = sendPostRequest("/networking/peer/blacklist", "");

        checkMandatoryParameterMissingErrorCode(response, 2003);
    }

    @Test
    void addAPIProxyPeerInBlackList() throws URISyntaxException, IOException {
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
        MockHttpResponse response = sendPostRequest("/networking/peer/proxyblacklist", "");

        checkMandatoryParameterMissingErrorCode(response, 2003);
    }

    @Test
    void addAPIProxyPeerInBlackList_whenCallWithWrongPeer_thenGetError_2005() throws URISyntaxException, IOException {
        MockHttpResponse response = sendPostRequest("/networking/peer/proxyblacklist", "peer="+WRONG_PEER_ADDRESS);

        checkMandatoryParameterMissingErrorCode(response, 2005);
    }

    @Test
    void setAPIProxyPeer() throws URISyntaxException, IOException {
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
        MockHttpResponse response = sendPostRequest("/networking/peer/setproxy", "");

        checkMandatoryParameterMissingErrorCode(response, 2003);
    }

    private void checkMandatoryParameterMissingErrorCode(MockHttpResponse response, int expectedErrorCode) throws IOException {
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertTrue(result.containsKey("newErrorCode"),"Missing param, it's an issue.");
        assertEquals(expectedErrorCode, result.get("newErrorCode"));
    }

    private MockHttpResponse sendGetRequest(String uri) throws URISyntaxException{
        MockHttpRequest request = get(uri);
        request.accept(MediaType.APPLICATION_JSON);
        request.contentType(MediaType.APPLICATION_JSON_TYPE);

        MockHttpResponse response = new MockHttpResponse();

        return sendHttpRequest(request, response);
    }

    private MockHttpResponse sendPostRequest(String uri, String body) throws URISyntaxException{
        MockHttpRequest request = post(uri);
        request.accept(MediaType.TEXT_HTML);
        request.contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        request.content(body.getBytes());

        MockHttpResponse response = new MockHttpResponse();

        return sendHttpRequest(request, response);
    }

    private MockHttpResponse sendHttpRequest(MockHttpRequest request, MockHttpResponse response) {
        dispatcher.invoke(request, response);
        return response;
    }

    private static void print(String format, Object... args){
        System.out.printf(format, args);
    }

}