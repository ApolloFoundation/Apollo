/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.NotFoundException;
import com.apollocurrency.aplwallet.api.v2.SmcApiService;
import com.apollocurrency.aplwallet.api.v2.model.PublishContractReqTest;
import com.apollocurrency.aplwallet.api.v2.model.TransactionArrayResp;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.rest.ApiErrors;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.rest.v2.ResponseBuilderV2;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcPublishContractAttachment;
import com.apollocurrency.aplwallet.apl.core.utils.Convert2;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.smc.blockchain.crypt.CryptoLibProvider;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

/**
 * @author andrew.zinchenko@gmail.com
 */
@RequestScoped
public class SmcApiServiceImpl implements SmcApiService {

    private final AccountService accountService;
    private final TransactionCreator transactionCreator;
    private final CryptoLibProvider cryptoLibProvider;

    @Inject
    public SmcApiServiceImpl(AccountService accountService, TransactionCreator transactionCreator, CryptoLibProvider cryptoLibProvider) {
        this.accountService = accountService;
        this.transactionCreator = transactionCreator;
        this.cryptoLibProvider = cryptoLibProvider;
    }

    @Override
    public Response createPublishContractTxTest(PublishContractReqTest body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();

        //return ResponseBuilderV2.apiError(ApiErrors.INCORRECT_PARAM_VALUE, "child_count").build();
        long senderAccountId = Convert.parseAccountId(body.getSender());
        Account account = accountService.getAccount(senderAccountId);
        if (account == null) {
            return ResponseBuilderV2.apiError(ApiErrors.INCORRECT_VALUE, "parent_account", body.getSender()).build();
        }
        TransactionArrayResp response = new TransactionArrayResp();

        BigInteger fuelLimit = new BigInteger(body.getFuelLimit());
        BigInteger fuelPrice = new BigInteger(body.getFuelPrice());

        SmcPublishContractAttachment attachment = SmcPublishContractAttachment.builder()
            .contractSource(body.getSource())
            .constructorParams(body.getParams())
            .build();

        String publicKey = Convert.toHexString(
            Crypto.getPublicKey(
                Crypto.getKeySeed(
                    Convert2.rsAccount(account.getId())
                    , Crypto.sha256().digest(attachment.getContractSource().getBytes(StandardCharsets.UTF_8))
                    , Crypto.getSecureRandom().generateSeed(32)
                )
            )
        );

        CreateTransactionRequest txRequest = CreateTransactionRequest.builder()
            .version(3)
            .senderAccount(account)
            .recipientPublicKey(publicKey)
            .secretPhrase(body.getSecret())
            .deadlineValue("1440")
            .amountATM(0)
            .feeATM(fuelLimit.multiply(fuelPrice).longValueExact())
            .attachment(attachment)
            .broadcast(false)
            .validate(false)
            .build();

        Transaction transaction = transactionCreator.createTransactionThrowingException(txRequest);

        response.setTx(
            Convert.toHexString(
                transaction.getCopyTxBytes()
            )
        );

        return builder.bind(response).build();
    }
}
