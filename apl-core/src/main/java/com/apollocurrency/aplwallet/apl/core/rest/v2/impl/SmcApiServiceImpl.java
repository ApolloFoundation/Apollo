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
import com.apollocurrency.aplwallet.api.v2.model.ContractMethod;
import com.apollocurrency.aplwallet.api.v2.model.ContractSpecResponse;
import com.apollocurrency.aplwallet.api.v2.model.ContractStateResponse;
import com.apollocurrency.aplwallet.api.v2.model.MemberSpec;
import com.apollocurrency.aplwallet.api.v2.model.ModuleListResponse;
import com.apollocurrency.aplwallet.api.v2.model.ModuleSourceResponse;
import com.apollocurrency.aplwallet.api.v2.model.PropertySpec;
import com.apollocurrency.aplwallet.api.v2.model.PublishContractReq;
import com.apollocurrency.aplwallet.api.v2.model.ResultValueResponse;
import com.apollocurrency.aplwallet.api.v2.model.TransactionArrayResp;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.rest.v2.ResponseBuilderV2;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.CallMethodResultMapper;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.MethodSpecMapper;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.SmartMethodMapper;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
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
import com.apollocurrency.aplwallet.apl.smc.service.SmcContractTxBatchProcessor;
import com.apollocurrency.aplwallet.apl.smc.service.SmcContractTxProcessor;
import com.apollocurrency.aplwallet.apl.smc.service.tx.CallMethodTxValidator;
import com.apollocurrency.aplwallet.apl.smc.service.tx.CallViewMethodTxProcessor;
import com.apollocurrency.aplwallet.apl.smc.service.tx.PublishContractTxValidator;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
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
import com.apollocurrency.smc.contract.vm.ResultValue;
import com.apollocurrency.smc.data.expr.Term;
import com.apollocurrency.smc.data.expr.TrueTerm;
import com.apollocurrency.smc.data.expr.parser.TermParser;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.polyglot.SimpleVersion;
import com.apollocurrency.smc.polyglot.Version;
import com.apollocurrency.smc.polyglot.language.ContractSpec;
import com.apollocurrency.smc.polyglot.language.SmartSource;
import com.apollocurrency.smc.polyglot.language.lib.JSLibraryProvider;
import com.apollocurrency.smc.util.HexUtils;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
    private final SmartMethodMapper methodMapper;
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
                             SmartMethodMapper methodMapper,
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
        this.methodMapper = methodMapper;
        this.methodResultMapper = methodResultMapper;
        this.methodSpecMapper = methodSpecMapper;
        this.maxAPIRecords = maxAPIRecords;
        this.elGamal = elGamal;
        this.fuelValidator = fuelValidator;
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
        SmartContract smartContract = contractToolService.createNewContract(transaction);
        var context = smcConfig.asContext(accountService.getBlockchainHeight(),
            smartContract,
            integratorFactory.createMockProcessor(transaction.getId())
        );
        //syntactical and semantic validation
        SmcContractTxProcessor processor = new PublishContractTxValidator(smartContract, context);

        BigInteger calculatedFuel = context.getPrice().contractPublishing().calc(smartContract);
        if (!smartContract.getFuel().tryToCharge(calculatedFuel)) {
            log.error("Needed fuel={} but actual={}", calculatedFuel, smartContract.getFuel());
            return builder.error(ApiErrors.CONTRACT_VALIDATION_ERROR, "Not enough fuel to execute this transaction, expected=" + calculatedFuel + " but actual=" + smartContract.getFuel()).build();
        }

        ExecutionLog executionLog = new ExecutionLog();
        processor.process(executionLog);
        if (executionLog.hasError()) {
            log.debug("smart contract validation = INVALID");
            return builder.error(ApiErrors.CONTRACT_VALIDATION_ERROR, executionLog.toJsonString()).build();
        }
        log.debug("smart contract validation = VALID");
        return builder.ok();
    }

    private Transaction validateAndCreatePublishContractTransaction(PublishContractReq body, ResponseBuilderV2 response) {
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

        if (!contractToolService.validateContractSource(body.getSource())) {//validate with pattern
            response.error(ApiErrors.CONSTRAINT_VIOLATION, "The contract source code doesn't match the contract template code.");
            return null;
        }
        var contractSource = contractToolService.createSmartSource(body.getName(), body.getSource(), DEFAULT_LANGUAGE_NAME);
        SmartSource smartSource = contractToolService.completeContractSource(contractSource);

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

        byte[] generatedPublicKey = AccountService.generatePublicKey(senderAccount, attachment.getContractSource());
        long recipientId = AccountService.getId(generatedPublicKey);

        CreateTransactionRequest txRequest = CreateTransactionRequest.builder()
            .version(2)
            .amountATM(Convert.parseLong(valueStr))
            .senderAccount(senderAccount)
            .publicKey(publicKey)//it's the sender public key
            .recipientPublicKey(Convert.toHexString(generatedPublicKey))
            .recipientId(recipientId)
            .secretPhrase(secretPhrase)
            .deadlineValue(String.valueOf(1440))
            .attachment(attachment)
            .credential((secretPhrase != null) ? new MultiSigCredential(1, Crypto.getKeySeed(secretPhrase)) : null)
            .broadcast(false)
            .validate(false)
            .build();

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

        var result = processAllViewMethods(new AplAddress(contractId), body.getMembers(), executionLog);

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
        var response = new ContractSpecResponse();
        var aplContractSpec = contractRepository.loadAsrModuleSpec(address);
        var contractSpec = aplContractSpec.getContractSpec();
        var notViewMethods = contractSpec.getMembers().stream()
            .filter(member -> member.getType() == ContractSpec.MemberType.FUNCTION && member.getStateMutability() != ContractSpec.StateMutability.VIEW
                && (member.getVisibility() == ContractSpec.Visibility.PUBLIC
                || member.getVisibility() == ContractSpec.Visibility.EXTERNAL))
            .collect(Collectors.toList());
        var viewMethods = contractSpec.getMembers().stream()
            .filter(member -> member.getType() == ContractSpec.MemberType.FUNCTION && member.getStateMutability() == ContractSpec.StateMutability.VIEW
                && (member.getVisibility() == ContractSpec.Visibility.PUBLIC
                || member.getVisibility() == ContractSpec.Visibility.EXTERNAL))
            .collect(Collectors.toList());

        var events = contractSpec.getMembers().stream()
            .filter(member -> member.getType() == ContractSpec.MemberType.EVENT)
            .collect(Collectors.toList());

        List<ContractMethod> methodsToCall = new ArrayList<>();

        viewMethods.forEach(member -> {
            ContractMethod m = new ContractMethod();
            m.setFunction(member.getName());
            if (member.getInputs() == null || member.getInputs().isEmpty()) {
                methodsToCall.add(m);
            }
        });

        var executionLog = new ExecutionLog();
        var result = processAllViewMethods(address, methodsToCall, executionLog);
        if (executionLog.hasError()) {
            return builder.detailedError(ApiErrors.CONTRACT_READ_METHOD_ERROR, executionLog.toJsonString(), executionLog.getLatestCause()).build();
        }
        result.add(ResultValue.builder()
            .method(JSLibraryProvider.CONTRACT_OVERVIEW_ITEM.getName())
            .output(List.of(address.getHex()))
            .build());

        var resultMap = toMap(result);
        var overviewProperties = new ArrayList<>(
            createOverview(contractSpec, resultMap)
        );
        response.setOverview(overviewProperties);

        response.getMembers().addAll(methodSpecMapper.convert(viewMethods));
        matchResults(response.getMembers(), resultMap);
        response.getMembers().addAll(methodSpecMapper.convert(notViewMethods));
        response.getMembers().addAll(methodSpecMapper.convert(events));

        response.setInheritedContracts(
            contractService.getInheritedAsrModules(contractSpec.getType()
                , aplContractSpec.getLanguage()
                , aplContractSpec.getVersion())
        );

        return builder.bind(response).build();
    }

    private Map<String, ResultValue> toMap(List<ResultValue> result) {
        return result.stream().collect(Collectors.toMap(ResultValue::getMethod, Function.identity()));
    }

    private void matchResults(List<MemberSpec> methods, Map<String, ResultValue> resultMap) {
        methods.forEach(methodSpec -> {
            if (methodSpec.getInputs() == null || methodSpec.getInputs().isEmpty()) {
                var res = resultMap.getOrDefault(methodSpec.getName(), ResultValue.UNDEFINED_RESULT);
                methodSpec.setValue(res.getStringResult());
                methodSpec.setSignature(res.getSignature());
            }
        });
    }

    private List<PropertySpec> createOverview(ContractSpec contractSpec, Map<String, ResultValue> resultMap) {
        //TODO: move Overview info to ContractSpec
        var properties = contractSpec.getOverview();

        var propertySpec = new ArrayList<PropertySpec>();

        properties.forEach(item -> {
            var prop = new PropertySpec();
            prop.setName(item.getName());
            prop.setType(item.getType());
            prop.setValue(resultMap.getOrDefault(item.getName(), ResultValue.UNDEFINED_RESULT).getStringResult());
            propertySpec.add(prop);
        });
        return propertySpec;
    }

    private List<ResultValue> processAllViewMethods(Address contractAddress, List<ContractMethod> members, ExecutionLog executionLog) {
        SmartContract smartContract = contractRepository.loadContract(
            contractAddress,
            contractAddress,
            new ContractFuel(contractAddress, BigInteger.ZERO, BigInteger.ONE)
        );
        var methods = methodMapper.convert(members);
        var context = smcConfig.asViewContext(accountService.getBlockchainHeight(),
            smartContract,
            integratorFactory.createReadonlyProcessor()
        );

        SmcContractTxBatchProcessor processor = new CallViewMethodTxProcessor(smartContract, methods, context);

        return processor.batchProcess(executionLog);
    }

    @Override
    public Response createCallContractMethodTx(CallContractMethodReq body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        Transaction transaction = validateAndCreateCallContractMethodTransaction(body, builder);
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

        ExecutionLog executionLog = new ExecutionLog();
        processor.process(executionLog);
        if (executionLog.hasError()) {
            log.debug("method smart contract validation = INVALID");
            return builder.error(ApiErrors.CONTRACT_METHOD_VALIDATION_ERROR, executionLog.toJsonString()).build();
        }
        log.debug("method smart contract validation = VALID");
        return builder.ok();
    }

    private Transaction validateAndCreateCallContractMethodTransaction(CallContractMethodReq body, ResponseBuilderV2 response) throws NotFoundException {
        //validate params
        Account contractAccount = getAccountByAddress(body.getAddress());
        if (contractAccount == null) {
            response.error(ApiErrors.CONTRACT_NOT_FOUND, body.getAddress());
            return null;
        }
        if (contractAccount.getPublicKey() == null) {
            contractAccount.setPublicKey(accountService.getPublicKey(contractAccount.getId()));
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
            .recipientPublicKey(Convert.toHexString(contractAccount.getPublicKey().getPublicKey()))
            .recipientId(contractAccount.getId())
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
            , Convert.toHexString(contractAccount.getId())
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
        var publisher = new AplAddress(accountId);

        FirstLastIndexBeanParam indexBeanParam = new FirstLastIndexBeanParam(firstIndex, lastIndex);
        indexBeanParam.adjustIndexes(maxAPIRecords);

        ContractListResponse response = new ContractListResponse();

        List<ContractDetails> contracts = contractRepository.loadContractsByFilter(
            null,
            null,
            publisher,
            null,
            null,
            null,
            null,
            -1,
            indexBeanParam.getFirstIndex(),
            indexBeanParam.getLastIndex()
        );

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
        if (Strings.isNullOrEmpty(filterStr)) {
            filter = new TrueTerm();
        } else {
            try {
                filter = TermParser.parse(filterStr);
            } catch (Exception e) {
                return ResponseBuilderV2.apiError(ApiErrors.CONTRACT_EVENT_FILTER_ERROR, e.getMessage()).build();
            }
        }
        int[] blockBoundaries = boundaries(body.getFromBlock(), body.getToBlock());
        int[] paging = boundaries(body.getFrom(), body.getTo());
        String order;
        if (!Strings.isNullOrEmpty(body.getOrder())) {
            order = body.getOrder();
            if (!"ASC".equals(order) && !"DESC".equals(order)) {
                order = "ASC";
            }
        } else
            order = "ASC";

        ContractEventsResponse response = new ContractEventsResponse();
        var rc = eventService.getEventsByFilter(contractId, eventName,
            filter,
            blockBoundaries[0], blockBoundaries[1],
            paging[0], paging[1],
            order
        );

        response.setEvents(rc);

        return builder.bind(response).build();
    }

    private int[] boundaries(Integer from, Integer to) {
        int[] rc = new int[]{0, -1};
        if (from != null) {
            rc[0] = from;
        }
        if (to != null) {
            rc[1] = to;
        }
        return rc;
    }

    @Override
    public Response getSmcByAddress(String addressStr, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        Long contractId = getIdByAddress(addressStr);
        if (contractId == null || !contractRepository.isContractExist(new AplAddress(contractId))) {
            return builder.error(ApiErrors.CONTRACT_NOT_FOUND, addressStr).build();
        }
        var address = new AplAddress(contractId);
        ContractListResponse response = new ContractListResponse();

        var contracts = contractRepository.getContractDetailsByAddress(address);

        response.setContracts(contracts);

        return builder.bind(response).build();
    }

    @Override
    public Response getSmcList(String addressStr, String publisherStr, String name, String baseContract, Long timestamp, String transactionAddr, String status, Integer firstIndex, Integer lastIndex, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();

        FirstLastIndexBeanParam indexBeanParam = new FirstLastIndexBeanParam(firstIndex, lastIndex);
        indexBeanParam.adjustIndexes(maxAPIRecords);
        AplAddress address = null;
        AplAddress publisher = null;
        AplAddress transaction = null;

        ContractStatus smcStatus = null;
        if (status != null) {
            try {
                smcStatus = ContractStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                return ResponseBuilderV2.apiError(ApiErrors.INCORRECT_VALUE, "status", addressStr).build();
            }
        }

        if (addressStr != null) {
            Account account = getAccountByAddress(addressStr);
            if (account == null) {
                return builder.error(ApiErrors.CONTRACT_NOT_FOUND, addressStr).build();
            }
            address = new AplAddress(account.getId());
            if (!contractRepository.isContractExist(address)) {
                return ResponseBuilderV2.apiError(ApiErrors.CONTRACT_NOT_FOUND, addressStr).build();
            }
        }

        if (publisherStr != null) {
            var publisherId = getIdByAddress(publisherStr);
            if (publisherId != null) {
                publisher = new AplAddress(publisherId);
            }
        }

        if (transactionAddr != null) {
            var transactionId = getIdByAddress(transactionAddr);
            if (transactionId != null) {
                transaction = new AplAddress(transactionId);
            }
        }
        ContractListResponse response = new ContractListResponse();

        List<ContractDetails> contracts = contractRepository.loadContractsByFilter(
            address,
            transaction,
            publisher,
            name,
            baseContract,
            timestamp,
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
        long addressId;
        BigInteger bi;
        if (address.startsWith("0x")) {
            bi = new BigInteger(HexUtils.parseHex(address));
            addressId = bi.longValue();
        } else {
            try {
                addressId = Convert.parseAccountId(address);
            } catch (Exception e) {
                return builder.error(ApiErrors.INCORRECT_PARAM, "address", e.getMessage()).build();
            }
            bi = new BigInteger(Long.toUnsignedString(addressId));
        }

        AddressSpecResponse response = new AddressSpecResponse();
        response.setRs(Convert2.rsAccount(addressId));
        response.setHex(toHex(bi));
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
            if (addressStr.startsWith("0x")) {
                var bi = new BigInteger(HexUtils.parseHex(addressStr));
                addressId = bi.longValue();
            } else {
                try {
                    addressId = Convert.parseAccountId(addressStr);
                } catch (Exception e) {
                    //do nothing
                }
            }
        }
        return addressId;
    }

}
