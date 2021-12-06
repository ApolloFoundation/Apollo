/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.exception.AplCoreContractViolationException;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractService;
import com.apollocurrency.aplwallet.apl.smc.model.AplContractSpec;
import com.apollocurrency.smc.polyglot.Version;
import com.apollocurrency.smc.polyglot.language.LanguageContext;
import com.apollocurrency.smc.polyglot.language.lib.LibraryProvider;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class SmcContractServiceImpl implements SmcContractService {
    protected final SmcConfig smcConfig;
    private final LibraryProvider libraryProvider;

    @Inject
    public SmcContractServiceImpl(SmcConfig smcConfig) {
        this.smcConfig = smcConfig;
        final LanguageContext languageContext = smcConfig.createLanguageContext();
        this.libraryProvider = languageContext.getLibraryProvider();
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
