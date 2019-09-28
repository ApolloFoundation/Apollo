/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.api.dto.ShardDTO;
import com.apollocurrency.aplwallet.apl.core.shard.ShardService;
import com.apollocurrency.aplwallet.apl.data.ShardTestData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.Dispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.List;
import javax.ws.rs.core.MediaType;

class ShardControllerTest {

    private static ObjectMapper mapper = new ObjectMapper();
    private Dispatcher dispatcher;
    private ShardService shardService;

    @BeforeEach
    void setup(){
        dispatcher = MockDispatcherFactory.createDispatcher();
        shardService = mock(ShardService.class);
        ShardController controller = new ShardController(shardService);
        dispatcher.getRegistry().addSingletonResource(controller);
    }

    @Test
    void testGetAllShards() throws URISyntaxException, IOException {
        doReturn(ShardTestData.SHARDS).when(shardService).getAllCompletedShards();

        MockHttpRequest request = MockHttpRequest.get("/shards").contentType(MediaType.APPLICATION_JSON_TYPE);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());

        String shardJson = response.getContentAsString();
        List<ShardDTO> shards = mapper.readValue(shardJson, new TypeReference<List<ShardDTO>>(){});
        assertEquals(ShardTestData.SHARD_DTO_LIST, shards);
    }

    @Test
    void testResetToBackup() throws URISyntaxException, UnsupportedEncodingException {
        doReturn(true).when(shardService).reset(1);

        MockHttpRequest request = MockHttpRequest.post("/shards/" + 1).contentType(MediaType.APPLICATION_JSON_TYPE);
        MockHttpResponse response = new MockHttpResponse();

        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());
        String content = response.getContentAsString();
        assertEquals("true", content);
    }
}