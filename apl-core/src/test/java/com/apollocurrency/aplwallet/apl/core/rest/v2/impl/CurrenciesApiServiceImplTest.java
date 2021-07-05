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
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyBurningAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.exception.RestParameterException;
import com.apollocurrency.aplwallet.vault.service.KMSService;
import com.apollocurrency.aplwallet.vault.service.auth.Account2FAService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    Account2FAService account2FAService;
    @Mock
    KMSService kmsService;
    @Mock
    AccountService accountService;
    @Mock
    BlockchainConfig blockchainConfig;
    @InjectMocks
    CurrenciesApiServiceImpl apiService;


    long senderId         ;
    String rsAccount      ;
    Account senderAccount ;
    byte[] publicKey;
    private String secretPhrase;
    private String passphrase;

    @BeforeEach
    void setUp() {
        Convert2.init("APL", 0);
        secretPhrase = "123";
        passphrase = "12345";
        publicKey = Crypto.getPublicKey(secretPhrase);
        senderId = AccountService.getId(publicKey);
        rsAccount = Convert2.rsAccount(senderId);
        senderAccount = new Account(senderId, 0);
    }

    @Test
    void currencyBurningTx_StandardOK() {

        CurrencyBurningTxCreationRequest request = secretPhraseRequest(rsAccount, 1L, 20_000L);
        doReturn(senderAccount).when(accountService).getAccount(publicKey);
        doReturn(new AccountCurrency(senderId, 1L, 20_100, 20_000, 10)).when(accountCurrencyService).getAccountCurrency(senderId, 1L);
        TransactionCreationResponse response = getTransactionCreationResponse();
        doReturn(response).when(creator).createApiV2Transaction(newStandardTxCreatorRequest(senderAccount));

        Response result = apiService.currencyBurningTx(request, null);

        assertEquals(200, result.getStatus());
        assertEquals(response, result.getEntity());
        verify(account2FAService).verify2FA(rsAccount, null, secretPhrase, null, 239_123);
    }

    @Test
    void currencyBurningTx_VaultOK() {

        CurrencyBurningTxCreationRequest request = passphraseRequest(rsAccount, 1L, 20_000L);
        doReturn(secretPhrase.getBytes()).when(kmsService).getAplSecretBytes(senderId, passphrase);
        doReturn(senderAccount).when(accountService).getAccount(publicKey);
        doReturn(new AccountCurrency(senderId, 1L, 20_100, 20_000, 10)).when(accountCurrencyService).getAccountCurrency(senderId, 1L);
        TransactionCreationResponse response = getTransactionCreationResponse();
        CreateTransactionRequest creationRequest = newVaultTxCreatorRequest();
        doReturn(response).when(creator).createApiV2Transaction(creationRequest);

        Response result = apiService.currencyBurningTx(request, null);

        assertEquals(200, result.getStatus());
        assertEquals(response, result.getEntity());
        verify(account2FAService).verify2FA(rsAccount, passphrase, null, null, 239_123);
    }

    @Test
    void currencyBurningTx_VaultNoSender() {

        CurrencyBurningTxCreationRequest request = passphraseRequest(rsAccount, 1L, 20_000L);
        request.setSender(null);

        doFailingExCurrencyBurning("At least one of [secretPhrase,publicKey,passphrase] must be specified", request);
    }

    @Test
    void currencyBurningTx_PublicKeyOK() {

        CurrencyBurningTxCreationRequest request = publicKeyRequest(rsAccount, 1L, 20_000L);
        doReturn(senderAccount).when(accountService).getAccount(publicKey);
        doReturn(new AccountCurrency(senderId, 1L, 20_100, 20_000, 10)).when(accountCurrencyService).getAccountCurrency(senderId, 1L);
        TransactionCreationResponse response = getTransactionCreationResponse();
        CreateTransactionRequest creationRequest = newPublicKeyTxCreatorRequest();
        doReturn(response).when(creator).createApiV2Transaction(creationRequest);

        Response result = apiService.currencyBurningTx(request, null);

        assertEquals(200, result.getStatus());
        assertEquals(response, result.getEntity());
        verify(account2FAService).verify2FA(rsAccount, null, null, Convert.toHexString(publicKey), 239_123);
    }

    @Test
    void currencyBurningTx_PublicKeyIsNotCanonical() {

        CurrencyBurningTxCreationRequest request = publicKeyRequest(rsAccount, 1L, 20_000L);
        request.setPublicKey(Convert.toHexString(new byte[3]));

        doFailingExCurrencyBurning("Incorrect 'publicKey'", request);
    }

    @Test
    void currencyBurningTx_noSendersAccount() {
        CurrencyBurningTxCreationRequest request = secretPhraseRequest(rsAccount, 1L, 20_000L);

        doFailingCurrencyBurning("Unknown 'sender' : Sender specified by the public key: f8bed2de2b8b2799902e864761c3987557a82280c31e276bce084d58968f5624 is not found", request);
    }

    @Test
    void currencyBurningTx_missingCurrencyIdParameter() {
        CurrencyBurningTxCreationRequest request = secretPhraseRequest(rsAccount, null, 20_000L);
        doReturn(senderAccount).when(accountService).getAccount(publicKey);

        doFailingCurrencyBurning("The mandatory parameter 'currencyId' is not specified", request);
    }

    @Test
    void currencyBurningTx_missingBurningAmountParameter() {
        CurrencyBurningTxCreationRequest request = secretPhraseRequest(rsAccount, 1L, null);
        doReturn(senderAccount).when(accountService).getAccount(publicKey);

        doFailingCurrencyBurning("The mandatory parameter 'burningAmount' is not specified", request);
    }

    @Test
    void currencyBurningTx_notValidBurningAmountParameter() {
        CurrencyBurningTxCreationRequest request = secretPhraseRequest(rsAccount, 1L, 0L);
        doReturn(senderAccount).when(accountService).getAccount(publicKey);

        doFailingCurrencyBurning("burningAmount is not in range [1..9,223,372,036,854,775,807]", request);
    }

    @Test
    void currencyBurningTx_absentCurrencyBalance() {
        CurrencyBurningTxCreationRequest request = secretPhraseRequest(rsAccount, 1L, 20_000L);
        doReturn(senderAccount).when(accountService).getAccount(publicKey);

        doFailingCurrencyBurning("Not enough currency funds", request);
    }

    @Test
    void currencyBurningTx_notEnoughCurrencyBalance() {
        CurrencyBurningTxCreationRequest request = secretPhraseRequest(rsAccount, 1L, 20_000L);
        doReturn(senderAccount).when(accountService).getAccount(publicKey);
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

    private void doFailingExCurrencyBurning(String errorMessage, CurrencyBurningTxCreationRequest request) {
        RestParameterException ex = assertThrows(RestParameterException.class, () -> apiService.currencyBurningTx(request, null));

        assertEquals(errorMessage, ex.getMessage());
        verifyNoInteractions(creator);
    }


    private CurrencyBurningTxCreationRequest secretPhraseRequest(String rsAccount, Long currencyId, Long burningAmount) {
        CurrencyBurningTxCreationRequest request = new CurrencyBurningTxCreationRequest();
        request.setSecretPhrase(secretPhrase);
        request.setSender(rsAccount);
        request.setBroadcast(true);
        request.setCode2FA(239_123);
        request.setDeadline(2223);
        request.setBurningAmount(burningAmount);
        request.setFee(1_000_000L);
        request.setCurrencyId(currencyId == null ? null : currencyId.toString());
        return request;
    }

    private CurrencyBurningTxCreationRequest passphraseRequest(String rsAccount, Long currencyId, Long burningAmount) {
        CurrencyBurningTxCreationRequest request = secretPhraseRequest(rsAccount, currencyId, burningAmount);
        request.setSecretPhrase(null);
        request.setPassphrase("12345");
        return request;
    }
    private CurrencyBurningTxCreationRequest publicKeyRequest(String rsAccount, Long currencyId, Long burningAmount) {
        CurrencyBurningTxCreationRequest request = secretPhraseRequest(rsAccount, currencyId, burningAmount);
        request.setSecretPhrase(null);
        request.setPublicKey(Convert.toHexString(publicKey));
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

    private CreateTransactionRequest newStandardTxCreatorRequest(Account senderAccount) {
        return CreateTransactionRequest.builder()
            .senderAccount(senderAccount)
            .recipientId(0)
            .amountATM(0)
            .feeATM(1_000_000L)
            .attachment(new MonetarySystemCurrencyBurningAttachment(1, 20_000))
            .deadlineValue("2223")
            .broadcast(true)
            .secretPhrase(secretPhrase)
            .validate(true)
            .version(1)
            .build();
    }

    private CreateTransactionRequest newVaultTxCreatorRequest() {
        CreateTransactionRequest request = newStandardTxCreatorRequest(senderAccount);
        request.setSecretPhrase(null);
        request.setPassphrase(passphrase);
        return request;
    }

    private CreateTransactionRequest newPublicKeyTxCreatorRequest() {
        CreateTransactionRequest request = newStandardTxCreatorRequest(senderAccount);
        request.setSecretPhrase(null);
        request.setPublicKey(publicKey);
        return request;
    }
}