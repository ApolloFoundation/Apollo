package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import static org.jboss.resteasy.mock.MockHttpRequest.get;
import static org.jboss.resteasy.mock.MockHttpRequest.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractEndpointTest {
    static ObjectMapper mapper = new ObjectMapper();
    Dispatcher dispatcher;

    void checkMandatoryParameterMissingErrorCode(MockHttpResponse response, int expectedErrorCode) throws IOException {
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertTrue(result.containsKey("newErrorCode"),"Missing param, it's an issue.");
        assertEquals(expectedErrorCode, result.get("newErrorCode"));
    }

    MockHttpResponse sendGetRequest(String uri) throws URISyntaxException {
        MockHttpRequest request = get(uri);
        request.accept(MediaType.APPLICATION_JSON);
        request.contentType(MediaType.APPLICATION_JSON_TYPE);

        MockHttpResponse response = new MockHttpResponse();

        return sendHttpRequest(request, response);
    }

    MockHttpResponse sendPostRequest(String uri, String body) throws URISyntaxException{
        MockHttpRequest request = post(uri);
        request.accept(MediaType.TEXT_HTML);
        request.contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        if (StringUtils.isNoneEmpty(body)) {
            request.content(body.getBytes());
        }

        MockHttpResponse response = new MockHttpResponse();

        return sendHttpRequest(request, response);
    }

    MockHttpResponse sendHttpRequest(MockHttpRequest request, MockHttpResponse response) {
        dispatcher.invoke(request, response);
        return response;
    }

    public static void print(String format, Object... args){
        System.out.printf(format, args);
    }

}
