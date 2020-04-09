package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URISyntaxException;

import com.apollocurrency.aplwallet.apl.core.rest.utils.FirstLastIndexParser;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountControlControllerTest extends AbstractEndpointTest {

    private FirstLastIndexParser indexParser = new FirstLastIndexParser(100);

    @BeforeEach
    void setUp() {
        super.setUp();
    }

    @Test
    void testGetAllPhasingOnly() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/control/phasing");

    }
}