/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.NotFoundException;
import com.apollocurrency.aplwallet.api.v2.SmcApiService;
import com.apollocurrency.aplwallet.api.v2.model.CallContractMethodReqTest;
import com.apollocurrency.aplwallet.api.v2.model.ContractDetails;
import com.apollocurrency.aplwallet.api.v2.model.ContractListResponse;
import com.apollocurrency.aplwallet.api.v2.model.ContractStateResponse;
import com.apollocurrency.aplwallet.api.v2.model.PublishContractReqTest;
import com.apollocurrency.aplwallet.api.v2.model.TransactionArrayResp;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.model.smc.AplAddress;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.rest.v2.ResponseBuilderV2;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractService;
import com.apollocurrency.aplwallet.apl.core.signature.MultiSigCredential;
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxBContext;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcCallMethodAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcPublishContractAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.api.parameter.FirstLastIndexBeanParam;
import com.apollocurrency.aplwallet.apl.util.cdi.config.Property;
import com.apollocurrency.aplwallet.apl.util.exception.ApiErrors;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import com.apollocurrency.smc.contract.ContractStatus;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.math.BigInteger;
import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@RequestScoped
public class SmcApiServiceImpl implements SmcApiService {

    private final AccountService accountService;
    private final ContractService contractService;
    private final TransactionCreator transactionCreator;
    private final TxBContext txBContext;
    private final int maxAPIrecords;

    @Inject
    public SmcApiServiceImpl(BlockchainConfig blockchainConfig,
                             AccountService accountService,
                             ContractService contractService,
                             TransactionCreator transactionCreator,
                             @Property(name = "apl.maxAPIRecords", defaultValue = "100") int maxAPIrecords) {
        this.accountService = accountService;
        this.contractService = contractService;
        this.transactionCreator = transactionCreator;
        this.txBContext = TxBContext.newInstance(blockchainConfig.getChain());
        this.maxAPIrecords = maxAPIrecords;
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
        String valueStr = body.getValue() != null ? body.getValue() : "0";

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
            .amountATM(Convert.parseLong(valueStr))
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

        log.debug("Transaction id={} sender={} fee={}", Convert.toHexString(transaction.getId()), Convert.toHexString(senderAccountId), transaction.getFeeATM());

        return builder.bind(response).build();
    }

    @Override
    public Response createCallContractMethodTxTest(CallContractMethodReqTest body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();

        long address = Convert.parseAccountId(body.getAddress());
        byte[] contractPublicKey;
        Account contractAccount = accountService.getAccount(address);
        if (contractAccount == null) {
            return ResponseBuilderV2.apiError(ApiErrors.INCORRECT_VALUE, "contract_address", body.getAddress()).build();
        }
        if (contractAccount.getPublicKey() == null) {
            contractPublicKey = accountService.getPublicKeyByteArray(contractAccount.getId());
        } else {
            contractPublicKey = contractAccount.getPublicKey().getPublicKey();
        }
        long senderAccountId = Convert.parseAccountId(body.getSender());
        Account account = accountService.getAccount(senderAccountId);
        if (account == null) {
            return ResponseBuilderV2.apiError(ApiErrors.INCORRECT_VALUE, "sender_account", body.getSender()).build();
        }
        TransactionArrayResp response = new TransactionArrayResp();

        BigInteger fuelLimit = new BigInteger(body.getFuelLimit());
        BigInteger fuelPrice = new BigInteger(body.getFuelPrice());
        String valueStr = body.getValue() != null ? body.getValue() : "0";

        SmcCallMethodAttachment attachment = SmcCallMethodAttachment.builder()
            .methodName(body.getName())
            .methodParams(String.join(",", body.getParams()))
            .fuelLimit(fuelLimit)
            .fuelPrice(fuelPrice)
            .build();

        CreateTransactionRequest txRequest = CreateTransactionRequest.builder()
            .version(2)
            .amountATM(Convert.parseLong(valueStr))
            .senderAccount(account)
            .recipientPublicKey(Convert.toHexString(contractPublicKey))
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

        log.debug("Transaction id={} contract={} fee={}", Convert.toHexString(transaction.getId()), Convert.toHexString(contractAccount.getId()), transaction.getFeeATM());

        return builder.bind(response).build();
    }

    @Override
    public Response createCallContractMethodTx(CallContractMethodReqTest body, SecurityContext securityContext) throws NotFoundException {
        return ResponseBuilderV2.apiError(ApiErrors.CUSTOM_ERROR_MESSAGE, "Not implemented yet").build();
    }

    @Override
    public Response createPublishContractTx(PublishContractReqTest body, SecurityContext securityContext) throws NotFoundException {
        return ResponseBuilderV2.apiError(ApiErrors.CUSTOM_ERROR_MESSAGE, "Not implemented yet").build();
    }

    @Override
    public Response getSmcByOwnerAccount(String accountStr, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();

        AplAddress address = new AplAddress(Convert.parseAccountId(accountStr));
        Account account = accountService.getAccount(address.getLongId());
        if (account == null) {
            return ResponseBuilderV2.apiError(ApiErrors.INCORRECT_VALUE, "account", accountStr).build();
        }

        ContractListResponse response = new ContractListResponse();

        List<ContractDetails> contracts = contractService.loadContractsByOwner(address, 0, Integer.MAX_VALUE);
        response.setContracts(contracts);

        return builder.bind(response).build();
    }

    @Override
    public Response getSmcByAddress(String addressStr, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();

        AplAddress address = new AplAddress(Convert.parseAccountId(addressStr));
        Account account = accountService.getAccount(address.getLongId());
        if (account == null) {
            return ResponseBuilderV2.apiError(ApiErrors.INCORRECT_VALUE, "address", addressStr).build();
        }
        if (!contractService.isContractExist(address)) {
            return ResponseBuilderV2.apiError(ApiErrors.CONTRACT_NOT_FOUND, addressStr).build();
        }
        ContractListResponse response = new ContractListResponse();

        ContractDetails contract = contractService.getContractDetailsByAddress(address);
        response.setContracts(List.of(contract));

        return builder.bind(response).build();
    }

    @Override
    public Response getSmcList(String addressStr, String publisherStr, String name, String status, Integer firstIndex, Integer lastIndex, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();

        FirstLastIndexBeanParam indexBeanParam = new FirstLastIndexBeanParam(firstIndex, lastIndex);
        indexBeanParam.adjustIndexes(maxAPIrecords);
        AplAddress address = null;
        AplAddress publisher = null;

        ContractStatus smcStatus = null;
        if (status != null) {
            try {
                smcStatus = ContractStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                return ResponseBuilderV2.apiError(ApiErrors.INCORRECT_VALUE, "status", addressStr).build();
            }
        }

        if (addressStr != null) {
            address = new AplAddress(Convert.parseAccountId(addressStr));
            Account account = accountService.getAccount(address.getLongId());
            if (account == null) {
                return ResponseBuilderV2.apiError(ApiErrors.INCORRECT_VALUE, "address", addressStr).build();
            }
            if (!contractService.isContractExist(address)) {
                return ResponseBuilderV2.apiError(ApiErrors.CONTRACT_NOT_FOUND, addressStr).build();
            }
        }

        if (publisherStr != null) {
            publisher = new AplAddress(Convert.parseAccountId(publisherStr));
            Account account = accountService.getAccount(publisher.getLongId());
            if (account == null) {
                return ResponseBuilderV2.apiError(ApiErrors.INCORRECT_VALUE, "publisher", publisherStr).build();
            }
        }
        ContractListResponse response = new ContractListResponse();

        List<ContractDetails> contracts = contractService.loadContractsByFilter(
            address,
            publisher,
            name,
            smcStatus,
            -1,
            indexBeanParam.getFirstIndex(),
            indexBeanParam.getLastIndex()
        );

        response.setContracts(contracts);

        return builder.bind(response).build();
    }

    @Override
    public Response getSmcStateByAddress(String addressStr, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();

        AplAddress address = new AplAddress(Convert.parseAccountId(addressStr));
        Account account = accountService.getAccount(address.getLongId());
        if (account == null) {
            return ResponseBuilderV2.apiError(ApiErrors.INCORRECT_VALUE, "address", addressStr).build();
        }
        if (!contractService.isContractExist(address)) {
            return ResponseBuilderV2.apiError(ApiErrors.CONTRACT_NOT_FOUND, addressStr).build();
        }
        ContractStateResponse response = new ContractStateResponse();

        String contractState = contractService.loadSerializedContract(address);
        response.setState(contractState);

        return builder.bind(response).build();
    }
}
