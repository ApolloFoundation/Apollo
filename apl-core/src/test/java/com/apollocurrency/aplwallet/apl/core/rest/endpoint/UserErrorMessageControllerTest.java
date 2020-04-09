package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.apl.data.UserErrorMessageTestData;
import com.apollocurrency.aplwallet.apl.exchange.model.UserErrorMessage;
import com.apollocurrency.aplwallet.apl.exchange.service.UserErrorMessageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.Dispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
public class UserErrorMessageControllerTest {
    private static ObjectMapper mapper = new ObjectMapper();
    private Dispatcher dispatcher;
    @Mock
    private UserErrorMessageService service;
    private UserErrorMessageTestData td;

    @BeforeEach
    void setup() {
        dispatcher = MockDispatcherFactory.createDispatcher();
        UserErrorMessageController controller = new UserErrorMessageController(service);
        dispatcher.getRegistry().addSingletonResource(controller);
        td = new UserErrorMessageTestData();
    }

    @Test
    void testGetAllForAddressWithDefaultParams() throws URISyntaxException, IOException {
        List<UserErrorMessage> errors = List.of(td.ERROR_1, td.ERROR_3);
        doReturn(errors).when(service).getAllByAddress(td.ERROR_1.getAddress(), Long.MAX_VALUE, 100);

        MockHttpRequest request = MockHttpRequest.get("/user-errors/" + td.ERROR_1.getAddress()).contentType(MediaType.APPLICATION_JSON_TYPE);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());

        String errorJson = response.getContentAsString();
        List<UserErrorMessage> responseErrors = mapper.readValue(errorJson, new TypeReference<List<UserErrorMessage>>() {
        });
        assertEquals(errors, responseErrors);
    }


    @Test
    void testGetAllForAddressUsingTooBigLimit() throws URISyntaxException, IOException {
        List<UserErrorMessage> errors = List.of(td.ERROR_1, td.ERROR_3);
        doReturn(errors).when(service).getAllByAddress(td.ERROR_1.getAddress(), Long.MAX_VALUE, 100);

        MockHttpRequest request = MockHttpRequest.get("/user-errors/" + td.ERROR_1.getAddress() + "?limit=101").contentType(MediaType.APPLICATION_JSON_TYPE);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());

        String errorJson = response.getContentAsString();
        List<UserErrorMessage> responseErrors = mapper.readValue(errorJson, new TypeReference<List<UserErrorMessage>>() {
        });
        assertEquals(errors, responseErrors);
    }

    @Test
    void testGetAllForAddressUsingDbIdAndLimit() throws URISyntaxException, IOException {
        List<UserErrorMessage> errors = List.of(td.ERROR_1, td.ERROR_3);
        doReturn(errors).when(service).getAllByAddress(td.ERROR_1.getAddress(), 120L, 100);

        MockHttpRequest request = MockHttpRequest.get("/user-errors/" + td.ERROR_1.getAddress() + "?limit=101&toDbId=120").contentType(MediaType.APPLICATION_JSON_TYPE);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());

        String errorJson = response.getContentAsString();
        List<UserErrorMessage> responseErrors = mapper.readValue(errorJson, new TypeReference<List<UserErrorMessage>>() {
        });
        assertEquals(errors, responseErrors);
    }

    @Test
    void testGetAll() throws URISyntaxException, IOException {
        List<UserErrorMessage> errors = List.of(td.ERROR_1, td.ERROR_2, td.ERROR_3);
        doReturn(errors).when(service).getAll(Long.MAX_VALUE, 100);

        MockHttpRequest request = MockHttpRequest.get("/user-errors").contentType(MediaType.APPLICATION_JSON_TYPE);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());

        String errorJson = response.getContentAsString();
        List<UserErrorMessage> responseErrors = mapper.readValue(errorJson, new TypeReference<List<UserErrorMessage>>() {
        });
        assertEquals(errors, responseErrors);
    }

    @Test
    void testGetAllWithBigLimit() throws URISyntaxException, IOException {
        List<UserErrorMessage> errors = List.of(td.ERROR_1, td.ERROR_2, td.ERROR_3);
        doReturn(errors).when(service).getAll(Long.MAX_VALUE, 101);

        MockHttpRequest request = MockHttpRequest.get("/user-errors?limit=101").contentType(MediaType.APPLICATION_JSON_TYPE);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());

        String errorJson = response.getContentAsString();
        List<UserErrorMessage> responseErrors = mapper.readValue(errorJson, new TypeReference<List<UserErrorMessage>>() {
        });
        assertEquals(errors, responseErrors);
    }

    @Test
    void testRemove() throws URISyntaxException, IOException {
        doReturn(1).when(service).deleteByTimestamp(101);

        MockHttpRequest request = MockHttpRequest.delete("/user-errors?timestamp=101").contentType(MediaType.APPLICATION_JSON_TYPE);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());

        String errorJson = response.getContentAsString();
        UserErrorMessageController.UserErrorsDeleteResponse value = mapper.readValue(errorJson, UserErrorMessageController.UserErrorsDeleteResponse.class);
        assertEquals(1, value.getDeleted());
    }
}
