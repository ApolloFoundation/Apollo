/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.apl.core.rest.exception.ClientErrorExceptionMapper;
import com.apollocurrency.aplwallet.apl.core.rest.exception.ConstraintViolationExceptionMapper;
import com.apollocurrency.aplwallet.apl.core.rest.exception.DefaultGlobalExceptionMapper;
import com.apollocurrency.aplwallet.apl.core.rest.exception.IllegalArgumentExceptionMapper;
import com.apollocurrency.aplwallet.apl.core.rest.exception.ParameterExceptionMapper;
import com.apollocurrency.aplwallet.apl.core.rest.validation.BlockchainHeightValidator;
import com.apollocurrency.aplwallet.apl.core.rest.validation.CustomValidatorFactory;
import com.apollocurrency.aplwallet.apl.core.rest.validation.TimestampValidator;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.exception.RestParameterExceptionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
public abstract class AbstractEndpointTest {
    public static final int CURRENT_HEIGHT = 650000;
    public static final int CODE_2FA = 123456;
    public static final String PASSPHRASE = "123456";
    public static final String SECRET = "SuperSecretPhrase"; //accountId=-3831750337430207973
    public static final byte[] ETH_PRIVATE_KEY = SECRET.getBytes();
    public static final String ETH_PUBLIC_KEY = "cd469185df58a4f26629a522658c1917f839c2f3b82ab16860f677501989bfa256fcd0043a39cc09a26dd2307c7a9c560ab6f40abfdc74c9f8924aa0aaba597d";
    public static final String ETH_ADDRESS = "0xeba908a0ec5c553afb9251f293c4f2dc18494ec8";
    public static final String PUBLIC_KEY_SECRET = "ce2466ca75ba9703be43f24a9d638e0cc5005b41df72383cbf85093233c17e21";
    public static final long ACCOUNT_ID_WITH_SECRET = -3831750337430207973L;

    static ObjectMapper mapper = new ObjectMapper();

    static {
        Convert2.init("APL", 1739068987193023818L);
    }

    Dispatcher dispatcher;
    Blockchain blockchain = mock(Blockchain.class);
    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
        .configure()
        .constraintValidatorFactory(
            new CustomValidatorFactory(
                Map.of( BlockchainHeightValidator.class, new BlockchainHeightValidator(blockchain),
                    TimestampValidator.class, new TimestampValidator()
                ))
        )
        .buildValidatorFactory();
    Validator validator = validatorFactory.getValidator();

    public static void print(String format, Object... args) {
        log.trace(format, args);
    }

    void setUp() {
        dispatcher = MockDispatcherFactory.createDispatcher();
        dispatcher.getProviderFactory()
            .register(DefaultGlobalExceptionMapper.class)
            .register(RestParameterExceptionMapper.class)
            .register(ConstraintViolationExceptionMapper.class)
            .register(IllegalArgumentExceptionMapper.class)
            .register(ClientErrorExceptionMapper.class)
            .register(ParameterExceptionMapper.class);

        doReturn(CURRENT_HEIGHT).when(blockchain).getHeight();
    }

    void checkMandatoryParameterMissingErrorCode(MockHttpResponse response, int expectedErrorCode) throws IOException {
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertTrue(result.containsKey("newErrorCode"), "Missing expected field [newErrorCode], it's an issue.");
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

    MockHttpResponse sendPostRequest(String uri, String body) throws URISyntaxException {
        MockHttpRequest request = post(uri);
        return sendPostRequest(request, body);
    }

    MockHttpResponse sendPostRequest(MockHttpRequest request, String body) throws URISyntaxException {
//        request.accept(MediaType.TEXT_HTML);
        request.contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        request.setAttribute(Validator.class.getName(), validator);
        if (StringUtils.isNoneBlank(body)) {
            request.content(body.getBytes());
        }

        MockHttpResponse response = new MockHttpResponse();

        return sendHttpRequest(request, response);
    }

    MockHttpResponse sendHttpRequest(MockHttpRequest request, MockHttpResponse response) {
        dispatcher.invoke(request, response);
        return response;
    }

}
