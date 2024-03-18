/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.exception.AplCoreContractViolationException;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.MethodSpecMapper;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.SmartMethodMapper;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.SmcBlockchainIntegratorFactory;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.SmcContractServiceImpl;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.smc.polyglot.SimpleVersion;
import com.apollocurrency.smc.polyglot.language.ContractSpec;
import com.apollocurrency.smc.polyglot.language.LanguageContext;
import com.apollocurrency.smc.polyglot.language.lib.LibraryProvider;
import com.apollocurrency.smc.polyglot.language.lib.ModuleSource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author andrew.zinchenko@gmail.com
 */
@ExtendWith(MockitoExtension.class)
class SmcContractServiceTest {

    static {
        Convert2.init("APL", 1739068987193023818L);
    }

    @Mock
    SmcConfig smcConfig;
    @Mock
    LanguageContext languageContext;
    @Mock
    LibraryProvider libraryProvider;
    @Mock
    SmcContractRepository contractRepository;
    @Mock
    SmcBlockchainIntegratorFactory integratorFactory;
    @Mock
    AccountService accountService;
    @Mock
    SmartMethodMapper methodMapper;
    @Mock
    MethodSpecMapper methodSpecMapper;

    SmcContractService contractService;

    @BeforeEach
    void setUp() {
        when(smcConfig.createLanguageContext()).thenReturn(languageContext);
        when(languageContext.getLibraryProvider()).thenReturn(libraryProvider);
        contractService = new SmcContractServiceImpl(
            smcConfig, accountService, contractRepository, integratorFactory, methodMapper, methodSpecMapper, Optional.empty());
    }

    @SneakyThrows
    @Test
    void loadSpecByModuleName() {
        //GIVEN
        var language = "js";
        var version = SimpleVersion.fromString("0.1.1");
        var moduleName = "APL20";
        var module = mock(ContractSpec.class);
        when(libraryProvider.isCompatible(language, version)).thenReturn(true);
        when(libraryProvider.loadSpecification(moduleName)).thenReturn(module);
        when(libraryProvider.importModule(moduleName)).thenReturn(mock(ModuleSource.class));

        //WHEN
        var rc = contractService.loadAsrModuleSpec(moduleName, language, version);
        //THEN
        assertNotNull(rc);
    }

    @Test
    void loadSpecByModuleNameWithException() {
        //GIVEN
        var language = "js";
        var version = SimpleVersion.fromString("0.1.1");
        var moduleName = "APL20";
        when(libraryProvider.getLanguageName()).thenReturn(language);
        when(libraryProvider.getLanguageVersion()).thenReturn(version);

        //WHEN
        //THEN
        assertThrows(AplCoreContractViolationException.class, () -> contractService.loadAsrModuleSpec(moduleName, "java", version));
    }
}
