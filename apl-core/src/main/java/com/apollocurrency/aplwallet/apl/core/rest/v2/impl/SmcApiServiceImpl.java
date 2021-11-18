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
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractService;
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
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.math.BigInteger;
import java.util.ArrayList;
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
public class SmcApiServiceImpl implements SmcApiService {

    public static final String DEFAULT_LANGUAGE_NAME = "js";
    private final AccountService accountService;
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


    @Inject
    public SmcApiServiceImpl(BlockchainConfig blockchainConfig,
                             AccountService accountService,
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
                             ElGamalEncryptor elGamal) {
        this.accountService = accountService;
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
        if (StringUtils.isBlank(body.getSecretPhrase())) {
            response.error(ApiErrors.MISSING_PARAM, "secretPhrase");
            return null;
        }

        if (!contractToolService.validateContractSource(body.getSource())) {
            response.error(ApiErrors.CONSTRAINT_VIOLATION, "The contract source code doesn't match the contract template code.");
        }

        var contractSource = contractToolService.createSmartSource(body.getName(), body.getSource(), DEFAULT_LANGUAGE_NAME);

        SmartSource smartSource = contractToolService.completeContractSource(contractSource);

        var secretPhrase = elGamal.elGamalDecrypt(body.getSecretPhrase());

        BigInteger fuelLimit = new BigInteger(body.getFuelLimit());
        BigInteger fuelPrice = new BigInteger(body.getFuelPrice());
        String valueStr = body.getValue() != null ? body.getValue() : "0";

        SmcPublishContractAttachment attachment = SmcPublishContractAttachment.builder()
            .contractName(smartSource.getName())
            .contractSource(smartSource.getSourceCode())
            .constructorParams(String.join(",", body.getParams()))
            .languageName(smartSource.getLanguageName())
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
            .secretPhrase(secretPhrase)
            .deadlineValue(String.valueOf(1440))
            .attachment(attachment)
            .credential(new MultiSigCredential(1, Crypto.getKeySeed(secretPhrase)))
            .broadcast(false)
            .validate(false)
            .build();

        Transaction transaction = transactionCreator.createTransactionThrowingException(txRequest);

        log.debug("Transaction id={} sender={} fee={}"
            , Convert.toHexString(transaction.getId())
            , Convert.toHexString(txRequest.getSenderAccount().getId())
            , transaction.getFeeATM());

        return transaction;
    }

    @Override
    public Response callViewMethod(CallViewMethodReq body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        //validate params
        if (body.getMembers().isEmpty()) {
            return builder.error(ApiErrors.INCORRECT_VALUE, "members").build();
        }

        long addressId = Convert.parseAccountId(body.getAddress());
        Account contractAccount = accountService.getAccount(addressId);
        if (contractAccount == null) {
            return builder.error(ApiErrors.INCORRECT_VALUE, "address", body.getAddress()).build();
        }
        if (contractAccount.getPublicKey() == null) {
            contractAccount.setPublicKey(accountService.getPublicKey(contractAccount.getId()));
        }
        AplAddress contractAddress = new AplAddress(contractAccount.getId());

        var executionLog = new ExecutionLog();

        var result = processAllMethods(contractAddress, body.getMembers(), executionLog);

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

        AplAddress address = new AplAddress(Convert.parseAccountId(addressStr));
        Account account = accountService.getAccount(address.getLongId());
        if (account == null) {
            return builder.error(ApiErrors.INCORRECT_VALUE, "address", addressStr).build();
        }
        if (!contractService.isContractExist(address)) {
            return builder.error(ApiErrors.CONTRACT_NOT_FOUND, addressStr).build();
        }
        var response = new ContractSpecResponse();
        var aplContractSpec = contractService.loadAsrModuleSpec(address);
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
        var result = processAllMethods(address, methodsToCall, executionLog);
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

        response.setInheritedContracts(contractService.getInheritedAsrModules(contractSpec.getType()
            , aplContractSpec.getLanguage()
            , aplContractSpec.getVersion()));

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

    private List<ResultValue> processAllMethods(Address contractAddress, List<ContractMethod> members, ExecutionLog executionLog) {
        SmartContract smartContract = contractService.loadContract(
            contractAddress,
            contractAddress,
            new ContractFuel(contractAddress, BigInteger.ZERO, BigInteger.ONE)
        );
        var methods = methodMapper.convert(members);
        var context = smcConfig.asContext(accountService.getBlockchainHeight(),
            smartContract,
            integratorFactory.createReadonlyProcessor()
        );

        SmcContractTxBatchProcessor processor = new CallViewMethodTxProcessor(smartContract, methods, context);

        return processor.batchProcess(executionLog);
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
        final AplAddress transactionSender = new AplAddress(transaction.getSenderId());
        try {
            smartContract = contractService.loadContract(
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
        if (StringUtils.isBlank(body.getSecretPhrase())) {
            response.error(ApiErrors.MISSING_PARAM, "secretPhrase");
            return null;
        }
        var secretPhrase = elGamal.elGamalDecrypt(body.getSecretPhrase());

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
            .secretPhrase(secretPhrase)
            .deadlineValue(String.valueOf(1440))
            .attachment(attachment)
            .credential(new MultiSigCredential(1, Crypto.getKeySeed(secretPhrase)))
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
    public Response getSmcEvents(String address, ContractEventsRequest body, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();
        final AplAddress contract = AplAddress.valueOf(address);
        if (!contractService.isContractExist(contract)) {
            return ResponseBuilderV2.apiError(ApiErrors.CONTRACT_NOT_FOUND, address).build();
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
        int fromBlock = 0;
        int toBlock = -1;
        if (body.getFromBlock() != null) {
            fromBlock = body.getFromBlock();
        }
        if (body.getToBlock() != null) {
            toBlock = body.getToBlock();
        }
        int from = 0;
        int to = -1;
        String order;
        if (body.getFrom() != null) {
            from = body.getFrom();
        }
        if (body.getTo() != null) {
            to = body.getTo();
        }
        if (!Strings.isNullOrEmpty(body.getOrder())) {
            order = body.getOrder();
            if (!"ASC".equals(order) && !"DESC".equals(order)) {
                order = "ASC";
            }
        } else
            order = "ASC";

        ContractEventsResponse response = new ContractEventsResponse();
        var rc = eventService.getEventsByFilter(contract.getLongId(), eventName,
            filter,
            fromBlock, toBlock,
            from, to, order
        );

        response.setEvents(rc);

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
        indexBeanParam.adjustIndexes(maxAPIRecords);
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
            return builder.error(ApiErrors.INCORRECT_VALUE, "address", addressStr).build();
        }
        if (!contractService.isContractExist(address)) {
            return builder.error(ApiErrors.CONTRACT_NOT_FOUND, addressStr).build();
        }
        ContractStateResponse response = new ContractStateResponse();

        String contractState = contractService.loadSerializedContract(address);
        response.setState(contractState);

        return builder.bind(response).build();
    }

    @Override
    public Response parseAddress(String address, SecurityContext securityContext) throws NotFoundException {
        ResponseBuilderV2 builder = ResponseBuilderV2.startTiming();

        long addr = Convert.parseAccountId(address);
        var bi = new BigInteger(Long.toUnsignedString(addr));

        AddressSpecResponse response = new AddressSpecResponse();
        response.setRs(Convert2.rsAccount(addr));
        response.setHex(toHex(bi));
        response.setLong(Long.toString(addr));
        response.setUlong(Long.toUnsignedString(addr));

        return builder.bind(response).build();
    }
}
