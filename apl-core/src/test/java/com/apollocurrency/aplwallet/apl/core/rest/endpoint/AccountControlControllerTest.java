/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.stream.Stream;

import com.apollocurrency.aplwallet.api.dto.account.AccountControlPhasingDTO;
import com.apollocurrency.aplwallet.api.response.AccountControlPhasingResponse;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.PublicKey;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountControlPhasingService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.http.ElGamalEncryptor;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.rest.converter.HttpRequestToCreateTransactionRequestConverter;
import com.apollocurrency.aplwallet.apl.core.rest.utils.FirstLastIndexParser;
import com.apollocurrency.aplwallet.apl.data.AccountControlPhasingData;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountControlControllerTest extends AbstractEndpointTest {

    private AccountControlController endpoint;
    private FirstLastIndexParser indexParser = new FirstLastIndexParser(100);
    private AccountControlPhasingData actd;
    private static final String accCtrlPhaseListUri = "/accounts/control/list";
    private static final String accCtrlPhaseIdUri = "/accounts/control/id";
    private static final String accCtrlLeaseBalanceUri = "/accounts/control/lease";
    private static final String accCtrlPhasingUri = "/accounts/control/phasing";

    private static String senderRS = "APL-Q6U9-FWH3-LA6G-D3F88";
    private static Long senderId = -5541220367884151993L;

    private static String recipientRS = "APL-FXHG-6KHM-23LE-42ACU";
    private static Long recipientId = 3254132361968154094L;

    @Mock
    private AccountControlPhasingService accountControlPhasingService = mock(AccountControlPhasingService.class);
    @Mock
    private HeightConfig heightConfig = mock(HeightConfig.class);
    @Mock
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    @Mock
    private TransactionCreator txCreator = mock(TransactionCreator.class);
    @Mock
    private AccountService accountService = mock(AccountService.class);
    @Mock
    HttpServletRequest req;

    @BeforeEach
    void setUp() {
        super.setUp();
        doReturn(100L).when(heightConfig).getMaxBalanceATM();
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        endpoint = new AccountControlController(
            indexParser, accountControlPhasingService, blockchainConfig, txCreator, accountService);
        dispatcher.getRegistry().addSingletonResource(endpoint);
        dispatcher.getDefaultContextObjects().put(HttpServletRequest.class, req);
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
        assertEquals(2005, error.getNewErrorCode(), error.getErrorDescription());
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

    @Test
    void testLeaseBalance_missingSenderAccount()
        throws URISyntaxException, UnsupportedEncodingException,
        AplException.ValidationException, JsonProcessingException {

        MockHttpResponse response = sendPostRequest(accCtrlLeaseBalanceUri,
            "passphrase=" + PASSPHRASE);
        String respondJson = response.getContentAsString();

        Error error = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(error.getErrorDescription());
        assertEquals(2001, error.getNewErrorCode(), error.getErrorDescription());
        // several 'Constraint violation: ... '
        assertTrue(error.getErrorDescription().contains("Constraint violation:"), error.getErrorDescription());
    }

    @Test
    void testLeaseBalance_missingRecipientAccount()
        throws URISyntaxException, UnsupportedEncodingException,
        AplException.ValidationException, JsonProcessingException {

        MockHttpResponse response = sendPostRequest(accCtrlLeaseBalanceUri,
            "passphrase=" + PASSPHRASE + "&sender=" + senderRS
                + "");
        String respondJson = response.getContentAsString();

        Error error = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(error.getErrorDescription());
        assertEquals(2001, error.getNewErrorCode(), error.getErrorDescription());
        assertTrue(error.getErrorDescription().contains("Constraint violation: leaseBalance.recipientIdParameter"), error.getErrorDescription());
    }

    @Test
    void testLeaseBalance_incorrect_period()
        throws URISyntaxException, UnsupportedEncodingException,
        AplException.ValidationException, JsonProcessingException {
        Account recipient = new Account(recipientId, 10000 * Constants.ONE_APL, 10000 * Constants.ONE_APL, 0, 0, CURRENT_HEIGHT);
        recipient.setPublicKey(new PublicKey(recipient.getId(), new byte[]{}, 0));

        MockHttpResponse response = sendPostRequest(accCtrlLeaseBalanceUri,
            "passphrase=" + PASSPHRASE + "&sender=" + senderRS
                + "&recipient=" + recipientRS + "&period=-1");
        String respondJson = response.getContentAsString();

        Error error = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(error.getErrorDescription());
        assertEquals(2001, error.getNewErrorCode(), error.getErrorDescription());
        assertTrue(error.getErrorDescription().contains("Constraint violation: leaseBalance.period"));
    }

    @Test
    void testSetPhasingOnlyControl_NO_PARAMS()
        throws URISyntaxException, UnsupportedEncodingException, JsonProcessingException {

        MockHttpResponse response = sendPostRequest(accCtrlPhasingUri,
            "passphrase=" + PASSPHRASE);
        String respondJson = response.getContentAsString();

        Error error = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(error.getErrorDescription());
        assertEquals(2001, error.getNewErrorCode(), error.getErrorDescription());
        // several Constraint violation:
        assertTrue(error.getErrorDescription().contains("Constraint violation:"), error.getErrorDescription());
    }

    @Test
    void testSetPhasingOnlyControl_minimal_params()
        throws URISyntaxException, UnsupportedEncodingException,
        AplException.ValidationException, JsonProcessingException {

        MockHttpResponse response = sendPostRequest(accCtrlPhasingUri,
            "passphrase=" + PASSPHRASE + "&sender=" + senderRS
                + "&controlVotingModel=NONE" + "&controlQuorum=1");
        String respondJson = response.getContentAsString();

        Error error = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(error.getErrorDescription());
        assertEquals(2030, error.getNewErrorCode(), error.getErrorDescription());
        assertTrue(error.getErrorDescription().contains("Constraint violation:"), error.getErrorDescription());
    }


}