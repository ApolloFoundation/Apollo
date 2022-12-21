/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.FailedTxsApiService;
import com.apollocurrency.aplwallet.api.v2.NotFoundException;
import com.apollocurrency.aplwallet.api.v2.model.LastTransactionVerificationResponse;
import com.apollocurrency.aplwallet.api.v2.model.TransactionVerification;
import com.apollocurrency.aplwallet.api.v2.model.TransactionVerificationResponse;
import com.apollocurrency.aplwallet.apl.core.model.TxsVerificationResult;
import com.apollocurrency.aplwallet.apl.core.model.VerifiedTransaction;
import com.apollocurrency.aplwallet.apl.core.rest.v2.ResponseBuilderV2;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.FailedTransactionVerificationService;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Failed txs API implementation
 * @author Andrii Boiarskyi
 * @see com.apollocurrency.aplwallet.api.v2.FailedTxsApi
 * @see com.apollocurrency.aplwallet.api.v2.FailedTxsApiService
 * @since 1.48.4
 */
@Singleton
public class FailedTxsApiServiceImpl implements FailedTxsApiService {
    private final FailedTransactionVerificationService service;

    @Inject
    public FailedTxsApiServiceImpl(FailedTransactionVerificationService service) {
        this.service = service;
    }

    @Override
    public Response verifyFailedTransaction(String id, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builderV2 = ResponseBuilderV2.startTiming();
        long txId = Long.parseUnsignedLong(id);
        TxsVerificationResult result = service.verifyTransaction(txId);
        TransactionVerificationResponse response = new TransactionVerificationResponse();
        Optional<VerifiedTransaction> verificationResultOpt = result.get(txId);
        if (verificationResultOpt.isPresent()) {
            VerifiedTransaction verifiedTransaction = verificationResultOpt.get();
            response.setId(Long.toUnsignedString(verifiedTransaction.getId()));
            response.setMessage(verifiedTransaction.getError());
            response.setVerificationCount(verifiedTransaction.getCount());
            response.setVerified(verifiedTransaction.isVerified());
        }
        return builderV2.bind(response).build();
    }

    @Override
    public Response verifyFailedTransactions(SecurityContext securityContext) throws NotFoundException {
        return txsVerificationResultToResponse(service::verifyTransactions);
    }

    @Override
    public Response getVerifiedFailedTransactions(SecurityContext securityContext) {
        return txsVerificationResultToResponse(service::getLastVerificationResult);
    }

    private Response txsVerificationResultToResponse(Supplier<Optional<TxsVerificationResult>> supplier) {
        ResponseBuilderV2 builderV2 = ResponseBuilderV2.startTiming();
        ArrayList<TransactionVerification> verifications = new ArrayList<>();
        LastTransactionVerificationResponse response = new LastTransactionVerificationResponse();
        Optional<TxsVerificationResult> resultOpt = supplier.get();
        if (resultOpt.isPresent()) {
            TxsVerificationResult result = resultOpt.get();
            List<VerifiedTransaction> all = result.all();
            all.forEach(e-> {
                TransactionVerification verification = mapToDTO(e);
                verifications.add(verification);
            });
            response.setFromHeight(result.getFromHeight());
            response.setToHeight(result.getToHeight());
        }
        response.setVerificationResults(verifications);
        return builderV2.bind(response).build();
    }


    private TransactionVerification mapToDTO(VerifiedTransaction e) {
        TransactionVerification verification = new TransactionVerification();
        verification.setId(Long.toUnsignedString(e.getId()));
        verification.setMessage(e.getError());
        verification.setVerificationCount(e.getCount());
        verification.setVerified(e.isVerified());
        return verification;
    }
}
