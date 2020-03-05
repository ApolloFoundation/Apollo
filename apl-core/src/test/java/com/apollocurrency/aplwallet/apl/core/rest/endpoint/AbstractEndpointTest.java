package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.rest.RestParametersParser;
import com.apollocurrency.aplwallet.apl.core.rest.exception.ClientErrorExceptionMapper;
import com.apollocurrency.aplwallet.apl.core.rest.exception.ConstraintViolationExceptionMapper;
import com.apollocurrency.aplwallet.apl.core.rest.exception.DefaultGlobalExceptionMapper;
import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterExceptionMapper;
import com.apollocurrency.aplwallet.apl.core.rest.validation.BlockchainHeightValidator;
import com.apollocurrency.aplwallet.apl.core.rest.validation.CustomValidatorFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.Dispatcher;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import static org.jboss.resteasy.mock.MockHttpRequest.get;
import static org.jboss.resteasy.mock.MockHttpRequest.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class AbstractEndpointTest {
    public static final int CURRENT_HEIGHT = 650000;
    public static final int CODE_2FA = 123456;
    public static final String PASSPHRASE = "123456";
    public static final String SECRET = "SuperSecretPhrase"; //accountId=-3831750337430207973
    public static final long ACCOUNT_ID_WITH_SECRET = -3831750337430207973L;

    static ObjectMapper mapper = new ObjectMapper();
    Dispatcher dispatcher;

    Blockchain blockchain = mock(Blockchain.class);
    RestParametersParser restParametersParser = new RestParametersParser(blockchain);
    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
        .configure()
        .constraintValidatorFactory( new CustomValidatorFactory(
                       Map.of( BlockchainHeightValidator.class, new BlockchainHeightValidator(blockchain) )) )
        .buildValidatorFactory();
    Validator validator = validatorFactory.getValidator();

    static{
        BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
        doReturn("APL").when(blockchainConfig).getAccountPrefix();
        Convert2.init(blockchainConfig);
    }

    void setUp() {
        dispatcher = MockDispatcherFactory.createDispatcher();
        dispatcher.getProviderFactory()
            .register(DefaultGlobalExceptionMapper.class)
            .register(RestParameterExceptionMapper.class)
            .register(ConstraintViolationExceptionMapper.class)
            .register(ClientErrorExceptionMapper.class);

        doReturn(CURRENT_HEIGHT).when(blockchain).getHeight();
    }

    void checkMandatoryParameterMissingErrorCode(MockHttpResponse response, int expectedErrorCode) throws IOException {
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertTrue(result.containsKey("newErrorCode"),"Missing expected field [newErrorCode], it's an issue.");
        assertEquals(expectedErrorCode, result.get("newErrorCode"));
    }

    MockHttpResponse sendGetRequest(String uri) throws URISyntaxException {
        MockHttpRequest request = get(uri);
        request.accept(MediaType.APPLICATION_JSON);
        request.contentType(MediaType.APPLICATION_JSON_TYPE);
        request.setAttribute(Validator.class.getName(), validator);

        MockHttpResponse response = new MockHttpResponse();

        return sendHttpRequest(request, response);
    }

    MockHttpResponse sendPostRequest(String uri, String body) throws URISyntaxException{
        MockHttpRequest request = post(uri);
        return sendPostRequest(request, body);
    }

    MockHttpResponse sendPostRequest(MockHttpRequest request, String body) throws URISyntaxException{
        request.accept(MediaType.TEXT_HTML);
        request.contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        request.setAttribute(Validator.class.getName(), validator);
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
