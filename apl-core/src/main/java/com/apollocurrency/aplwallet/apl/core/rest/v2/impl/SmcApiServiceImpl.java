/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.NotFoundException;
import com.apollocurrency.aplwallet.api.v2.SmcApiService;
import com.apollocurrency.aplwallet.api.v2.model.CallContractMethodReq;
import com.apollocurrency.aplwallet.api.v2.model.ContractDetails;
import com.apollocurrency.aplwallet.api.v2.model.ContractListResponse;
import com.apollocurrency.aplwallet.api.v2.model.ContractStateResponse;
import com.apollocurrency.aplwallet.api.v2.model.PublishContractReq;
import com.apollocurrency.aplwallet.api.v2.model.TransactionArrayResp;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.model.smc.AplAddress;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.rest.v2.ResponseBuilderV2;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcBlockchainIntegratorFactory;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractTxProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.internal.SandboxCallMethodValidationProcessorContract;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.internal.SandboxPublishContractValidationProcessor;
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
import com.apollocurrency.smc.blockchain.BlockchainIntegrator;
import com.apollocurrency.smc.blockchain.ContractNotFoundException;
import com.apollocurrency.smc.contract.ContractStatus;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.SmartMethod;
import com.apollocurrency.smc.contract.fuel.ContractFuel;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.google.common.base.Strings;
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
    private final SmcContractService contractService;
    private final TransactionCreator transactionCreator;
    private final TxBContext txBContext;
    private final SmcBlockchainIntegratorFactory integratorFactory;
    private final SmcConfig smcConfig;
    private final int maxAPIrecords;

    @Inject
    public SmcApiServiceImpl(BlockchainConfig blockchainConfig,
                             AccountService accountService,
                             SmcContractService contractService,
                             TransactionCreator transactionCreator,
                             SmcBlockchainIntegratorFactory integratorFactory,
                             SmcConfig smcConfig,
                             @Property(name = "apl.maxAPIRecords", defaultValue = "100") int maxAPIrecords) {
        this.accountService = accountService;
        this.contractService = contractService;
        this.transactionCreator = transactionCreator;
        this.txBContext = TxBContext.newInstance(blockchainConfig.getChain());
        this.integratorFactory = integratorFactory;
        this.smcConfig = smcConfig;
        this.maxAPIrecords = maxAPIrecords;
    }

    @Override
    public Response createPublishContractTx(PublishContractReq body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        Transaction transaction = validateAndCreatePublishContractTransaction(body, builder);
        if (transaction == null) {
            return builder.build();
        }
        TransactionArrayResp response = new TransactionArrayResp();

        Result signedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(transaction.getVersion()).serialize(transaction, signedTxBytes);
        response.setTx(Convert.toHexString(signedTxBytes.array()));

        return builder.bind(response).build();
    }

    @Override
    public Response validatePublishContractTx(PublishContractReq body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        Transaction transaction = validateAndCreatePublishContractTransaction(body, builder);
        if (transaction == null) {
            return builder.build();
        }
        SmartContract smartContract = contractService.createNewContract(transaction);

        //syntactical and semantic validation
        BlockchainIntegrator integrator = integratorFactory.createMockProcessor(transaction.getId());
        SmcContractTxProcessor processor = new SandboxPublishContractValidationProcessor(smartContract, integrator, smcConfig);

        BigInteger calculatedFuel = processor.getExecutionEnv().getPrice().forContractPublishing().calc(smartContract);
        if (!smartContract.getFuel().tryToCharge(calculatedFuel)) {
            log.error("Needed fuel={} but actual={}", calculatedFuel, smartContract.getFuel());
            return builder.error(ApiErrors.CONTRACT_VALIDATION_ERROR, "Not enough fuel to execute this transaction, expected=" + calculatedFuel + " but actual=" + smartContract.getFuel()).build();
        }

        ExecutionLog executionLog = processor.process();
        if (executionLog.isError()) {
            log.debug("smart contract validation = INVALID");
            return builder.error(ApiErrors.CONTRACT_VALIDATION_ERROR, executionLog.toJsonString()).build();
        }
        log.debug("smart contract validation = VALID");
        return builder.ok();
    }

    private Transaction validateAndCreatePublishContractTransaction(PublishContractReq body, ResponseBuilderV2 response) {
        long senderAccountId = Convert.parseAccountId(body.getSender());
        Account account = accountService.getAccount(senderAccountId);
        if (account == null) {
            response.error(ApiErrors.INCORRECT_VALUE, "sender", body.getSender());
            return null;
        }
        if (Strings.isNullOrEmpty(body.getName())) {
            response.error(ApiErrors.INCORRECT_VALUE, "contract_name", body.getName());
            return null;
        }
        if (Strings.isNullOrEmpty(body.getSource())) {
            response.error(ApiErrors.INCORRECT_VALUE, "contract_source", body.getSource());
            return null;
        }
        if (Strings.isNullOrEmpty(body.getFuelPrice())) {
            response.error(ApiErrors.INCORRECT_VALUE, "fuel_price", body.getFuelPrice());
            return null;
        }
        if (Strings.isNullOrEmpty(body.getFuelLimit())) {
            response.error(ApiErrors.INCORRECT_VALUE, "fuel_limit", body.getFuelLimit());
            return null;
        }

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

        log.debug("Transaction id={} sender={} fee={}"
            , Convert.toHexString(transaction.getId())
            , Convert.toHexString(senderAccountId)
            , transaction.getFeeATM());

        return transaction;
    }

    @Override
    public Response createCallContractMethodTx(CallContractMethodReq body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        Transaction transaction = createCallContractMethodTransaction(body, builder);
        TransactionArrayResp response = new TransactionArrayResp();
        Result signedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(transaction.getVersion()).serialize(transaction, signedTxBytes);
        response.setTx(Convert.toHexString(signedTxBytes.array()));

        return builder.bind(response).build();
    }

    @Override
    public Response validateCallContractMethodTx(CallContractMethodReq body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();

        Transaction transaction = createCallContractMethodTransaction(body, builder);
        SmcCallMethodAttachment attachment = (SmcCallMethodAttachment) transaction.getAttachment();
        SmartContract smartContract;
        AplAddress contractAddress = new AplAddress(transaction.getRecipientId());
        try {
            smartContract = contractService.loadContract(
                contractAddress,
                new ContractFuel(attachment.getFuelLimit(), attachment.getFuelPrice())
            );
            smartContract.setSender(new AplAddress(transaction.getSenderId()));
        } catch (ContractNotFoundException e) {
            return ResponseBuilderV2.apiError(ApiErrors.CONTRACT_NOT_FOUND, contractAddress.getHex(), body.getSender()).build();
        }

        SmartMethod smartMethod = SmartMethod.builder()
            .name(attachment.getMethodName())
            .args(attachment.getMethodParams())
            .value(BigInteger.valueOf(transaction.getAmountATM()))
            .build();
        //syntactical and semantic validation
        SmcContractTxProcessor processor = new SandboxCallMethodValidationProcessorContract(
            smartContract,
            smartMethod,
            integratorFactory.createMockProcessor(transaction.getId()),
            smcConfig
        );

        BigInteger calculatedFuel = processor.getExecutionEnv().getPrice().forMethodCalling(smartMethod.getValue()).calc(smartMethod);
        if (!smartContract.getFuel().tryToCharge(calculatedFuel)) {
            log.error("Needed fuel={} but actual={}", calculatedFuel, smartContract.getFuel());
            return builder.error(ApiErrors.CONTRACT_VALIDATION_ERROR, "Not enough fuel to execute this transaction, expected=" + calculatedFuel + " but actual=" + smartContract.getFuel()).build();
        }

        ExecutionLog executionLog = processor.process();
        if (executionLog.isError()) {
            log.debug("method smart contract validation = INVALID");
            return builder.error(ApiErrors.CONTRACT_METHOD_VALIDATION_ERROR, executionLog.toJsonString()).build();
        }
        log.debug("method smart contract validation = VALID");
        return builder.ok();
    }

    private Transaction createCallContractMethodTransaction(CallContractMethodReq body, ResponseBuilderV2 response) throws NotFoundException {
        //validate params
        long addressId = Convert.parseAccountId(body.getAddress());
        Account contractAccount = accountService.getAccount(addressId);
        if (contractAccount == null) {
            response.error(ApiErrors.INCORRECT_VALUE, "contract_address", body.getAddress());
            return null;
        }
        if (contractAccount.getPublicKey() == null) {
            contractAccount.setPublicKey(accountService.getPublicKey(contractAccount.getId()));
        }
        long senderAccountId = Convert.parseAccountId(body.getSender());
        Account senderAccount = accountService.getAccount(senderAccountId);
        if (senderAccount == null) {
            response.error(ApiErrors.INCORRECT_VALUE, "sender_account", body.getSender());
            return null;
        }
        if (Strings.isNullOrEmpty(body.getName())) {
            response.error(ApiErrors.INCORRECT_VALUE, "method_name", body.getName());
            return null;
        }
        if (Strings.isNullOrEmpty(body.getFuelPrice())) {
            response.error(ApiErrors.INCORRECT_VALUE, "fuel_price", body.getFuelPrice());
            return null;
        }
        if (Strings.isNullOrEmpty(body.getFuelLimit())) {
            response.error(ApiErrors.INCORRECT_VALUE, "fuel_limit", body.getFuelLimit());
            return null;
        }

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
            .senderAccount(senderAccount)
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

        log.debug("Transaction id={} contract={} fee={}"
            , Convert.toHexString(transaction.getId())
            , Convert.toHexString(contractAccount.getId())
            , transaction.getFeeATM());

        return transaction;
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
