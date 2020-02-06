/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.Dispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class UtilsControllerTest {

    private static ObjectMapper mapper = new ObjectMapper();
    private Dispatcher dispatcher;
    private static String encodeQrUri = "/utils/qrcode/encode";
//    private AnService service;

    @BeforeEach
    void setup(){
        dispatcher = MockDispatcherFactory.createDispatcher();
//        service = mock(AnService.class);
        UtilsController controller = new UtilsController();
        dispatcher.getRegistry().addSingletonResource(controller);
    }

    @Test
    void encodeQrCode_SUCCESS() throws URISyntaxException, IOException {
//        doReturn(data).when(service).method();
        MockHttpRequest request = MockHttpRequest.post(encodeQrUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
            .addFormHeader("qrCodeData", "123456");
//            .addFormHeader("width", "100").addFormHeader("height", "100");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());
        String respondJson = response.getContentAsString();
        UtilsController.QrCodeDto qrCodeDto = mapper.readValue(respondJson, new TypeReference<UtilsController.QrCodeDto>(){});
        assertNotNull(qrCodeDto.qrCodeBase64);
        assertTrue(qrCodeDto.qrCodeBase64.length() > 0);
    }

    @Test
    void encodeQrCode_NO_QrCodeData() throws Exception {
//        doReturn(true).when(service).reset(1);
        MockHttpRequest request = MockHttpRequest.post(encodeQrUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
            .addFormHeader("qrCodeData", "")
            .addFormHeader("width", "100").addFormHeader("height", "100");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());
        String respondJson = response.getContentAsString();
        Error error = mapper.readValue(respondJson, new TypeReference<Error>(){});

        assertEquals(200, response.getStatus());
        assertNotNull(error.getErrorDescription());
        assertEquals(2003, error.getNewErrorCode());
    }

    @Test
    void encodeQrCode_incorrect_width() throws Exception {
//        doReturn(true).when(service).reset(1);
        MockHttpRequest request = MockHttpRequest.post(encodeQrUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
            .addFormHeader("qrCodeData", "1234")
            .addFormHeader("width", "S_DF_123").addFormHeader("height", "100");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());
        String respondJson = response.getContentAsString();
        Error error = mapper.readValue(respondJson, new TypeReference<>(){});

        assertEquals(200, response.getStatus());
        assertNotNull(error.getErrorDescription());
        assertEquals(2011, error.getNewErrorCode());
    }

    @Test
    void encodeQrCode_incorrect_height() throws Exception {
//        doReturn(true).when(service).reset(1);
        MockHttpRequest request = MockHttpRequest.post(encodeQrUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
            .addFormHeader("qrCodeData", "1234")
            .addFormHeader("width", "100").addFormHeader("height", "incorrect");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());
        String respondJson = response.getContentAsString();
        Error error = mapper.readValue(respondJson, new TypeReference<>(){});

        assertEquals(200, response.getStatus());
        assertNotNull(error.getErrorDescription());
        assertEquals(2011, error.getNewErrorCode());
    }

    @Test
    void encodeQrCode_height_outOfRange() throws Exception {
//        doReturn(true).when(service).reset(1);
        MockHttpRequest request = MockHttpRequest.post(encodeQrUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
            .addFormHeader("qrCodeData", "1234")
            .addFormHeader("width", "100").addFormHeader("height", "-1");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());
        String respondJson = response.getContentAsString();
        Error error = mapper.readValue(respondJson, new TypeReference<>(){});

        assertEquals(200, response.getStatus());
        assertNotNull(error.getErrorDescription());
        assertEquals(2011, error.getNewErrorCode());
    }
}