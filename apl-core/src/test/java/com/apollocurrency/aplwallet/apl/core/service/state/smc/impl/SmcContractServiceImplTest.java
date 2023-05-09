package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.api.v2.model.ContractSpecResponse;
import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.exception.AplCoreContractViolationException;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.MethodSpecMapper;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.SmartMethodMapper;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractRepository;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractService;
import com.apollocurrency.aplwallet.apl.smc.model.AplAddress;
import com.apollocurrency.aplwallet.apl.smc.model.AplContractSpec;
import com.apollocurrency.aplwallet.apl.smc.service.SmcContractTxBatchProcessor;
import com.apollocurrency.aplwallet.apl.util.Convert2;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.apollocurrency.aplwallet.apl.core.app.GenesisImporter.EPOCH_BEGINNING;
import static org.junit.jupiter.api.Assertions.*;
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
    SmartMethodMapper methodMapper = new SmartMethodMapper();
    @Mock
    MethodSpecMapper methodSpecMapper;
    @Mock
    SmcContractTxBatchProcessor processor;

    SmcContractService contractService;

    @BeforeEach
    void setUp() {
         when(smcConfig.createLanguageContext()).thenReturn(languageContext);
         when(languageContext.getLibraryProvider()).thenReturn(libraryProvider);
        contractService = new SmcContractServiceImpl(
            smcConfig, accountService, contractRepository, integratorFactory, methodMapper, methodSpecMapper, Optional.of(processor));
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
    @Test
    void test_getAsrModules() {
        //GIVEN
        Version version = new SimpleVersion("0", "1", "1");
        when(libraryProvider.isCompatible("js", version)).thenReturn(true).thenReturn(false);
        when(libraryProvider.getAsrModules(anyString())).thenReturn(List.of("one", "two"));
        //WHEN
        List<String> result = contractService.getAsrModules("js", version, "type");
        assertNotNull(result);
        result = contractService.getAsrModules("js", version, "type");
        assertTrue(result.isEmpty());

        //verify
        verify(libraryProvider, times(2)).isCompatible("js", version);
        verify(libraryProvider, times(1)).getAsrModules(anyString());
    }

    @SneakyThrows
    @Test
    void test_getInheritedAsrModulesError() {
        //GIVEN
        Version version = new SimpleVersion("0", "1", "1");
        when(libraryProvider.isCompatible("js", version)).thenReturn(false);
        //WHEN
        assertThrows(AplCoreContractViolationException.class, () ->
            contractService.getInheritedAsrModules("an_module", "js", version)
        );
        //verify
        verify(libraryProvider, times(1)).isCompatible("js", version);
    }

    @SneakyThrows
    @Test
    void test_loadContractSpecification() {
        Convert2.init("APL", EPOCH_BEGINNING);
        //GIVEN
        SmartContract smartContract = mock(SmartContract.class);
        when(smartContract.getBaseContract()).thenReturn("anyContract");
        when(smartContract.getLanguageName()).thenReturn("js");
        Version version = new SimpleVersion("0", "1", "1");
        when(smartContract.getLanguageVersion()).thenReturn(version);
        Address address = new AplAddress(1L);

        when(libraryProvider.isCompatible("js", version)).thenReturn(true);

        ContractSpec contractSpec = mock(ContractSpec.class);
        List<ContractSpec.Item> overview = List.of(new ContractSpec.Item("address", "address"));
        when(contractSpec.getOverview()).thenReturn(overview);
        List<ContractSpec.Member> members = List.of(
            new ContractSpec.Member(ContractSpec.MemberType.FUNCTION, "1",
                ContractSpec.Visibility.PUBLIC,
                ContractSpec.StateMutability.PAYABLE, null, List.of(), "value 1"),
            new ContractSpec.Member(ContractSpec.MemberType.FUNCTION, "2",
                ContractSpec.Visibility.EXTERNAL,
                ContractSpec.StateMutability.PAYABLE, null, List.of(), "value 2"),
            new ContractSpec.Member(ContractSpec.MemberType.FUNCTION, "3",
                ContractSpec.Visibility.EXTERNAL,
                ContractSpec.StateMutability.VIEW, null, List.of(), "value 3"),
            new ContractSpec.Member(ContractSpec.MemberType.CONSTRUCTOR, "4",
                ContractSpec.Visibility.PUBLIC,
                ContractSpec.StateMutability.NONPAYABLE, List.of(), null, "value 4"),
            new ContractSpec.Member(ContractSpec.MemberType.EVENT, "5",
                ContractSpec.Visibility.INTERNAL,
                ContractSpec.StateMutability.VIEW, List.of(), List.of(), "value 5")
        );
        when(contractSpec.getMembers()).thenReturn(members);
        when(libraryProvider.loadSpecification((anyString()))).thenReturn(contractSpec);

        ModuleSource moduleSource = mock(ModuleSource.class);
        when(libraryProvider.importModule((anyString()))).thenReturn(moduleSource);
        when(contractRepository.loadContract(address)).thenReturn(smartContract);

        //WHEN
        ContractSpecResponse response = contractService.loadContractSpecification(address);
        assertNull(response);
        //verify
        verify(libraryProvider, times(2)).isCompatible("js", version);
        verify(libraryProvider, times(1)).loadSpecification(anyString());
        verify(libraryProvider, times(1)).importModule(anyString());
        verify(contractRepository, times(1)).loadContract(address);

    }
}