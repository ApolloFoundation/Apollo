/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.model.CurrencyBurningTxCreationRequest;
import com.apollocurrency.aplwallet.api.v2.model.ErrorResponse;
import com.apollocurrency.aplwallet.api.v2.model.TransactionCreationResponse;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.rest.utils.Account2FAHelper;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyBurningAttachment;
import com.apollocurrency.aplwallet.apl.core.utils.Convert2;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CurrenciesApiServiceImplTest {
    @Mock
    TransactionCreator creator;
    @Mock
    AccountCurrencyService accountCurrencyService;
    @Mock
    Account2FAHelper helper2FA;
    @Mock
    AccountService accountService;
    @Mock
    BlockchainConfig blockchainConfig;
    @InjectMocks
    CurrenciesApiServiceImpl apiService;


    long senderId         ;
    String rsAccount      ;
    Account senderAccount ;

    @BeforeEach
    void setUp() {
        doReturn("APL").when(blockchainConfig).getAccountPrefix();
        Convert2.init(blockchainConfig);
        senderId = AccountService.getId(Crypto.getPublicKey("123"));
        rsAccount = Convert2.rsAccount(senderId);
        senderAccount = new Account(senderId, 0);
    }

    @Test
    void currencyBurningTx_OK() {

        CurrencyBurningTxCreationRequest request = getCurrencyBurningTxCreationRequest(rsAccount, 1L, 20_000L);
        doReturn(senderAccount).when(accountService).getAccount(senderId);
        doReturn(new AccountCurrency(senderId, 1L, 20_100, 20_000, 10)).when(accountCurrencyService).getAccountCurrency(senderId, 1L);
        TransactionCreationResponse response = getTransactionCreationResponse();
        doReturn(response).when(creator).createApiV2Transaction(newTxCreatorRequest(senderAccount));

        Response result = apiService.currencyBurningTx(request, null);

        assertEquals(200, result.getStatus());
        assertEquals(response, result.getEntity());
        verify(helper2FA).verify2FA(rsAccount, null, "123", null, 239_123);
    }

    @Test
    void currencyBurningTx_noSendersAccount() {
        CurrencyBurningTxCreationRequest request = getCurrencyBurningTxCreationRequest(rsAccount, 1L, 20_000L);

        doFailingCurrencyBurning("Incorrect senderAccount, APL-AWXM-L3EK-DTT7-6HM7U is not exist", request);
    }

    @Test
    void currencyBurningTx_missingCurrencyIdParameter() {
        CurrencyBurningTxCreationRequest request = getCurrencyBurningTxCreationRequest(rsAccount, null, 20_000L);
        doReturn(senderAccount).when(accountService).getAccount(senderId);

        doFailingCurrencyBurning("The mandatory parameter 'currencyId' is not specified.", request);
    }

    @Test
    void currencyBurningTx_missingBurningAmountParameter() {
        CurrencyBurningTxCreationRequest request = getCurrencyBurningTxCreationRequest(rsAccount, 1L, null);
        doReturn(senderAccount).when(accountService).getAccount(senderId);

        doFailingCurrencyBurning("The mandatory parameter 'burningAmount' is not specified.", request);
    }

    @Test
    void currencyBurningTx_absentCurrencyBalance() {
        CurrencyBurningTxCreationRequest request = getCurrencyBurningTxCreationRequest(rsAccount, 1L, 20_000L);
        doReturn(senderAccount).when(accountService).getAccount(senderId);

        doFailingCurrencyBurning("Not enough currency funds", request);
    }

    @Test
    void currencyBurningTx_notEnoughCurrencyBalance() {
        CurrencyBurningTxCreationRequest request = getCurrencyBurningTxCreationRequest(rsAccount, 1L, 20_000L);
        doReturn(senderAccount).when(accountService).getAccount(senderId);
        doReturn(new AccountCurrency(senderId, 1L, 20_100, 19999, 10)).when(accountCurrencyService).getAccountCurrency(senderId, 1L);

        doFailingCurrencyBurning("Not enough currency funds", request);
    }

    private void doFailingCurrencyBurning(String errorMessage, CurrencyBurningTxCreationRequest request) {
        Response result = apiService.currencyBurningTx(request, null);

        assertEquals(400, result.getStatus());
        ErrorResponse errorResponse = (ErrorResponse) result.getEntity();
        assertEquals(errorMessage, errorResponse.getErrorDescription());
        verifyNoInteractions(creator);
    }


    private CurrencyBurningTxCreationRequest getCurrencyBurningTxCreationRequest(String rsAccount, Long currencyId, Long burningAmount) {
        CurrencyBurningTxCreationRequest request = new CurrencyBurningTxCreationRequest();
        request.setSecretPhrase("123");
        request.setSender(rsAccount);
        request.setBroadcast(true);
        request.setCode2FA(239_123);
        request.setDeadline(2223);
        request.setBurningAmount(burningAmount);
        request.setFee(1_000_000L);
        request.setCurrencyId(currencyId == null ? null : currencyId.toString());
        return request;
    }

    private TransactionCreationResponse getTransactionCreationResponse() {
        TransactionCreationResponse response = new TransactionCreationResponse();
        response.setUnsignedTransactionBytes("0000");
        response.setTransactionBytes("ffff");
        response.setId("1212");
        response.setFullHash("11223344");
        response.setSignature("0aa94bf1");
        response.setBroadcasted(true);
        return response;
    }

    private CreateTransactionRequest newTxCreatorRequest(Account senderAccount) {
        return CreateTransactionRequest.builder()
            .senderAccount(senderAccount)
            .recipientId(0)
            .amountATM(0)
            .feeATM(1_000_000L)
            .attachment(new MonetarySystemCurrencyBurningAttachment(1, 20_000))
            .deadlineValue("2223")
            .broadcast(true)
            .secretPhrase("123")
            .validate(true)
            .version(1)
            .build();
    }
}