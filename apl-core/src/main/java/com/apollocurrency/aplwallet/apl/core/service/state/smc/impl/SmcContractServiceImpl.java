/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.api.v2.model.ContractMethod;
import com.apollocurrency.aplwallet.api.v2.model.ContractSpecResponse;
import com.apollocurrency.aplwallet.api.v2.model.MemberSpec;
import com.apollocurrency.aplwallet.api.v2.model.PropertySpec;
import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.exception.AplCoreContractViolationException;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.MethodSpecMapper;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.SmartMethodMapper;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractRepository;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractService;
import com.apollocurrency.aplwallet.apl.smc.model.AplContractSpec;
import com.apollocurrency.aplwallet.apl.smc.service.SmcContractTxBatchProcessor;
import com.apollocurrency.aplwallet.apl.smc.service.tx.CallViewMethodTxProcessor;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.fuel.ContractFuel;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.contract.vm.ResultValue;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.polyglot.Version;
import com.apollocurrency.smc.polyglot.language.ContractSpec;
import com.apollocurrency.smc.polyglot.language.LanguageContext;
import com.apollocurrency.smc.polyglot.language.lib.JSLibraryProvider;
import com.apollocurrency.smc.polyglot.language.lib.LibraryProvider;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class SmcContractServiceImpl implements SmcContractService {
    protected final SmcConfig smcConfig;
    private final LibraryProvider libraryProvider;
    private final SmcContractRepository contractRepository;
    private final SmcBlockchainIntegratorFactory integratorFactory;
    private final AccountService accountService;
    private final SmartMethodMapper methodMapper;
    private final MethodSpecMapper methodSpecMapper;


    @Inject
    public SmcContractServiceImpl(SmcConfig smcConfig,
                                  AccountService accountService,
                                  SmcContractRepository contractRepository,
                                  SmcBlockchainIntegratorFactory integratorFactory,
                                  SmartMethodMapper methodMapper,
                                  MethodSpecMapper methodSpecMapper) {
        this.smcConfig = smcConfig;
        final LanguageContext languageContext = smcConfig.createLanguageContext();
        this.libraryProvider = languageContext.getLibraryProvider();
        this.contractRepository = contractRepository;
        this.integratorFactory = integratorFactory;
        this.accountService = accountService;
        this.methodMapper = methodMapper;
        this.methodSpecMapper = methodSpecMapper;
    }

    @SneakyThrows
    @Override
    public AplContractSpec loadAsrModuleSpec(String asrModuleName, String language, Version version) {
        checkLibraryCompatibility(language, version);
        var contractSpec = libraryProvider.loadSpecification(asrModuleName);
        log.trace("Loaded specification for module={}, spec={}", asrModuleName, contractSpec);
        var src = libraryProvider.importModule(asrModuleName);
        return AplContractSpec.builder()
            .language(language)
            .version(version)
            .contractSpec(contractSpec)
            .content(src.getSourceCode())
            .build();
    }

    @Override
    public AplContractSpec loadAsrModuleSpec(Address address) {
        var smcEntity = contractRepository.loadContract(address);
        log.trace("Load specification for contract {} name={} type={}", address.getHex(), smcEntity.getName(), smcEntity.getBaseContract());
        return loadAsrModuleSpec(smcEntity.getBaseContract(), smcEntity.getLanguageName(), smcEntity.getLanguageVersion());
    }

    @Override
    public ContractSpecResponse loadContractSpecification(Address contractAddress) {
        var response = new ContractSpecResponse();
        var aplContractSpec = loadAsrModuleSpec(contractAddress);
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
        var result = processAllViewMethods(contractAddress, methodsToCall, executionLog);
        if (executionLog.hasError()) {
            return null;
        }
        result.add(ResultValue.builder()
            .method(JSLibraryProvider.CONTRACT_OVERVIEW_ITEM.getName())
            .output(List.of(contractAddress.getHex()))
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
            this.getInheritedAsrModules(contractSpec.getType()
                , aplContractSpec.getLanguage()
                , aplContractSpec.getVersion())
        );
        return null;
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

    @Override
    public List<ResultValue> processAllViewMethods(Address contractAddress, List<ContractMethod> members, ExecutionLog executionLog) {
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

        var rc = processor.batchProcess();
        executionLog.join(processor.getExecutionLog());
        return rc;
    }

    @Override
    public List<String> getInheritedAsrModules(String asrModuleName, String language, Version version) {
        checkLibraryCompatibility(language, version);
        var resultList = new ArrayList<String>();
        resultList.add(asrModuleName);
        resultList.addAll(libraryProvider.getInheritedModules(asrModuleName));
        return resultList;
    }

    @Override
    public List<String> getAsrModules(String language, Version version, String type) {
        if (libraryProvider.isCompatible(language, version)) {
            return libraryProvider.getAsrModules(type);
        } else {
            return List.of();
        }
    }

    private void checkLibraryCompatibility(String language, Version version) {
        if (!libraryProvider.isCompatible(language, version)) {
            throw new AplCoreContractViolationException("The library provider is not compatible for the given version of language ["
                + language + ":" + version + "], expected language="
                + libraryProvider.getLanguageName() + ", version=" + libraryProvider.getLanguageVersion());
        }
    }

}
