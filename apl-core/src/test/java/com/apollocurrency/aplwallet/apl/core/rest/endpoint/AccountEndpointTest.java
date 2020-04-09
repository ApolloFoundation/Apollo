package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.apl.core.account.service.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountAssetServiceImpl;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountCurrencyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountPublicKeyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountServiceImpl;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;
import com.apollocurrency.aplwallet.apl.core.app.TwoFactorAuthService;
import com.apollocurrency.aplwallet.apl.core.app.TwoFactorAuthServiceImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.PropertyProducer;
import com.apollocurrency.aplwallet.apl.core.http.ElGamalEncryptor;
import com.apollocurrency.aplwallet.apl.core.rest.converter.Account2FAConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.Account2FADetailsConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountAssetConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountBlockConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountCurrencyConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.WalletKeysConverter;
import com.apollocurrency.aplwallet.apl.core.rest.filters.Secured2FAInterceptor;
import com.apollocurrency.aplwallet.apl.core.rest.service.AccountStatisticsService;
import com.apollocurrency.aplwallet.apl.core.rest.service.OrderService;
import com.apollocurrency.aplwallet.apl.core.rest.utils.Account2FAHelper;
import com.apollocurrency.aplwallet.apl.core.rest.utils.FirstLastIndexParser;
import com.apollocurrency.aplwallet.apl.core.task.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.Setter;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.jboss.resteasy.mock.MockHttpRequest.post;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@EnableWeld
class AccountEndpointTest extends AbstractEndpointTest {

    private static final String PASSPHRASE = "123456";

    ElGamalEncryptor elGamal = new ElGamalEncryptor(mock(TaskDispatchManager.class));

    TwoFactorAuthService twoFactorAuthService = mock(TwoFactorAuthService.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, PropertyProducer.class,
        Account2FAHelper.class,
        AccountController.class,
        Secured2FAInterceptor.class,
        FirstLastIndexParser.class
    )
        .addBeans(MockBean.of(blockchain, Blockchain.class))
        .addBeans(MockBean.of(elGamal, ElGamalEncryptor.class))
        .addBeans(MockBean.of(twoFactorAuthService, TwoFactorAuthService.class, TwoFactorAuthServiceImpl.class))
        .addBeans(MockBean.of(mock(DirProvider.class), DirProvider.class))
        .addBeans(MockBean.of(mock(KeyStoreService.class), KeyStoreService.class))
        .addBeans(MockBean.of(mock(BlockchainConfig.class), BlockchainConfig.class))
        .addBeans(MockBean.of(mock(AccountService.class), AccountService.class, AccountServiceImpl.class))
        .addBeans(MockBean.of(mock(AccountPublicKeyService.class), AccountPublicKeyService.class, AccountPublicKeyServiceImpl.class))
        .addBeans(MockBean.of(mock(AccountAssetService.class), AccountAssetService.class, AccountAssetServiceImpl.class))
        .addBeans(MockBean.of(mock(AccountCurrencyService.class), AccountCurrencyService.class, AccountCurrencyServiceImpl.class))
        .addBeans(MockBean.of(mock(AccountAssetConverter.class), AccountAssetConverter.class))
        .addBeans(MockBean.of(mock(AccountCurrencyConverter.class), AccountCurrencyConverter.class))
        .addBeans(MockBean.of(mock(AccountConverter.class), AccountConverter.class))
        .addBeans(MockBean.of(mock(AccountBlockConverter.class), AccountBlockConverter.class))
        .addBeans(MockBean.of(mock(WalletKeysConverter.class), WalletKeysConverter.class))
        .addBeans(MockBean.of(mock(Account2FADetailsConverter.class), Account2FADetailsConverter.class))
        .addBeans(MockBean.of(mock(Account2FAConverter.class), Account2FAConverter.class))
        .addBeans(MockBean.of(mock(OrderService.class), OrderService.class))
        .addBeans(MockBean.of(mock(AccountStatisticsService.class), AccountStatisticsService.class))
        .build();

    @Inject
    @Setter
    private AccountController endpoint;

    @Inject
    private Secured2FAInterceptor secured2FAInterceptor;

    @BeforeEach
    void setUp() {
        super.setUp();

        dispatcher.getRegistry().addSingletonResource(endpoint);
        dispatcher.getProviderFactory().getContainerRequestFilterRegistry().registerSingleton(secured2FAInterceptor);
    }

    @AfterEach
    void tearDown() {

    }

    @ParameterizedTest(name = "{index} url={arguments}")
    @ValueSource(strings = {"/accounts/disable2fa", "/accounts/confirm2fa", "/accounts/delete-key"})
    public void check2FA_withoutMandatoryParameters_thenGetError_2002(String uri) throws URISyntaxException, IOException {
        MockHttpRequest request = post(uri);
        MockHttpResponse response = sendPostRequest(request, "wrong=value");

        checkMandatoryParameterMissingErrorCode(response, 2002);
    }

    @ParameterizedTest(name = "{index} url={arguments}")
    @ValueSource(strings = {"/accounts/disable2fa", "/accounts/confirm2fa"})
    public void check2FA_withBothSecretPhraseAndPassPhrase_thenGetError_2011(String uri) throws URISyntaxException, IOException {
        MockHttpRequest request = post(uri);
        MockHttpResponse response = sendPostRequest(request, "passphrase=" + PASSPHRASE + "&secretPhrase=" + SECRET + "&code2FA=" + CODE_2FA);

        checkMandatoryParameterMissingErrorCode(response, 2011);
    }

    @ParameterizedTest(name = "{index} url={arguments}")
    @ValueSource(strings = {"/accounts/disable2fa", "/accounts/confirm2fa", "/accounts/delete-key"})
    public void check2FA_withoutMandatoryParameter_Code2FA_thenGetError_2003(String uri) throws URISyntaxException, IOException {
        doReturn(true).when(twoFactorAuthService).isEnabled(ACCOUNT_ID_WITH_SECRET);
        MockHttpRequest request = post(uri);
        MockHttpResponse response = sendPostRequest(request, "secretPhrase=" + SECRET);

        checkMandatoryParameterMissingErrorCode(response, 2003);
    }

}