/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.NotFoundException;
import com.apollocurrency.aplwallet.api.v2.SmcApiService;
import com.apollocurrency.aplwallet.api.v2.model.CallContractMethodReqTest;
import com.apollocurrency.aplwallet.api.v2.model.PublishContractReqTest;
import com.apollocurrency.aplwallet.api.v2.model.TransactionArrayResp;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.rest.v2.ResponseBuilderV2;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.signature.MultiSigCredential;
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxBContext;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcCallMethodAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcPublishContractAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.exception.ApiErrors;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import com.apollocurrency.smc.blockchain.crypt.CryptoLibProvider;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.math.BigInteger;

/**
 * @author andrew.zinchenko@gmail.com
 */
@RequestScoped
public class SmcApiServiceImpl implements SmcApiService {

    private final BlockchainConfig blockchainConfig;
    private final AccountService accountService;
    private final TransactionCreator transactionCreator;
    private final CryptoLibProvider cryptoLibProvider;
    private final TxBContext txBContext;

    @Inject
    public SmcApiServiceImpl(BlockchainConfig blockchainConfig, AccountService accountService, TransactionCreator transactionCreator, CryptoLibProvider cryptoLibProvider) {
        this.blockchainConfig = blockchainConfig;
        this.accountService = accountService;
        this.transactionCreator = transactionCreator;
        this.cryptoLibProvider = cryptoLibProvider;
        this.txBContext = TxBContext.newInstance(blockchainConfig.getChain());
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
            .contractName(body.getName())
            .contractSource(body.getSource())
            .constructorParams(String.join(",", body.getParams()))
            .languageName("javascript")
            .fuelLimit(fuelLimit)
            .fuelPrice(fuelPrice)
            .build();

        byte[] publicKey = AccountService.generatePublicKey(account, attachment.getContractSource());
        long recipientId = AccountService.getId(publicKey);

        CreateTransactionRequest txRequest = CreateTransactionRequest.builder()
            .version(2)
            .senderAccount(account)
            .recipientPublicKey(Convert.toHexString(publicKey))
            .recipientId(recipientId)
            .secretPhrase(body.getSecret())
            .deadlineValue(String.valueOf(1440))
            .attachment(attachment)
            .credential(new MultiSigCredential(1, Crypto.getKeySeed(body.getSecret())))
            .broadcast(false)
            .validate(false)
            .build();

        Transaction transaction = transactionCreator.createTransactionThrowingException(txRequest);

        Result signedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(transaction.getVersion()).serialize(transaction, signedTxBytes);
        response.setTx(Convert.toHexString(signedTxBytes.array()));

        return builder.bind(response).build();
    }

    @Override
    public Response createCallContractMethodTx(CallContractMethodReqTest body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();

        long address = Convert.parseAccountId(body.getAddress());
        Account contractAccount = accountService.getAccount(address);
        if (contractAccount == null) {
            return ResponseBuilderV2.apiError(ApiErrors.INCORRECT_VALUE, "contract_address", body.getAddress()).build();
        }
        long senderAccountId = Convert.parseAccountId(body.getSender());
        Account account = accountService.getAccount(senderAccountId);
        if (account == null) {
            return ResponseBuilderV2.apiError(ApiErrors.INCORRECT_VALUE, "sender_account", body.getSender()).build();
        }
        TransactionArrayResp response = new TransactionArrayResp();

        BigInteger fuelLimit = new BigInteger(body.getFuelLimit());
        BigInteger fuelPrice = new BigInteger(body.getFuelPrice());

        SmcCallMethodAttachment attachment = SmcCallMethodAttachment.builder()
            .methodName(body.getName())
            .methodParams(String.join(",", body.getParams()))
            .fuelLimit(fuelLimit)
            .fuelPrice(fuelPrice)
            .build();

        CreateTransactionRequest txRequest = CreateTransactionRequest.builder()
            .version(2)
            .senderAccount(account)
            .recipientPublicKey(Convert.toHexString(contractAccount.getPublicKey().getPublicKey()))
            .recipientId(contractAccount.getId())
            .secretPhrase(body.getSecret())
            .deadlineValue(String.valueOf(1440))
            .attachment(attachment)
            .credential(new MultiSigCredential(1, Crypto.getKeySeed(body.getSecret())))
            .broadcast(false)
            .validate(false)
            .build();

        Transaction transaction = transactionCreator.createTransactionThrowingException(txRequest);

        Result signedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(transaction.getVersion()).serialize(transaction, signedTxBytes);
        response.setTx(Convert.toHexString(signedTxBytes.array()));

        return builder.bind(response).build();
    }

    @Override
    public Response createPublishContractTx(PublishContractReqTest body, SecurityContext securityContext) throws NotFoundException {
        return null;
    }
}
