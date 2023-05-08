package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.api.v2.model.ContractSpecResponse;
import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.MethodSpecMapper;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.SmartMethodMapper;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractRepository;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractService;
import com.apollocurrency.aplwallet.apl.smc.model.AplAddress;
import com.apollocurrency.aplwallet.apl.smc.model.AplContractSpec;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.polyglot.SimpleVersion;
import com.apollocurrency.smc.polyglot.Version;
import com.apollocurrency.smc.polyglot.language.ContractSpec;
import com.apollocurrency.smc.polyglot.language.LanguageContext;
import com.apollocurrency.smc.polyglot.language.lib.LibraryProvider;
import com.apollocurrency.smc.polyglot.language.lib.ModuleSource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmcContractServiceImplTest {

    @Mock
    LanguageContext languageContext;
    @Mock
    LibraryProvider libraryProvider;
    @Mock
    SmcConfig smcConfig;
    @Mock
    AccountService accountService;
    @Mock
    SmcContractRepository contractRepository;
    @Mock
    SmcBlockchainIntegratorFactory integratorFactory;
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
            smcConfig, accountService, contractRepository, integratorFactory, methodMapper, methodSpecMapper);
    }

    @SneakyThrows
    @Test
    void test_loadAsrModuleSpec() {
        //GIVEN
        SmartContract smartContract = mock(SmartContract.class);
        when(smartContract.getBaseContract()).thenReturn("anyContract");
        when(smartContract.getLanguageName()).thenReturn("anyLang");
        Version version = new SimpleVersion("0", "1", "1");
        when(smartContract.getLanguageVersion()).thenReturn(version);
        when(libraryProvider.isCompatible("anyLang", version)).thenReturn(true);
        ContractSpec contractSpec = mock(ContractSpec.class);
        when(libraryProvider.loadSpecification((anyString()))).thenReturn(contractSpec);
        ModuleSource moduleSource = mock(ModuleSource.class);
        when(libraryProvider.importModule((anyString()))).thenReturn(moduleSource);
        Address address = new AplAddress(1L);
        when(contractRepository.loadContract(address)).thenReturn(smartContract);
        //WHEN
        AplContractSpec aplContractSpec = contractService.loadAsrModuleSpec(address);
        assertNotNull(aplContractSpec);
        //THEN
        verify(libraryProvider, times(1)).isCompatible("anyLang", version);
        verify(libraryProvider, times(1)).importModule((anyString()));
        verify(libraryProvider, times(1)).importModule((anyString()));
    }

    @SneakyThrows
    @Disabled
    void test_loadContractSpecification() {
        //GIVEN
        SmartContract smartContract = mock(SmartContract.class);
        when(smartContract.getBaseContract()).thenReturn("anyContract");
        when(smartContract.getLanguageName()).thenReturn("anyLang");
        Version version = new SimpleVersion("0", "1", "1");
        when(smartContract.getLanguageVersion()).thenReturn(version);
        when(libraryProvider.isCompatible("anyLang", version)).thenReturn(true);
        ContractSpec contractSpec = mock(ContractSpec.class);
        when(libraryProvider.loadSpecification((anyString()))).thenReturn(contractSpec);
        ModuleSource moduleSource = mock(ModuleSource.class);
        when(libraryProvider.importModule((anyString()))).thenReturn(moduleSource);
        Address address = new AplAddress(1L);
        when(contractRepository.loadContract(address)).thenReturn(smartContract);
        //WHEN
        ContractSpecResponse contractSpecLoaded = contractService.loadContractSpecification(address);
        assertNotNull(contractSpecLoaded);
    }
}