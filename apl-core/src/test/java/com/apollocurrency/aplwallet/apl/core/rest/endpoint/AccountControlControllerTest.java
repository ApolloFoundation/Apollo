package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.stream.Stream;

import com.apollocurrency.aplwallet.api.dto.account.AccountControlPhasingDTO;
import com.apollocurrency.aplwallet.api.response.AccountControlPhasingResponse;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountControlPhasingService;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.rest.provider.WhiteListedAccountConverterProvider;
import com.apollocurrency.aplwallet.apl.core.rest.utils.FirstLastIndexParser;
import com.apollocurrency.aplwallet.apl.data.AccountControlPhasingData;
import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class AccountControlControllerTest extends AbstractEndpointTest {

    private AccountControlController endpoint;
//    private AccountControlPhasingConverter converter = mock(AccountControlPhasingConverter.class);
    private FirstLastIndexParser indexParser = new FirstLastIndexParser(100);
    private AccountControlPhasingData actd;
    private static final String accCtrlPhaseListUri = "/accounts/control/list";
    private static final String accCtrlPhaseIdUri = "/accounts/control/id";

    @Mock
    private AccountControlPhasingService accountControlPhasingService = mock(AccountControlPhasingService.class);
    @Mock
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    @Mock
    private TransactionCreator txCreator = mock(TransactionCreator.class);

    @BeforeEach
    void setUp() {
        super.setUp();
        endpoint = new AccountControlController(indexParser, accountControlPhasingService, blockchainConfig, txCreator);
        dispatcher.getProviderFactory().register(WhiteListedAccountConverterProvider.class);
        dispatcher.getRegistry().addSingletonResource(endpoint);
        actd = new AccountControlPhasingData();
    }

    @Test
    void testGetAll_by_indexes() throws URISyntaxException, IOException {
        doReturn(Stream.of(actd.AC_CONT_PHAS_0, actd.AC_CONT_PHAS_1, actd.AC_CONT_PHAS_2, actd.AC_CONT_PHAS_3))
            .when(accountControlPhasingService).getAllStream(0, 10);

        MockHttpResponse response = sendGetRequest(accCtrlPhaseListUri + "?firstIndex=0&lastIndex=10");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String respondJson = response.getContentAsString();
        assertNotNull(respondJson);
        assertFalse(respondJson.contains("Error"), "Error from API : " + respondJson);

        AccountControlPhasingResponse result = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(result);
        assertNotNull(result.getPhasingOnlyControls());
        assertEquals(4, result.getPhasingOnlyControls().size());

        //verify
        verify(accountControlPhasingService, times(1)).getAllStream(0, 10);
    }

    @Test
    void testGetAll_by_EMPTY_indexes() throws URISyntaxException, IOException {
        doReturn(Stream.of(actd.AC_CONT_PHAS_0, actd.AC_CONT_PHAS_1, actd.AC_CONT_PHAS_2, actd.AC_CONT_PHAS_3))
            .when(accountControlPhasingService).getAllStream(0, 99);

        MockHttpResponse response = sendGetRequest(accCtrlPhaseListUri);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String respondJson = response.getContentAsString();
        assertNotNull(respondJson);
        assertFalse(respondJson.contains("Error"), "Error from API : " + respondJson);

        AccountControlPhasingResponse result = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(result);
        assertNotNull(result.getPhasingOnlyControls());
        assertEquals(4, result.getPhasingOnlyControls().size());

        //verify
        verify(accountControlPhasingService, times(1)).getAllStream(0, 99);
    }

    @Test
    void testGetPhasingById_EMPTY_account() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest(accCtrlPhaseIdUri + "?account=");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String respondJson = response.getContentAsString();
        assertNotNull(respondJson);
        Error error = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(error.getErrorDescription());
        assertEquals(2005, error.getNewErrorCode());
        //verify
        verify(accountControlPhasingService, never()).get(actd.AC_CONT_PHAS_2.getAccountId());
    }

    @Test
    void testGetPhasingById_OK() throws URISyntaxException, IOException {
        doReturn(actd.AC_CONT_PHAS_2).when(accountControlPhasingService).get(actd.AC_CONT_PHAS_2.getAccountId());

        MockHttpResponse response = sendGetRequest(accCtrlPhaseIdUri + "?account=" + actd.AC_CONT_PHAS_2.getAccountId());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String respondJson = response.getContentAsString();
        assertNotNull(respondJson);
        assertFalse(respondJson.contains("Error"), "Error from API : " + respondJson);

        AccountControlPhasingDTO dtoResult = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(dtoResult);
        assertEquals(String.valueOf( Long.toUnsignedString(actd.AC_CONT_PHAS_2.getAccountId())), dtoResult.getAccount());
        //verify
        verify(accountControlPhasingService, times(1)).get(actd.AC_CONT_PHAS_2.getAccountId());
    }

}