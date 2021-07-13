/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.ShardDTO;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.rest.converter.ShardToDtoConverter;
import com.apollocurrency.aplwallet.apl.core.rest.utils.FirstLastIndexParser;
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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ShardControllerTest {

    private static ObjectMapper mapper = new ObjectMapper();
    private Dispatcher dispatcher;
    private ShardService shardService;
    private FirstLastIndexParser indexParser;
    private ShardToDtoConverter shardConverter = mock(ShardToDtoConverter.class);

    @BeforeEach
    void setup() {
        dispatcher = MockDispatcherFactory.createDispatcher();
        shardService = mock(ShardService.class);
        indexParser = mock(FirstLastIndexParser.class);
        ShardController controller = new ShardController(shardService, 100);
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
        List<ShardDTO> shards = mapper.readValue(shardJson, new TypeReference<List<ShardDTO>>() {
        });
        assertEquals(ShardTestData.SHARD_DTO_LIST, shards);
    }

    @Test
    void testResetToBackup() throws URISyntaxException, UnsupportedEncodingException {
        doReturn(true).when(shardService).reset(1);

        MockHttpRequest request = MockHttpRequest.post("/shards/reset/" + 1).contentType(MediaType.APPLICATION_JSON_TYPE);
        MockHttpResponse response = new MockHttpResponse();

        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());
        String content = response.getContentAsString();
        assertEquals("true", content);
    }

    @Test
    void testGetBlocksFromShards() throws URISyntaxException, UnsupportedEncodingException {
        List<Shard> list = new ArrayList<>();
        Shard shard = new Shard(-1, 100);
        list.add(shard);
        doReturn(list).when(shardService).getShardsByBlockHeightRange(0, -1);
        List<ShardDTO> dtoList = new ArrayList<>();
        dtoList.add(new ShardDTO(-1L, null, 100L, 100, null, null, null, null, null));
        doReturn(dtoList).when(shardConverter).convert(list);
        FirstLastIndexParser.FirstLastIndex index = new FirstLastIndexParser.FirstLastIndex(0, 99);
        doReturn(index).when(indexParser).adjustIndexes(0, -1);

        MockHttpRequest request = MockHttpRequest.get("/shards/blocks").contentType(MediaType.APPLICATION_JSON_TYPE);
        MockHttpResponse response = new MockHttpResponse();

        dispatcher.invoke(request, response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String respondJson = response.getContentAsString();
        assertNotNull(respondJson);
        assertFalse(respondJson.contains("Error"), "Error from API : " + respondJson);

        //verify
        verify(shardService, times(1)).getShardsByBlockHeightRange(0, -1);

    }
}