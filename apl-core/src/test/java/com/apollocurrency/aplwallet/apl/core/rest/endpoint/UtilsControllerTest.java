/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.Dispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Disabled
class UtilsControllerTest {

    private static ObjectMapper mapper = new ObjectMapper();
    private Dispatcher dispatcher;
//    private AnService service;

    @BeforeEach
    void setup(){
        dispatcher = MockDispatcherFactory.createDispatcher();
//        service = mock(AnService.class);
//        UtilsController controller = new UtilsController();
//        dispatcher.getRegistry().addSingletonResource(controller);
    }

    @Test
    void encodeQeCode() throws URISyntaxException, IOException {
//        doReturn(data).when(service).method();

        MockHttpRequest request = MockHttpRequest.post("/utils/qrcode/encode")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
//            .contentType(MediaType.APPLICATION_JSON_TYPE)
            .addFormHeader("qrCodeData", "123456")
            .addFormHeader("width", "100").addFormHeader("height", "100");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());
        String respondJson = response.getContentAsString();
//        UtilsController.QrCodeDto qrCodeDto = mapper.readValue(respondJson, new TypeReference<UtilsController.QrCodeDto>(){});
//        assertEquals(ShardTestData.SHARD_DTO_LIST, shards);
//        assertNotNull(qrCodeDto.qrCodeBase64);
    }

    @Test
    void decodeQrCode() throws URISyntaxException, UnsupportedEncodingException {
//        doReturn(true).when(service).reset(1);

        MockHttpRequest request = MockHttpRequest.post("").contentType(MediaType.APPLICATION_JSON_TYPE);
        MockHttpResponse response = new MockHttpResponse();

        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());
        String content = response.getContentAsString();
        assertEquals("true", content);
    }
}