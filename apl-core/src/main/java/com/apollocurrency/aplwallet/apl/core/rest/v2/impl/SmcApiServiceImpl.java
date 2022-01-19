/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.NotFoundException;
import com.apollocurrency.aplwallet.api.v2.SmcApiService;
import com.apollocurrency.aplwallet.api.v2.model.AddressSpecResponse;
import com.apollocurrency.aplwallet.api.v2.model.AsrSpecResponse;
import com.apollocurrency.aplwallet.api.v2.model.CallContractMethodReq;
import com.apollocurrency.aplwallet.api.v2.model.CallViewMethodReq;
import com.apollocurrency.aplwallet.api.v2.model.ContractDetails;
import com.apollocurrency.aplwallet.api.v2.model.ContractEventsRequest;
import com.apollocurrency.aplwallet.api.v2.model.ContractEventsResponse;
import com.apollocurrency.aplwallet.api.v2.model.ContractListResponse;
import com.apollocurrency.aplwallet.api.v2.model.ContractSpecResponse;
import com.apollocurrency.aplwallet.api.v2.model.ContractStateResponse;
import com.apollocurrency.aplwallet.api.v2.model.ModuleListResponse;
import com.apollocurrency.aplwallet.api.v2.model.ModuleSourceResponse;
import com.apollocurrency.aplwallet.api.v2.model.PublishContractReq;
import com.apollocurrency.aplwallet.api.v2.model.ResultValueResponse;
import com.apollocurrency.aplwallet.api.v2.model.TransactionByteArrayResp;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.rest.v2.ResponseBuilderV2;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.CallMethodResultMapper;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.MethodSpecMapper;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractQuery;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractToolService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractRepository;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcFuelValidator;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.SmcBlockchainIntegratorFactory;
import com.apollocurrency.aplwallet.apl.core.signature.MultiSigCredential;
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxBContext;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcCallMethodAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcPublishContractAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.smc.model.AplAddress;
import com.apollocurrency.aplwallet.apl.smc.service.SmcContractEventService;
import com.apollocurrency.aplwallet.apl.smc.service.SmcContractTxProcessor;
import com.apollocurrency.aplwallet.apl.smc.service.tx.CallMethodTxValidator;
import com.apollocurrency.aplwallet.apl.smc.service.tx.PublishContractTxValidator;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.api.NumericRange;
import com.apollocurrency.aplwallet.apl.util.api.Sort;
import com.apollocurrency.aplwallet.apl.util.api.parameter.FirstLastIndexBeanParam;
import com.apollocurrency.aplwallet.apl.util.cdi.config.Property;
import com.apollocurrency.aplwallet.apl.util.exception.ApiErrors;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import com.apollocurrency.aplwallet.apl.util.service.ElGamalEncryptor;
import com.apollocurrency.smc.contract.AddressNotFoundException;
import com.apollocurrency.smc.contract.ContractStatus;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.SmartMethod;
import com.apollocurrency.smc.contract.fuel.ContractFuel;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.data.expr.Term;
import com.apollocurrency.smc.data.expr.TrueTerm;
import com.apollocurrency.smc.data.expr.parser.TermParser;
import com.apollocurrency.smc.polyglot.SimpleVersion;
import com.apollocurrency.smc.polyglot.Version;
import com.apollocurrency.smc.polyglot.language.ContractSpec;
import com.apollocurrency.smc.polyglot.language.SmartSource;
import com.apollocurrency.smc.polyglot.language.lib.JSLibraryProvider;
import com.apollocurrency.smc.util.HexUtils;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.apollocurrency.smc.util.HexUtils.toHex;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@RequestScoped
class SmcApiServiceImpl implements SmcApiService {

    public static final String DEFAULT_LANGUAGE_NAME = "js";
    private final AccountService accountService;
    private final SmcContractRepository contractRepository;
    private final SmcContractService contractService;
    private final ContractToolService contractToolService;
    private final SmcContractEventService eventService;
    private final TransactionCreator transactionCreator;
    private final TxBContext txBContext;
    private final SmcBlockchainIntegratorFactory integratorFactory;
    private final SmcConfig smcConfig;
    private final int maxAPIRecords;
    private final CallMethodResultMapper methodResultMapper;
    private final MethodSpecMapper methodSpecMapper;
    private final ElGamalEncryptor elGamal;
    private final SmcFuelValidator fuelValidator;


    @Inject
    public SmcApiServiceImpl(BlockchainConfig blockchainConfig,
                             AccountService accountService,
                             SmcContractRepository contractRepository,
                             SmcContractService contractService,
                             ContractToolService contractToolService,
                             SmcContractEventService eventService,
                             TransactionCreator transactionCreator,
                             SmcBlockchainIntegratorFactory integratorFactory,
                             SmcConfig smcConfig,
                             CallMethodResultMapper methodResultMapper,
                             MethodSpecMapper methodSpecMapper,
                             @Property(name = "apl.maxAPIRecords", defaultValue = "100") int maxAPIRecords,
                             ElGamalEncryptor elGamal,
                             SmcFuelValidator fuelValidator) {
        this.accountService = accountService;
        this.contractRepository = contractRepository;
        this.contractService = contractService;
        this.contractToolService = contractToolService;
        this.eventService = eventService;
        this.transactionCreator = transactionCreator;
        this.txBContext = TxBContext.newInstance(blockchainConfig.getChain());
        this.integratorFactory = integratorFactory;
        this.smcConfig = smcConfig;
        this.methodResultMapper = methodResultMapper;
        this.methodSpecMapper = methodSpecMapper;
        this.maxAPIRecords = maxAPIRecords;
        this.elGamal = elGamal;
        this.fuelValidator = fuelValidator;
    }

    @Override
    public Response createPublishContractTxMultisig(PublishContractReq body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        Transaction transaction = validateAndCreatePublishContractTransaction(body, builder, true);
        if (transaction == null) {
            return builder.build();
        }
        TransactionByteArrayResp response = new TransactionByteArrayResp();

        Result signedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(transaction.getVersion()).serialize(transaction, signedTxBytes);
        response.setTx(Convert.toHexString(signedTxBytes.array()));

        return builder.bind(response).build();
    }

    @Override
    public Response createPublishContractTx(PublishContractReq body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        Transaction transaction = validateAndCreatePublishContractTransaction(body, builder);
        if (transaction == null) {
            return builder.build();
        }
        TransactionByteArrayResp response = new TransactionByteArrayResp();

        Result signedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(transaction.getVersion()).serialize(transaction, signedTxBytes);
        response.setTx(Convert.toHexString(signedTxBytes.array()));

        return builder.bind(response).build();
    }

    @Override
    public Response getAsrModuleFullSpec(String module, String languageStr, String versionStr, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        String language = defaultLanguage(languageStr);
        Version version = defaultVersion(versionStr);
        var aplContractSpec = contractService.loadAsrModuleSpec(module, language, version);
        if (aplContractSpec == null) {
            return builder.error(ApiErrors.CONTRACT_SPEC_NOT_FOUND, module).build();
        }
        var response = new AsrSpecResponse();
        response.setMembers(methodSpecMapper.convert(aplContractSpec.getContractSpec().getMembers()));
        response.setName(module);
        response.setType(module);
        response.setLanguage(aplContractSpec.getLanguage());
        response.setVersion(aplContractSpec.getVersion().toString());

        return builder.bind(response).build();
    }

    @Override
    public Response getAsrModuleInitSpec(String module, String languageStr, String versionStr, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        String language = defaultLanguage(languageStr);
        Version version = defaultVersion(versionStr);
        var contractSpec = contractService.loadAsrModuleSpec(module, language, version);
        if (contractSpec == null) {
            return builder.error(ApiErrors.CONTRACT_SPEC_NOT_FOUND, module).build();
        }
        var response = new ContractSpecResponse();
        var constructors = contractSpec.getContractSpec().getMembers().stream()
            .filter(member -> member.getType() == ContractSpec.MemberType.CONSTRUCTOR)
            .collect(Collectors.toList());
        //Collections.reverse(constructors);
        response.getMembers().addAll(methodSpecMapper.convert(constructors));

        return builder.bind(response).build();
    }

    @Override
    public Response getAsrModuleSource(String module, String languageStr, String versionStr, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        String language = defaultLanguage(languageStr);
        Version version = defaultVersion(versionStr);

        var contractSpec = contractService.loadAsrModuleSpec(module, language, version);
        if (contractSpec == null) {
            return builder.error(ApiErrors.CONTRACT_SPEC_NOT_FOUND, module).build();
        }
        var response = new ModuleSourceResponse();

        response.setLanguage(contractSpec.getLanguage());
        response.setVersion(contractSpec.getVersion().toString());
        response.setName(contractSpec.getContractSpec().getName());
        response.setContent(contractSpec.getContent());

        return builder.bind(response).build();
    }

    @Override
    public Response getSmcAsrModules(String languageStr, String versionStr, String typeStr, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        String language = defaultLanguage(languageStr);
        Version version = defaultVersion(versionStr);
        String type = defaultType(typeStr);
        var response = new ModuleListResponse();
        response.setModules(contractService.getAsrModules(language, version, type));
        response.setLanguage(language);
        response.setVersion(version.toString());
        return builder.bind(response).build();
    }

    private String defaultType(String typeStr) {
        if (typeStr == null || typeStr.isBlank()) {
            typeStr = JSLibraryProvider.ASR_TYPE_TOKEN;
        }
        return typeStr;
    }

    private Version defaultVersion(String versionStr) {
        if (versionStr == null || versionStr.isBlank()) {
            versionStr = "0.1.1";
        }
        return SimpleVersion.fromString(versionStr);
    }

    private String defaultLanguage(String languageStr) {
        if (languageStr == null || languageStr.isBlank()) {
            languageStr = DEFAULT_LANGUAGE_NAME;
        }
        return languageStr;
    }

    @Override
    public Response validatePublishContractTx(PublishContractReq body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        Transaction transaction = validateAndCreatePublishContractTransaction(body, builder);
        if (transaction == null) {
            return builder.build();
        }
        SmartContract smartContract = contractToolService.createMockContract(transaction);
        var context = smcConfig.asContext(accountService.getBlockchainHeight(),
            smartContract,
            integratorFactory.createMockProcessor(new AplAddress(smartContract.getTxId()).getLongId())
        );
        //syntactical and semantic validation
        SmcContractTxProcessor processor = new PublishContractTxValidator(smartContract, context);

        BigInteger calculatedFuel = context.getPrice().contractPublishing().calc(smartContract);
        if (!smartContract.getFuel().tryToCharge(calculatedFuel)) {
            log.error("Needed fuel={} but actual={}", calculatedFuel, smartContract.getFuel());
            return builder.error(ApiErrors.CONTRACT_VALIDATION_ERROR, "Not enough fuel to execute this transaction, expected=" + calculatedFuel + " but actual=" + smartContract.getFuel()).build();
        }
        processor.process();
        ExecutionLog executionLog = processor.getExecutionLog();
        if (executionLog.hasError()) {
            log.debug("smart contract validation = INVALID");
            return builder.error(ApiErrors.CONTRACT_VALIDATION_ERROR, executionLog.toJsonString()).build();
        }
        log.debug("smart contract validation = VALID");
        return builder.ok();
    }

    private Transaction validateAndCreatePublishContractTransaction(PublishContractReq body, ResponseBuilderV2 response) {
        return validateAndCreatePublishContractTransaction(body, response, false);
    }

    private Transaction validateAndCreatePublishContractTransaction(PublishContractReq body, ResponseBuilderV2 response, boolean isMultiSig) {
        String masterSecret = null;
        Account senderAccount;
        long senderAccountId;

        String secretPhrase = null;
        if (StringUtils.isNotBlank(body.getSecretPhrase())) {
            secretPhrase = elGamal.elGamalDecrypt(body.getSecretPhrase());
        }

        if (isMultiSig) {
            masterSecret = elGamal.elGamalDecrypt(body.getSender());
            var senderId = AccountService.getId(Crypto.getPublicKey(Crypto.getKeySeed(secretPhrase)));
            senderAccount = accountService.getAccount(senderId);
        } else {
            senderAccount = getAccountByAddress(body.getSender());
        }
        if (senderAccount == null) {
            response.error(ApiErrors.INCORRECT_VALUE, "sender", body.getSender());
            return null;
        }
        senderAccountId = senderAccount.getId();

        byte[] publicKey;
        if (senderAccount.getPublicKey() == null) {
            publicKey = accountService.getPublicKeyByteArray(senderAccount.getId());
        } else {
            publicKey = senderAccount.getPublicKey().getPublicKey();
        }

        if (StringUtils.isBlank(body.getName())) {
            response.error(ApiErrors.INCORRECT_VALUE, "contract_name", body.getName());
            return null;
        }
        if (StringUtils.isBlank(body.getSource())) {
            response.error(ApiErrors.INCORRECT_VALUE, "contract_source", body.getSource());
            return null;
        }
        if (StringUtils.isBlank(body.getFuelPrice())) {
            response.error(ApiErrors.INCORRECT_VALUE, "fuel_price", body.getFuelPrice());
            return null;
        }
        if (StringUtils.isBlank(body.getFuelLimit())) {
            response.error(ApiErrors.INCORRECT_VALUE, "fuel_limit", body.getFuelLimit());
            return null;
        }

        if (!contractToolService.validateContractSource(body.getSource())) {//validate with pattern
            response.error(ApiErrors.CONSTRAINT_VIOLATION, "The contract source code doesn't match the contract template code.");
            return null;
        }
        var contractSource = contractToolService.createSmartSource(body.getName(), body.getSource(), DEFAULT_LANGUAGE_NAME);
        SmartSource smartSource = contractToolService.completeContractSource(contractSource);

        if (StringUtils.isNotBlank(body.getPublicKey())) {
            try {
                publicKey = Convert.parseHexString(body.getPublicKey());
            } catch (NumberFormatException e) {
                log.error(e.getMessage());
                response.error(ApiErrors.BAD_CREDENTIALS, "Wrong public key");
                return null;
            }
        }

        if (isMultiSig && secretPhrase == null && publicKey == null) {
            response.error(ApiErrors.MISSING_PARAM, "secretPhrase");
            return null;
        }

        if (isMultiSig && isSenderWrong(senderAccountId, secretPhrase, publicKey)) {
            response.error(ApiErrors.BAD_CREDENTIALS, "Sender doesn't match the secret phrase");
            return null;
        }

        BigInteger fuelLimit = new BigInteger(body.getFuelLimit());
        BigInteger fuelPrice = new BigInteger(body.getFuelPrice());
        String valueStr = body.getValue() != null ? body.getValue() : "0";

        SmcPublishContractAttachment attachment = SmcPublishContractAttachment.builder()
            .contractName(smartSource.getName())
            .baseContract(smartSource.getBaseContract())
            .contractSource(smartSource.getSourceCode())
            .constructorParams(String.join(",", body.getParams()))
            .languageName(smartSource.getLanguageName())
            .languageVersion(smartSource.getLanguageVersion().toString())
            .fuelLimit(fuelLimit)
            .fuelPrice(fuelPrice)
            .build();

        try {
            fuelValidator.validate(attachment);
        } catch (Exception e) {
            response.error(ApiErrors.CONTRACT_VALIDATION_ERROR, e.getMessage());
            return null;
        }

        var txRequestBuilder = CreateTransactionRequest.builder()
            .version(2)
            .amountATM(Convert.parseLong(valueStr))
            .senderAccount(senderAccount)
            .publicKey(publicKey)//it's the sender public key
            .deadlineValue(String.valueOf(1440))
            .attachment(attachment)
            .broadcast(false)
            .validate(false);

        if (isMultiSig) {
            var msig = new MultiSigCredential(2, Crypto.getKeySeed(masterSecret), Crypto.getKeySeed(secretPhrase));
            txRequestBuilder
                .secretPhrase(secretPhrase)
                .credential(msig);
        }

        CreateTransactionRequest txRequest = txRequestBuilder.build();

        return transactionCreator.createTransactionThrowingException(txRequest);
    }

    @Override
    public Response callViewMethod(CallViewMethodReq body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        //validate params
        if (body.getMembers().isEmpty()) {
            return builder.error(ApiErrors.INCORRECT_VALUE, "members").build();
        }
        Long contractId = getIdByAddress(body.getAddress());
        if (contractId == null || !contractRepository.isContractExist(new AplAddress(contractId))) {
            return builder.error(ApiErrors.CONTRACT_NOT_FOUND, body.getAddress()).build();
        }
        var executionLog = new ExecutionLog();

        var result = contractService.processAllViewMethods(new AplAddress(contractId), body.getMembers(), executionLog);

        if (executionLog.hasError()) {
            return builder.detailedError(ApiErrors.CONTRACT_READ_METHOD_ERROR, executionLog.toJsonString(), executionLog.getLatestCause()).build();
        }
        var response = new ResultValueResponse();
        response.setResults(methodResultMapper.convert(result));

        return builder.bind(response).build();
    }

    @Override
    public Response getSmcSpecificationByAddress(String addressStr, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        Long accountId = getIdByAddress(addressStr);
        if (accountId == null || !contractRepository.isContractExist(new AplAddress(accountId))) {
            return builder.error(ApiErrors.CONTRACT_NOT_FOUND, addressStr).build();
        }
        var address = new AplAddress(accountId);

        var response = contractService.loadContractSpecification(address);

        if (response == null) {
            return builder.error(ApiErrors.CONTRACT_READ_METHOD_ERROR).build();
        }

        return builder.bind(response).build();
    }

    @Override
    public Response createCallContractMethodTx(CallContractMethodReq body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        Transaction transaction = validateAndCreateCallContractMethodTransaction(body, builder);
        if (transaction == null) {
            return builder.build();
        }
        TransactionByteArrayResp response = new TransactionByteArrayResp();
        Result signedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(transaction.getVersion()).serialize(transaction, signedTxBytes);
        response.setTx(Convert.toHexString(signedTxBytes.array()));

        return builder.bind(response).build();
    }

    @Override
    public Response validateCallContractMethodTx(CallContractMethodReq body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();

        Transaction transaction = validateAndCreateCallContractMethodTransaction(body, builder);
        if (transaction == null) {
            return builder.build();
        }

        SmcCallMethodAttachment attachment = (SmcCallMethodAttachment) transaction.getAttachment();
        SmartContract smartContract;
        AplAddress contractAddress = new AplAddress(transaction.getRecipientId());
        final AplAddress transactionSender = new AplAddress(transaction.getSenderId());
        try {
            smartContract = contractRepository.loadContract(
                contractAddress,
                transactionSender,
                new ContractFuel(contractAddress, attachment.getFuelLimit(), attachment.getFuelPrice())
            );
        } catch (AddressNotFoundException e) {
            return ResponseBuilderV2.apiError(ApiErrors.CONTRACT_NOT_FOUND, contractAddress.getHex(), body.getSender()).build();
        }

        SmartMethod smartMethod = SmartMethod.builder()
            .name(attachment.getMethodName())
            .args(attachment.getMethodParams())
            .value(BigInteger.valueOf(transaction.getAmountATM()))
            .build();

        var context = smcConfig.asContext(accountService.getBlockchainHeight(),
            smartContract,
            integratorFactory.createMockProcessor(transaction.getId())
        );

        //syntactical and semantic validation
        SmcContractTxProcessor processor = new CallMethodTxValidator(
            smartContract,
            smartMethod,
            context
        );

        BigInteger calculatedFuel = context.getPrice().methodCalling(smartMethod.getValue()).calc(smartMethod);
        if (!smartContract.getFuel().tryToCharge(calculatedFuel)) {
            log.error("Needed fuel={} but actual={}", calculatedFuel, smartContract.getFuel());
            return builder.error(ApiErrors.CONTRACT_VALIDATION_ERROR, "Not enough fuel to execute this transaction, expected=" + calculatedFuel + " but actual=" + smartContract.getFuel()).build();
        }
        processor.process();
        ExecutionLog executionLog = processor.getExecutionLog();
        if (executionLog.hasError()) {
            log.debug("method smart contract validation = INVALID");
            return builder.error(ApiErrors.CONTRACT_METHOD_VALIDATION_ERROR, executionLog.toJsonString()).build();
        }
        log.debug("method smart contract validation = VALID");
        return builder.ok();
    }

    private Transaction validateAndCreateCallContractMethodTransaction(CallContractMethodReq body, ResponseBuilderV2 response) throws NotFoundException {
        //validate params
        Long contractAccountId = getIdByAddress(body.getAddress());
        if (contractAccountId == null) {
            response.error(ApiErrors.CONTRACT_NOT_FOUND, body.getAddress());
            return null;
        }
        Account senderAccount = getAccountByAddress(body.getSender());
        if (senderAccount == null) {
            response.error(ApiErrors.INCORRECT_VALUE, "sender", body.getSender());
            return null;
        }
        long senderAccountId = senderAccount.getId();
        byte[] publicKey;
        if (senderAccount.getPublicKey() == null) {
            publicKey = accountService.getPublicKeyByteArray(senderAccount.getId());
        } else {
            publicKey = senderAccount.getPublicKey().getPublicKey();
        }

        if (StringUtils.isBlank(body.getName())) {
            response.error(ApiErrors.INCORRECT_VALUE, "method_name", body.getName());
            return null;
        }
        if (StringUtils.isBlank(body.getFuelPrice())) {
            response.error(ApiErrors.INCORRECT_VALUE, "fuel_price", body.getFuelPrice());
            return null;
        }
        if (StringUtils.isBlank(body.getFuelLimit())) {
            response.error(ApiErrors.INCORRECT_VALUE, "fuel_limit", body.getFuelLimit());
            return null;
        }
        String secretPhrase = null;
        if (StringUtils.isNotBlank(body.getSecretPhrase())) {
            secretPhrase = elGamal.elGamalDecrypt(body.getSecretPhrase());
        }
        if (StringUtils.isNotBlank(body.getPublicKey())) {
            try {
                publicKey = Convert.parseHexString(body.getPublicKey());
            } catch (NumberFormatException e) {
                log.error(e.getMessage());
                response.error(ApiErrors.BAD_CREDENTIALS, "Wrong public key");
                return null;
            }
        }
        if (secretPhrase == null && publicKey == null) {
            response.error(ApiErrors.MISSING_PARAM, "secretPhrase");
            return null;
        }
        if (isSenderWrong(senderAccountId, secretPhrase, publicKey)) {
            response.error(ApiErrors.BAD_CREDENTIALS, "Sender doesn't match the secret phrase");
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

        try {
            fuelValidator.validate(attachment);
        } catch (Exception e) {
            response.error(ApiErrors.CONTRACT_VALIDATION_ERROR, e.getMessage());
            return null;
        }

        CreateTransactionRequest txRequest = CreateTransactionRequest.builder()
            .version(2)
            .amountATM(Convert.parseLong(valueStr))
            .senderAccount(senderAccount)
            .publicKey(publicKey)//it's the sender public key
            .recipientId(contractAccountId)
            .secretPhrase(secretPhrase)
            .deadlineValue(String.valueOf(1440))
            .attachment(attachment)
            .credential((secretPhrase != null) ? new MultiSigCredential(1, Crypto.getKeySeed(secretPhrase)) : null)
            .broadcast(false)
            .validate(false)
            .build();

        Transaction transaction = transactionCreator.createTransactionThrowingException(txRequest);

        log.debug("Transaction id={} contract={} fee={}"
            , Convert.toHexString(transaction.getId())
            , Convert.toHexString(contractAccountId)
            , transaction.getFeeATM());

        return transaction;
    }

    @Override
    public Response getSmcByOwnerAccount(String accountStr, Integer firstIndex, Integer lastIndex, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        Long accountId = getIdByAddress(accountStr);
        if (accountId == null) {
            return builder.error(ApiErrors.CONTRACTS_NOT_FOUND).build();
        }

        FirstLastIndexBeanParam indexBeanParam = new FirstLastIndexBeanParam(firstIndex, lastIndex);
        indexBeanParam.adjustIndexes(maxAPIRecords);

        ContractListResponse response = new ContractListResponse();

        var query = ContractQuery.builder()
            .owner(accountId)
            .height(-1)
            .paging(indexBeanParam.range())
            .build();
        List<ContractDetails> contracts = contractRepository.loadContractsByFilter(query);

        response.setContracts(contracts);
        response.setContracts(contracts);

        return builder.bind(response).build();
    }

    @Override
    public Response getSmcEvents(String address, ContractEventsRequest body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        Long contractId = getIdByAddress(address);
        if (contractId == null || !contractRepository.isContractExist(new AplAddress(contractId))) {
            return builder.error(ApiErrors.CONTRACT_NOT_FOUND, address).build();
        }
        String eventName;
        if (body.getEvent() == null || "allEvents".equals(body.getEvent())) {
            eventName = null;
        } else {
            eventName = body.getEvent();
        }
        var filterStr = body.getFilter();
        Term filter;
        if (StringUtils.isBlank(filterStr)) {
            filter = new TrueTerm();
        } else {
            try {
                filter = TermParser.parse(filterStr);
            } catch (Exception e) {
                return ResponseBuilderV2.apiError(ApiErrors.CONTRACT_EVENT_FILTER_ERROR, e.getMessage()).build();
            }
        }
        var blockRange = new NumericRange(body.getFromBlock(), body.getToBlock());
        var paging = new NumericRange(body.getFrom(), body.getTo());
        var order = Sort.of(body.getOrder());
        ContractEventsResponse response = new ContractEventsResponse();
        var rc = eventService.getEventsByFilter(contractId, eventName, filter, blockRange, paging, order);

        response.setEvents(rc);

        return builder.bind(response).build();
    }

    @Override
    public Response getSmcByAddress(String addressStr, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        Long contractId = getIdByAddress(addressStr);
        if (contractId == null || !contractRepository.isContractExist(new AplAddress(contractId))) {
            return builder.error(ApiErrors.CONTRACT_NOT_FOUND, addressStr).build();
        }

        ContractListResponse response = new ContractListResponse();

        var contracts = contractRepository.getContractDetailsByAddress(contractId);

        response.setContracts(contracts);

        return builder.bind(response).build();
    }

    @Override
    public Response getSmcList(String addressStr, String publisherStr, String name, String baseContract, Long timestamp, String transactionAddr, String status, Integer firstIndex, Integer lastIndex, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();

        FirstLastIndexBeanParam indexBeanParam = new FirstLastIndexBeanParam(firstIndex, lastIndex);
        indexBeanParam.adjustIndexes(maxAPIRecords);
        Long address = null;
        Long publisher = null;
        Long transaction = null;

        if (status != null) {
            try {
                ContractStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                return ResponseBuilderV2.apiError(ApiErrors.INCORRECT_VALUE, "status", status).build();
            }
        }

        if (addressStr != null) {
            Account account = getAccountByAddress(addressStr);
            if (account == null) {
                return builder.error(ApiErrors.CONTRACT_NOT_FOUND, addressStr).build();
            }
            if (!contractRepository.isContractExist(new AplAddress(account.getId()))) {
                return ResponseBuilderV2.apiError(ApiErrors.CONTRACT_NOT_FOUND, addressStr).build();
            }
            address = account.getId();
        }

        if (publisherStr != null) {
            publisher = getIdByAddress(publisherStr);
        }

        if (transactionAddr != null) {
            transaction = getIdByAddress(transactionAddr);
        }
        ContractListResponse response = new ContractListResponse();

        var query = ContractQuery.builder()
            .address(address)
            .transaction(transaction)
            .owner(publisher)
            .name(name)
            .baseContract(baseContract)
            .status(status)
            .height(-1)
            .paging(indexBeanParam.range())
            .build();

        List<ContractDetails> contracts = contractRepository.loadContractsByFilter(query);

        response.setContracts(contracts);

        return builder.bind(response).build();
    }

    @Override
    public Response getSmcStateByAddress(String addressStr, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        Long accountId = getIdByAddress(addressStr);
        if (accountId == null || !contractRepository.isContractExist(new AplAddress(accountId))) {
            return builder.error(ApiErrors.CONTRACT_NOT_FOUND, addressStr).build();
        }
        ContractStateResponse response = new ContractStateResponse();
        String contractState = contractRepository.loadSerializedContract(new AplAddress(accountId));
        response.setState(contractState);

        return builder.bind(response).build();
    }

    @Override
    public Response parseAddress(String address, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        Long addressId = getIdByAddress(address);
        if (addressId == null) {
            return builder.error(ApiErrors.INCORRECT_PARAM, "address", address).build();
        }
        BigInteger bi = BigInteger.valueOf(addressId);

        AddressSpecResponse response = new AddressSpecResponse();
        response.setRs(Convert2.rsAccount(addressId));
        response.setHex(toHex(bi.toByteArray()));
        response.setLong(Long.toString(addressId));
        response.setUlong(Long.toUnsignedString(addressId));

        return builder.bind(response).build();
    }

    private boolean isSenderWrong(long senderId, String secretPhrase, byte[] publicKeyParam) {
        byte[] publicKey;
        if (secretPhrase != null) {
            publicKey = Crypto.getPublicKey(secretPhrase);
            if (publicKeyParam != null && !Arrays.equals(publicKey, publicKeyParam)) {
                return true;
            }
        } else {
            publicKey = publicKeyParam;
        }
        if (publicKey == null) {
            return true;
        } else {
            return senderId != AccountService.getId(publicKey);
        }
    }

    private Account getAccountByAddress(String addressStr) {
        Long addressId = getIdByAddress(addressStr);
        if (addressId != null) {
            return accountService.getAccount(addressId);
        }
        return null;
    }

    private Long getIdByAddress(String addressStr) {
        Long addressId = null;
        if (addressStr != null && !addressStr.isBlank()) {
            try {
                if (addressStr.startsWith("0x")) {
                    addressId = new BigInteger(HexUtils.parseHex(addressStr)).longValueExact();
                } else {
                    addressId = Convert.parseAccountId(addressStr);
                }
            } catch (Exception e) {
                //do nothing
            }
        }
        return addressId;
    }

}
