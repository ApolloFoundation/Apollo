package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountConverter;
import com.apollocurrency.aplwallet.apl.core.rest.service.AccountService;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.mockito.Mockito.mock;

class AccountEndpointTest extends AbstractEndpointTest{

    private AccountController endpoint = new AccountController();
    private AccountService service = mock(AccountService.class);

    @BeforeEach
    void setUp() {
        dispatcher = MockDispatcherFactory.createDispatcher();
        dispatcher.getRegistry().addSingletonResource(endpoint);

        endpoint.setConverter(new AccountConverter());
        endpoint.setService(service);
    }

    @AfterEach
    void tearDown() {

    }

    @Test
    void getAccount_whenCallWithoutMandatoryParameter_thenGetError_2003() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/account");

        checkMandatoryParameterMissingErrorCode(response, 2003);

    }

    @Test
    void getAccount_whenCallWithWrongAccountId_thenGetError_2005() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/account?account=0");

        checkMandatoryParameterMissingErrorCode(response, 2005);

    }

}