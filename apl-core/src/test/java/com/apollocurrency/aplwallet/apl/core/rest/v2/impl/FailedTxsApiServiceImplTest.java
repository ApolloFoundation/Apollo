/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.FailedTxsApiService;
import com.apollocurrency.aplwallet.api.v2.model.LastTransactionVerificationResponse;
import com.apollocurrency.aplwallet.api.v2.model.TransactionVerification;
import com.apollocurrency.aplwallet.api.v2.model.TransactionVerificationResponse;
import com.apollocurrency.aplwallet.apl.core.model.TxsVerificationResult;
import com.apollocurrency.aplwallet.apl.core.model.VerifiedTransaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.FailedTransactionVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class FailedTxsApiServiceImplTest {
    @Mock
    FailedTransactionVerificationService verificationService;
    @Mock
    SecurityContext securityContext;
    VerifiedTransaction tx1 = new VerifiedTransaction(-1L, "Test error", 3, true);
    VerifiedTransaction tx2 = new VerifiedTransaction(-2L, "Test error", 0, false);
    VerifiedTransaction tx3 = new VerifiedTransaction(2L, "Test error", 3, true);


    FailedTxsApiService apiService;

    @BeforeEach
    void setUp() {
        apiService = new FailedTxsApiServiceImpl(verificationService);
    }

    @Test
    void verifyFailedTransaction_incorrectId() {
        assertThrows(IllegalArgumentException.class, () -> apiService.verifyFailedTransaction("incorrect-id", securityContext));
    }

    @Test
    void verifyFailedTransaction_emptyResponse() {
        TxsVerificationResult emptyResult = new TxsVerificationResult();
        doReturn(emptyResult).when(verificationService).verifyTransaction(-1);

        Response response = apiService.verifyFailedTransaction("18446744073709551615", securityContext);

        TransactionVerificationResponse responseEntity = (TransactionVerificationResponse) response.getEntity();

        assertNull(responseEntity.getId(), "Response entity should be empty");
    }

    @Test
    void verifyFailedTransaction_OK() {
        TxsVerificationResult result = new TxsVerificationResult(Map.of(-1L, tx1));
        doReturn(result).when(verificationService).verifyTransaction(-1);

        Response response = apiService.verifyFailedTransaction("18446744073709551615", securityContext);

        TransactionVerificationResponse responseEntity = (TransactionVerificationResponse) response.getEntity();

        verifyEqual(tx1, responseEntity);
    }

    @Test
    void verifyFailedTransactions_OK() {
        TxsVerificationResult result = mockTxVerificationResult();
        doReturn(Optional.of(result)).when(verificationService).verifyTransactions();

        Response response = apiService.verifyFailedTransactions(securityContext);

        verifyTransactionsVerificationResponse(response);
    }

    @Test
    void verifyFailedTransactions_empty() {
        doReturn(Optional.empty()).when(verificationService).verifyTransactions();

        Response response = apiService.verifyFailedTransactions(securityContext);

        LastTransactionVerificationResponse responseEntity = (LastTransactionVerificationResponse) response.getEntity();

        assertTrue(responseEntity.getVerificationResults().isEmpty(), "Response should be empty, when no verified transactions supplied");
        assertNull(responseEntity.getFromHeight(), "fromHeight should not be set, when not verification result supplied");
        assertNull(responseEntity.getToHeight(), "toHeight should not be set, when not verification result supplied");
    }

    @Test
    void getVerifiedFailedTransactions_OK() {
        TxsVerificationResult result = mockTxVerificationResult();
        doReturn(Optional.of(result)).when(verificationService).getLastVerificationResult();

        Response response = apiService.getVerifiedFailedTransactions(securityContext);

        verifyTransactionsVerificationResponse(response);
    }

    private void verifyTransactionsVerificationResponse(Response response) {
        LastTransactionVerificationResponse responseEntity = (LastTransactionVerificationResponse) response.getEntity();
        verifyContainsAll(responseEntity, List.of(tx1, tx2, tx3));
        assertEquals(1000, responseEntity.getFromHeight());
        assertEquals(1720, responseEntity.getToHeight());
    }

    private TxsVerificationResult mockTxVerificationResult() {
        TxsVerificationResult result = new TxsVerificationResult(Map.of(tx1.getId(), tx1, tx2.getId(), tx2, tx3.getId(), tx3));
        result.setToHeight(1720);
        result.setFromHeight(1000);
        return result;
    }

    private void verifyEqual(VerifiedTransaction verifiedTransaction, TransactionVerificationResponse responseEntity) {
        assertEquals(Long.toUnsignedString(verifiedTransaction.getId()), responseEntity.getId());
        assertEquals(verifiedTransaction.getError(), responseEntity.getMessage());
        assertEquals(verifiedTransaction.isVerified(), responseEntity.isVerified());
        assertEquals(verifiedTransaction.getCount(), responseEntity.getVerificationCount());
    }

    private void verifyContainsAll(LastTransactionVerificationResponse response, List<VerifiedTransaction> verifiedTransactions) {
        verifiedTransactions.forEach(e-> {
            Optional<TransactionVerification> foundOpt = response.getVerificationResults().stream().filter(existing -> existing.getId().equals(Long.toUnsignedString(e.getId()))).findFirst();
            if (foundOpt.isEmpty()) {
                fail("Transaction with id = " + e.getId() + " was not found inside: " + response);
            }
            TransactionVerification verification = foundOpt.get();
            assertEquals(Long.toUnsignedString(e.getId()), verification.getId());
            assertEquals(e.getError(), verification.getMessage());
            assertEquals(e.isVerified(), verification.isVerified());
            assertEquals(e.getCount(), verification.getVerificationCount());
        });
    }
}