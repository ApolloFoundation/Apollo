/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.account.AccountDTO;
import com.apollocurrency.aplwallet.api.dto.account.AccountEffectiveBalanceDto;
import com.apollocurrency.aplwallet.api.dto.account.AccountsCountDto;
import com.apollocurrency.aplwallet.api.dto.auth.Status2FA;
import com.apollocurrency.aplwallet.api.dto.auth.TwoFactorAuthParameters;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.AskOrder;
import com.apollocurrency.aplwallet.apl.core.rest.converter.Account2FAConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.Account2FADetailsConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountAssetConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.AccountCurrencyConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.BlockConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.TransactionConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.UnconfirmedTransactionConverter;
import com.apollocurrency.aplwallet.apl.core.rest.service.AccountStatisticsService;
import com.apollocurrency.aplwallet.apl.core.rest.utils.FirstLastIndexParser;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.order.OrderService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAskOrderPlacement;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.vault.model.ApolloFbWallet;
import com.apollocurrency.aplwallet.vault.model.KMSResponseStatus;
import com.apollocurrency.aplwallet.vault.model.TwoFactorAuthDetails;
import com.apollocurrency.aplwallet.vault.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.vault.rest.converter.WalletKeysConverter;
import com.apollocurrency.aplwallet.vault.service.auth.Account2FAService;
import com.apollocurrency.aplwallet.vault.util.AccountGeneratorUtil;
import com.apollocurrency.aplwallet.vault.util.AccountHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_0_GENERATOR;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_0_HEIGHT;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_0_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_0_TIMESTAMP;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_1_GENERATOR;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_1_HEIGHT;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_1_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_1_TIMESTAMP;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_2_GENERATOR;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_2_HEIGHT;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_2_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_2_TIMESTAMP;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_3_GENERATOR;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_3_HEIGHT;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_3_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_3_TIMESTAMP;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.GENESIS_BLOCK_GENERATOR;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.GENESIS_BLOCK_HEIGHT;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.GENESIS_BLOCK_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.GENESIS_BLOCK_TIMESTAMP;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.buildBlock;
import static com.apollocurrency.aplwallet.vault.service.auth.Account2FAService.TWO_FACTOR_AUTH_PARAMETERS_ATTRIBUTE_NAME;
import static org.jboss.resteasy.mock.MockHttpRequest.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest extends AbstractEndpointTest {

    public static final String PUBLIC_KEY_HEX = "e03f00485cabc82491d05297acd9d140f62d61d86f16ba4bcf2a922482a4617d";
    public static final String ACCOUNT_RS = "APL-5MRD-NBKX-X5EJ-3UP2M";
    public static final long ACCOUNT_ID = 1838236804542746347L;
    public static final String QR_CODE_URL = "https://url.google.com/qrcode";

    public static final long ASSET_ID = 8180990979457659735L;
    public static final long CURRENCY_ID = 784842454721729391L;

    public Account account;
    public AccountDTO accountDTO;
    public AccountAsset accountAsset;
    public AccountCurrency accountCurrency;

    private AccountController endpoint;

    @Mock
    private AccountConverter accountConverter;
    @Mock
    private AccountService accountService;
    @Mock
    private AccountPublicKeyService accountPublicKeyService;
    @Mock
    private AccountAssetService accountAssetService;
    @Mock
    private AccountCurrencyService accountCurrencyService;
    @Mock
    private AccountCurrencyConverter accountCurrencyConverter;
    @Mock
    private AccountAssetConverter accountAssetConverter;
    @Mock
    private OrderService<AskOrder, ColoredCoinsAskOrderPlacement> orderService;
    @Mock
    private CurrencyService currencyService;
    @Mock
    PrunableLoadingService prunableLoadingService;

    private TransactionConverter transactionConverter = new TransactionConverter(blockchain, new UnconfirmedTransactionConverter(prunableLoadingService));
    private BlockConverter blockConverter = new BlockConverter(
        blockchain, transactionConverter,
        mock(PhasingPollService.class), mock(AccountService.class));

    @Mock
    private Account2FAService account2FAService;

    private FirstLastIndexParser indexParser = new FirstLastIndexParser(100);
    @Mock
    private AccountStatisticsService accountStatisticsService = Mockito.mock(AccountStatisticsService.class);
    @Mock
    private AssetService assetService = Mockito.mock(AssetService.class);

    private Block GENESIS_BLOCK, LAST_BLOCK, NEW_BLOCK;
    private Block BLOCK_0, BLOCK_1, BLOCK_2, BLOCK_3;
    private List<Block> BLOCKS;

    @BeforeEach
    void setUp() {
        super.setUp();

        endpoint = new AccountController(
            blockchain,
            account2FAService,
            accountService,
            accountPublicKeyService,
            accountAssetService,
            accountCurrencyService,
            accountAssetConverter,
            accountCurrencyConverter,
            accountConverter,
            blockConverter,
            new WalletKeysConverter(),
            new Account2FADetailsConverter(),
            new Account2FAConverter(),
            orderService,
            100,
            accountStatisticsService,
            assetService,
            currencyService
        );

        dispatcher.getRegistry().addSingletonResource(endpoint);
        account = new Account(ACCOUNT_ID, 1000L, 1500L, 0L, 0L, 0);
        account.setPublicKey(new PublicKey(ACCOUNT_ID, Convert.parseHexString(PUBLIC_KEY_HEX), 0));

        accountDTO = new AccountDTO(ACCOUNT_ID, ACCOUNT_RS,
            account.getBalanceATM(),
            account.getForgedBalanceATM(),
            account.getUnconfirmedBalanceATM());

        accountAsset = new AccountAsset(
            ACCOUNT_ID, ASSET_ID,
            1000, 1100,
            CURRENT_HEIGHT);

        accountCurrency = new AccountCurrency(
            ACCOUNT_ID,
            CURRENCY_ID,
            1000, 1100,
            CURRENT_HEIGHT);

        setupBlocks();
    }

    @Test
    void getAccount_whenCallWithoutMandatoryParameter_thenGetError_2001() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/account");

        checkMandatoryParameterMissingErrorCode(response, 2001);

    }

    @Test
    void getAccount_whenCallWithWrongAccountId_thenGetError_2005() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/account?account=0");

        checkMandatoryParameterMissingErrorCode(response, 2005);

    }

    @Test
    void getAccount_byAccountID_withoutIncludingAdditionalInformation() throws URISyntaxException, IOException {
        doReturn(account).when(accountService).getAccount(ACCOUNT_ID);
        doReturn(accountDTO).when(accountConverter).convert(account);
        MockHttpResponse response = sendGetRequest("/accounts/account?account=" + ACCOUNT_ID);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:" + result.get("newErrorCode"));
        assertEquals(Long.toUnsignedString(ACCOUNT_ID), result.get("account"));
        assertEquals(ACCOUNT_RS, result.get("accountRS"));
        //verify
        verify(accountConverter, times(1)).convert(account);
        verify(accountService, times(1)).getAccount(ACCOUNT_ID);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"BlaBlaBla"})
    void createAccount(String pass) throws URISyntaxException, IOException {
        WalletKeysInfo info = createWalletKeysInfo(pass);
        doReturn(info).when(account2FAService).generateUserWallet(pass);

        MockHttpResponse response = sendPostRequest("/accounts/account", pass == null ? "wrong=value" : "passphrase=" + pass);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:" + result.get("newErrorCode"));
        assertNotNull(result.get("account"));
        assertNotNull(result.get("accountRS"));
        assertNotNull(result.get("publicKey"));
        //verify
        verify(account2FAService, times(1)).generateUserWallet(pass);
    }

    @Test
    void enable2FA_withoutMandatoryParameters_thenGetError_2002() throws URISyntaxException, IOException {
        when(account2FAService.create2FAParameters(null, null, null, null)).thenCallRealMethod();
        MockHttpResponse response = sendPostRequest("/accounts/enable2fa", "wrong=value");

        checkMandatoryParameterMissingErrorCode(response, 2002);
        verify(account2FAService, times(1)).create2FAParameters(null, null, null, null);
    }

    @Test
    void enable2FA_withBothSecretPhraseAndPassPhrase_thenGetError_2011() throws URISyntaxException, IOException {
        when(account2FAService.create2FAParameters(null, PASSPHRASE, SECRET, null)).thenCallRealMethod();
        MockHttpResponse response = sendPostRequest("/accounts/enable2fa", "passphrase=" + PASSPHRASE + "&secretPhrase=" + SECRET);

        checkMandatoryParameterMissingErrorCode(response, 2011);
        verify(account2FAService, times(1)).create2FAParameters(null, PASSPHRASE, SECRET, null);
    }

    @Test
    void enable2FA_withoutMandatoryParameter_Account_thenGetError_2003() throws URISyntaxException, IOException {
        when(account2FAService.create2FAParameters(null, PASSPHRASE, null, null)).thenCallRealMethod();
        MockHttpResponse response = sendPostRequest("/accounts/enable2fa", "passphrase=" + PASSPHRASE);

        checkMandatoryParameterMissingErrorCode(response, 2003);
        verify(account2FAService, times(1)).create2FAParameters(null, PASSPHRASE, null, null);
    }

    @Test
    void enable2FA_withPassPhraseAndAccount() throws URISyntaxException, IOException {
        TwoFactorAuthParameters params2FA = new TwoFactorAuthParameters(ACCOUNT_ID, PASSPHRASE, null);
        TwoFactorAuthDetails authDetails = new TwoFactorAuthDetails(QR_CODE_URL, SECRET, Status2FA.OK);

        doReturn(params2FA).when(account2FAService).create2FAParameters(ACCOUNT_RS, PASSPHRASE, null, null);
        doReturn(authDetails).when(account2FAService).enable2FA(params2FA);
        MockHttpResponse response = sendPostRequest("/accounts/enable2fa", "passphrase=" + PASSPHRASE + "&account=" + ACCOUNT_RS);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:" + result.get("newErrorCode"));

        assertEquals(Long.toUnsignedString(ACCOUNT_ID), result.get("account"));
        assertEquals(ACCOUNT_RS, result.get("accountRS"));
        assertEquals(QR_CODE_URL, result.get("qrCodeUrl"));
        assertEquals(SECRET, result.get("secret"));
        verify(account2FAService, times(1)).create2FAParameters(ACCOUNT_RS, PASSPHRASE, null, null);
        verify(account2FAService, times(1)).enable2FA(params2FA);
    }

    @Test
    void enable2FA_withSecretPhrase() throws URISyntaxException, IOException {
        TwoFactorAuthParameters params2FA = new TwoFactorAuthParameters(ACCOUNT_ID, null, SECRET);
        TwoFactorAuthDetails authDetails = new TwoFactorAuthDetails(QR_CODE_URL, SECRET, Status2FA.OK);

        doReturn(params2FA).when(account2FAService).create2FAParameters(null, null, SECRET, null);
        doReturn(authDetails).when(account2FAService).enable2FA(params2FA);
        MockHttpResponse response = sendPostRequest("/accounts/enable2fa", "secretPhrase=" + SECRET);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:" + result.get("newErrorCode"));

        assertEquals(Long.toUnsignedString(ACCOUNT_ID), result.get("account"));
        assertEquals(ACCOUNT_RS, result.get("accountRS"));
        assertEquals(QR_CODE_URL, result.get("qrCodeUrl"));
        assertEquals(SECRET, result.get("secret"));
        verify(account2FAService, times(1)).create2FAParameters(null, null, SECRET, null);
        verify(account2FAService, times(1)).enable2FA(params2FA);
    }

    @Test
    void disable2FA_withPassPhraseAndAccountAndCode2FA() throws URISyntaxException, IOException {
        String uri = "/accounts/disable2fa";
        TwoFactorAuthParameters twoFactorAuthParameters = new TwoFactorAuthParameters(ACCOUNT_ID, PASSPHRASE, null);
        twoFactorAuthParameters.setCode2FA(CODE_2FA);

        doReturn(Status2FA.OK).when(account2FAService).disable2FA(twoFactorAuthParameters);
        check2FA_withPassPhraseAndAccountAndCode2FA(uri, twoFactorAuthParameters);
        verify(account2FAService, times(1)).disable2FA(twoFactorAuthParameters);
    }

    @Test
    void disable2FA_withSecretPhraseAndCode2FA() throws URISyntaxException, IOException {
        String uri = "/accounts/disable2fa";
        TwoFactorAuthParameters twoFactorAuthParameters = new TwoFactorAuthParameters(ACCOUNT_ID, null, SECRET);
        twoFactorAuthParameters.setCode2FA(CODE_2FA);

        doReturn(Status2FA.OK).when(account2FAService).disable2FA(twoFactorAuthParameters);
        check2FA_withSecretPhraseAndCode2FA(uri, twoFactorAuthParameters);
        verify(account2FAService, times(1)).disable2FA(twoFactorAuthParameters);
    }

    @Test
    void confirm2FA_withPassPhraseAndAccountAndCode2FA() throws URISyntaxException, IOException {
        String uri = "/accounts/confirm2fa";
        TwoFactorAuthParameters twoFactorAuthParameters = new TwoFactorAuthParameters(ACCOUNT_ID, PASSPHRASE, null);
        twoFactorAuthParameters.setCode2FA(CODE_2FA);

        doReturn(Status2FA.OK).when(account2FAService).confirm2FA(twoFactorAuthParameters);
        check2FA_withPassPhraseAndAccountAndCode2FA(uri, twoFactorAuthParameters);
        verify(account2FAService, times(1)).confirm2FA(twoFactorAuthParameters);
    }

    @Test
    void confirm2FA_withSecretPhraseAndCode2FA() throws URISyntaxException, IOException {
        String uri = "/accounts/confirm2fa";
        TwoFactorAuthParameters twoFactorAuthParameters = new TwoFactorAuthParameters(ACCOUNT_ID, null, SECRET);
        twoFactorAuthParameters.setCode2FA(CODE_2FA);

        doReturn(Status2FA.OK).when(account2FAService).confirm2FA(twoFactorAuthParameters);
        check2FA_withSecretPhraseAndCode2FA(uri, twoFactorAuthParameters);
        verify(account2FAService, times(1)).confirm2FA(twoFactorAuthParameters);
    }

    @Test
    void deleteKey_withPassPhraseAndAccountAndCode2FA() throws URISyntaxException, IOException {
        String uri = "/accounts/delete-key";
        TwoFactorAuthParameters twoFactorAuthParameters = new TwoFactorAuthParameters(ACCOUNT_ID, PASSPHRASE, null);
        twoFactorAuthParameters.setCode2FA(CODE_2FA);

        doReturn(KMSResponseStatus.OK).when(account2FAService).deleteAccount(twoFactorAuthParameters);
        check2FA_withPassPhraseAndAccountAndCode2FA(uri, twoFactorAuthParameters);
        verify(account2FAService, times(1)).deleteAccount(twoFactorAuthParameters);
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrong=value", "passphrase=" + PASSPHRASE + "&wrongAccount=" + ACCOUNT_RS, "wrongPassphrase=" + PASSPHRASE + "&account=" + ACCOUNT_RS})
    void deleteKey_withoutMandatoryParameters_thenGetError_2001(String bodyParams) throws URISyntaxException, IOException {
        MockHttpResponse response = sendPostRequest("/accounts/delete-key", bodyParams);

        checkMandatoryParameterMissingErrorCode(response, 2001);
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrong=value", "passphrase=" + PASSPHRASE + "&wrongAccount=" + ACCOUNT_RS, "wrongPassphrase=" + PASSPHRASE + "&account=" + ACCOUNT_RS})
    void exportKey_withoutMandatoryParameters_thenGetError_2001(String bodyParams) throws URISyntaxException, IOException {
        MockHttpResponse response = sendPostRequest("/accounts/export-key", bodyParams);

        checkMandatoryParameterMissingErrorCode(response, 2001);
    }

    @Test
    void exportKey_withPassPhraseAndAccountAndCode2FA() throws URISyntaxException, IOException {
        String uri = "/accounts/export-key";
        byte[] secretBytes = SECRET.getBytes();
        TwoFactorAuthParameters twoFactorAuthParameters = new TwoFactorAuthParameters(ACCOUNT_ID, PASSPHRASE, null);
        twoFactorAuthParameters.setCode2FA(CODE_2FA);
        doReturn(twoFactorAuthParameters).when(account2FAService).create2FAParameters(ACCOUNT_RS, PASSPHRASE, null, null);

        doReturn(secretBytes).when(account2FAService).findAplSecretBytes(twoFactorAuthParameters);
        check2FA_withPassPhraseAndAccountAndCode2FA(uri, twoFactorAuthParameters);
        verify(account2FAService, times(1)).create2FAParameters(ACCOUNT_RS, PASSPHRASE, null, null);
        verify(account2FAService, times(1)).findAplSecretBytes(twoFactorAuthParameters);
    }

    @Test
    void getAccountAssetCount_whenCallWithoutMandatoryParameter_thenGetError_2001() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/asset-count");

        checkMandatoryParameterMissingErrorCode(response, 2001);
    }

    @Test
    void getAccountAssetCount_whenCallwithWrongHeight_thenGetError_2001() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/asset-count?account=" + ACCOUNT_ID + "&height=" + (CURRENT_HEIGHT + 10));

        checkMandatoryParameterMissingErrorCode(response, 2001);
    }

    @Test
    void getAccountAssetCount_callOnEmptyHeight() throws URISyntaxException, IOException {
        int count = 15;
        doReturn(count).when(accountAssetService).getCountByAccount(ACCOUNT_ID, -1);
        MockHttpResponse response = sendGetRequest("/accounts/asset-count?account=" + ACCOUNT_ID);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);

        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:" + result.get("newErrorCode"));

        assertEquals(count, result.get("numberOfAssets"));
        verify(accountAssetService, times(1)).getCountByAccount(ACCOUNT_ID, -1);
    }

    @Test
    void getAccountAssetCount() throws URISyntaxException, IOException {
        int count = 38;
        doReturn(count).when(accountAssetService).getCountByAccount(ACCOUNT_ID, CURRENT_HEIGHT);
        MockHttpResponse response = sendGetRequest("/accounts/asset-count?account=" + ACCOUNT_ID + "&height=" + CURRENT_HEIGHT);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:" + result.get("newErrorCode"));

        assertEquals(count, result.get("numberOfAssets"));
        verify(accountAssetService, times(1)).getCountByAccount(ACCOUNT_ID, CURRENT_HEIGHT);
    }

    @Test
    void getAccountAssets_whenCallWithoutMandatoryParameter_thenGetError_2001() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/assets");

        checkMandatoryParameterMissingErrorCode(response, 2001);
    }

    @Test
    void getAccountAssets_whenCallwithWrongHeight_thenGetError_2001() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/assets?account=" + ACCOUNT_ID + "&height=" + (CURRENT_HEIGHT + 10));

        checkMandatoryParameterMissingErrorCode(response, 2001);
    }

    @Test
    void getAccountAssets_whenCallwithWrongAsset_thenGetError_2001() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/assets?account=" + ACCOUNT_ID + "&asset=AS123&height=" + (CURRENT_HEIGHT + 10));

        checkMandatoryParameterMissingErrorCode(response, 2001);
    }

    @Test
    void getAccountAssets_getList() throws URISyntaxException, IOException {
        endpoint.setAccountAssetConverter(new AccountAssetConverter());
        doReturn(List.of(accountAsset)).when(accountAssetService).getAssetsByAccount(ACCOUNT_ID, CURRENT_HEIGHT, 0, 99);

        MockHttpResponse response = sendGetRequest("/accounts/assets?account=" + ACCOUNT_ID + "&height=" + CURRENT_HEIGHT);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);

        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:" + result.get("newErrorCode"));

        JsonNode root = mapper.readTree(content);
        assertTrue(root.get("accountAssets").isArray());
        assertEquals(Long.toUnsignedString(ASSET_ID), root.withArray("accountAssets").get(0).get("asset").asText());
        verify(accountAssetService, times(1)).getAssetsByAccount(ACCOUNT_ID, CURRENT_HEIGHT, 0, 99);
    }

    @Test
    void getAccountAssets_getOne() throws URISyntaxException, IOException {
        endpoint.setAccountAssetConverter(new AccountAssetConverter());
        doReturn(accountAsset).when(accountAssetService).getAsset(ACCOUNT_ID, ASSET_ID, CURRENT_HEIGHT);

        MockHttpResponse response = sendGetRequest(
            "/accounts/assets?account=" + ACCOUNT_ID + "&asset=" + ASSET_ID + "&height=" + CURRENT_HEIGHT);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);

        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:" + result.get("newErrorCode"));
        assertEquals(Long.toUnsignedString(ASSET_ID), result.get("asset"));
        verify(accountAssetService, times(1)).getAsset(ACCOUNT_ID, ASSET_ID, CURRENT_HEIGHT);
    }

    @Test
    void getAccountCurrencyCount_whenCallWithoutMandatoryParameter_thenGetError_2001() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/currency-count");

        checkMandatoryParameterMissingErrorCode(response, 2001);
    }

    @Test
    void getAccountCurrencyCount_whenCallwithWrongHeight_thenGetError_2001() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/currency-count?account=" + ACCOUNT_ID + "&height=" + (CURRENT_HEIGHT + 10));

        checkMandatoryParameterMissingErrorCode(response, 2001);
    }

    @Test
    void getAccountCurrencyCount() throws URISyntaxException, IOException {
        int count = 58;

        doReturn(count).when(accountCurrencyService).getCountByAccount(ACCOUNT_ID, CURRENT_HEIGHT);
        MockHttpResponse response = sendGetRequest("/accounts/currency-count?account=" + ACCOUNT_ID + "&height=" + CURRENT_HEIGHT);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:" + result.get("newErrorCode"));

        assertEquals(count, result.get("numberOfCurrencies"));
        verify(accountCurrencyService, times(1)).getCountByAccount(ACCOUNT_ID, CURRENT_HEIGHT);
    }

    @Test
    void getAccountCurrencies_whenCallWithoutMandatoryParameter_thenGetError_2001() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/currencies");

        checkMandatoryParameterMissingErrorCode(response, 2001);
    }

    @Test
    void getAccountCurrencies_whenCallwithWrongHeight_thenGetError_2001() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/currencies?account=" + ACCOUNT_ID + "&height=" + (CURRENT_HEIGHT + 10));

        checkMandatoryParameterMissingErrorCode(response, 2001);
    }

    @Test
    void getAccountCurrencies_whenCallwithWrongCurrencyId_thenGetError_2001() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/currencies?account=" + ACCOUNT_ID + "&currency=AS123&height=" + (CURRENT_HEIGHT + 10));

        checkMandatoryParameterMissingErrorCode(response, 2001);
        NotFoundException exception;
    }

    @Test
    void getAccountCurrencies_getList() throws URISyntaxException, IOException {
        endpoint.setAccountCurrencyConverter(new AccountCurrencyConverter());
        doReturn(List.of(accountCurrency)).when(accountCurrencyService).getCurrenciesByAccount(ACCOUNT_ID, CURRENT_HEIGHT, 0, 99);

        MockHttpResponse response = sendGetRequest("/accounts/currencies?account=" + ACCOUNT_ID + "&height=" + CURRENT_HEIGHT);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);

        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:" + result.get("newErrorCode"));

        JsonNode root = mapper.readTree(content);
        assertTrue(root.get("accountCurrencies").isArray());
        assertEquals(Long.toUnsignedString(CURRENCY_ID), root.withArray("accountCurrencies").get(0).get("currency").asText());
        verify(accountCurrencyService, times(1)).getCurrenciesByAccount(ACCOUNT_ID, CURRENT_HEIGHT, 0, 99);
    }

    @Test
    void getAccountCurrencies_getOne_byCurrencyId() throws URISyntaxException, IOException {
        endpoint.setAccountCurrencyConverter(new AccountCurrencyConverter());
        doReturn(accountCurrency).when(accountCurrencyService).getAccountCurrency(ACCOUNT_ID, CURRENCY_ID, CURRENT_HEIGHT);

        MockHttpResponse response = sendGetRequest(
            "/accounts/currencies?account=" + ACCOUNT_ID + "&currency=" + CURRENCY_ID + "&height=" + CURRENT_HEIGHT);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);

        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:" + result.get("newErrorCode"));
        assertEquals(Long.toUnsignedString(CURRENCY_ID), result.get("currency"));
        verify(accountCurrencyService, times(1)).getAccountCurrency(ACCOUNT_ID, CURRENCY_ID, CURRENT_HEIGHT);
    }

    @Test
    void getAccountBlockCount_whenCallWithoutMandatoryParameter_thenGetError_2001() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/block-count");

        checkMandatoryParameterMissingErrorCode(response, 2001);
    }

    @Test
    void getAccountBlockCount_whenCallwithWrongAccount_thenGetError_2005() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/block-count?account=0");

        checkMandatoryParameterMissingErrorCode(response, 2005);
    }

    @Test
    void getAccountBlockCount() throws URISyntaxException, IOException {
        int count = 10000;
        doReturn(count).when(blockchain).getBlockCount(ACCOUNT_ID);
        MockHttpResponse response = sendGetRequest("/accounts/block-count?account=" + ACCOUNT_ID);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:" + result.get("newErrorCode"));

        assertEquals(count, result.get("numberOfBlocks"));
        verify(blockchain, times(1)).getBlockCount(ACCOUNT_ID);
    }

    @Test
    void getAccountBlockIds_whenCallWithoutMandatoryParameter_thenGetError_2001() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/block-ids");

        checkMandatoryParameterMissingErrorCode(response, 2001);
    }

    @Test
    void getAccountBlockIds_whenCallwithWrongAccount_thenGetError_2005() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/block-ids?account=0");

        checkMandatoryParameterMissingErrorCode(response, 2005);
    }

    @Test
    void getAccountBlockIds() throws URISyntaxException, IOException {
        int timestamp = (int) (System.currentTimeMillis() / 1000);
        int from = 0;
        int to = 99;

        doReturn(BLOCKS).when(accountService).getAccountBlocks(ACCOUNT_ID, from, to, timestamp);

        MockHttpResponse response = sendGetRequest("/accounts/block-ids?account=" + ACCOUNT_ID
            + "&timestamp=" + timestamp
            + "&firstIndex=" + from
            + "&lastIndex=" + to
        );

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:" + result.get("newErrorCode"));

        JsonNode root = mapper.readTree(content);
        assertTrue(root.get("blockIds").isArray());
        assertEquals(BLOCKS.size(), root.withArray("blockIds").size());
        verify(accountService, times(1)).getAccountBlocks(ACCOUNT_ID, from, to, timestamp);
    }

    @Test
    void getAccountBlocks_whenCallWithoutMandatoryParameter_thenGetError_2001() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/blocks");

        checkMandatoryParameterMissingErrorCode(response, 2001);
    }

    @Test
    void getAccountBlocks_whenCallwithWrongAccount_thenGetError_2005() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/blocks?account=0");

        checkMandatoryParameterMissingErrorCode(response, 2005);
    }

    @Test
    void getAccountBlocks() throws URISyntaxException, IOException {
        int timestamp = (int) (System.currentTimeMillis() / 1000);
        int from = 0;
        int to = 200;

        doReturn(BLOCKS).when(accountService).getAccountBlocks(ACCOUNT_ID, from, 99, timestamp);

        MockHttpResponse response = sendGetRequest("/accounts/blocks?account=" + ACCOUNT_ID
            + "&timestamp=" + timestamp
            + "&firstIndex=" + from
            + "&lastIndex=" + to
        );

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:" + result.get("newErrorCode"));

        JsonNode root = mapper.readTree(content);
        assertTrue(root.get("blocks").isArray());
        assertEquals(BLOCKS.size(), root.withArray("blocks").size());
        assertEquals(Long.toUnsignedString(BLOCK_0.getGeneratorId()), root.withArray("blocks").get(1).get("generator").asText());
        verify(accountService, times(1)).getAccountBlocks(ACCOUNT_ID, from, 99, timestamp);
    }

    @ParameterizedTest(name = "{index} url={arguments}")
    @ValueSource(strings = {"/accounts/disable2fa", "/accounts/confirm2fa", "/accounts/delete-key"})
    public void check2FA_withoutRequestAttribute_thenGetError_2001(String uri) throws URISyntaxException, IOException {
        MockHttpResponse response = sendPostRequest(uri, "wrong=value");

        checkMandatoryParameterMissingErrorCode(response, 2001);
    }

    @ParameterizedTest
    @ValueSource(strings = {"?numberOfAccounts=" + Constants.MIN_TOP_ACCOUNTS_NUMBER, "?numberOfAccounts="})
    void counts_SUCCESS(String numberOfAccounts) throws URISyntaxException, IOException {
        String accountCountUri = "/accounts/statistic";

        // prepare data
        AccountsCountDto dto = new AccountsCountDto(112L, 113L, 1, 124L);
        AccountEffectiveBalanceDto balanceDto = new AccountEffectiveBalanceDto(
            100L, 200L, 100L, 200L, 200L, "123", "RS-ADVB");
        dto.topHolders.add(balanceDto);
        doReturn(dto).when(accountStatisticsService).getAccountsStatistic(Constants.MIN_TOP_ACCOUNTS_NUMBER);
        // init mocks
//        ServerInfoController controller = new ServerInfoController(serverInfoService);
        dispatcher.getRegistry().addSingletonResource(endpoint);
        // call
        String uri = accountCountUri + numberOfAccounts;
        MockHttpRequest request = MockHttpRequest.get(uri);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        // check
        assertEquals(200, response.getStatus());
        String respondJson = response.getContentAsString();
        AccountsCountDto dtoResult = mapper.readValue(respondJson, new TypeReference<>() {
        });
        assertNotNull(dtoResult.topHolders);
        assertEquals(112L, dtoResult.totalSupply);
        assertEquals(1, dtoResult.topHolders.size());

        // verify
        verify(accountStatisticsService, times(1)).getAccountsStatistic(Constants.MIN_TOP_ACCOUNTS_NUMBER);
    }


    private void check2FA_withPassPhraseAndAccountAndCode2FA(String uri, TwoFactorAuthParameters twoFactorAuthParameters) throws URISyntaxException, IOException {
        //doCallRealMethod().when(account2FAHelper).validate2FAParameters(twoFactorAuthParameters);

        MockHttpRequest request = post(uri);
        request.setAttribute(TWO_FACTOR_AUTH_PARAMETERS_ATTRIBUTE_NAME, twoFactorAuthParameters);
        MockHttpResponse response = sendPostRequest(request, "passphrase=" + PASSPHRASE + "&account=" + ACCOUNT_RS + "&code2FA=" + CODE_2FA);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:" + result.get("newErrorCode"));

        assertEquals(Long.toUnsignedString(ACCOUNT_ID), result.get("account"));
        assertEquals(ACCOUNT_RS, result.get("accountRS"));
    }

    private void check2FA_withSecretPhraseAndCode2FA(String uri, TwoFactorAuthParameters twoFactorAuthParameters) throws URISyntaxException, IOException {
        //doCallRealMethod().when(account2FAHelper).validate2FAParameters(twoFactorAuthParameters);

        MockHttpRequest request = post(uri);
        request.setAttribute(TWO_FACTOR_AUTH_PARAMETERS_ATTRIBUTE_NAME, twoFactorAuthParameters);

        MockHttpResponse response = sendPostRequest(request, "secretPhrase=" + SECRET + "&code2FA=" + CODE_2FA);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:" + result.get("newErrorCode"));

        assertEquals(Long.toUnsignedString(ACCOUNT_ID), result.get("account"));
        assertEquals(ACCOUNT_RS, result.get("accountRS"));
    }

    private void setupBlocks() {
        GENESIS_BLOCK = buildBlock(GENESIS_BLOCK_ID, GENESIS_BLOCK_HEIGHT, -1, GENESIS_BLOCK_TIMESTAMP, 0, 0, 0, 0, "0000000000000000000000000000000000000000000000000000000000000000", "00", 5124095, 8235640967557025109L, "bc26bb638c9991f88fa52365591e00e22d3e9f9ad721ca4fe1683c8795a037e5", "bc26bb638c9991f88fa52365591e00e22d3e9f9ad721ca4fe1683c8795a037e5", "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000", "0000000000000000000000000000000000000000000000000000000000000000", GENESIS_BLOCK_GENERATOR, 0, Collections.emptyList());
        BLOCK_0 = buildBlock(BLOCK_0_ID, BLOCK_0_HEIGHT, 3, BLOCK_0_TIMESTAMP, 9108206803338182346L, 0, 100000000, 1255, "37f76b234414e64d33b71db739bd05d2cf3a1f7b344a88009b21c89143a00cd0", "026543d9a8161629", 9331842, -1868632362992335764L, "002bc5d6612e35e00e0a8141382eab45c20243d9dad4823348bfe85147b95acf", "002bc5d6612e35e00e0a8141382eab45c20243d9dad4823348bfe85147b95acf", "e920b526c9200ae5e9757049b3b16fcb050b416587b167cb9d5ca0dc71ec970df48c37ce310b6d20b9972951e9844fa817f0ff14399d9e0f82fde807d0957c31", "cabec48dd4d9667e562234245d06098f3f51f8dc9881d1959496fd73d7266282", BLOCK_0_GENERATOR, 0, Collections.emptyList());
        BLOCK_1 = buildBlock(BLOCK_1_ID, BLOCK_1_HEIGHT, 3, BLOCK_1_TIMESTAMP, -3475222224033883190L, 0, 100000000, 1257, "2cba9a6884de01ff23723887e565cbde21a3f5a0a70e276f3633645a97ed14c6", "026601a7a1c313ca", 7069966, 5841487969085496907L, "fbf795ff1d4138f11ea3d38842aa319f8a21589eb46ea8cfc71850f8b55508ef", "fbf795ff1d4138f11ea3d38842aa319f8a21589eb46ea8cfc71850f8b55508ef", "978b50eb629296b450f5298b61601685cbe965d4995b03707332fdc335a0e708a453bd7969bd9d336fbafcacd89073bf55c3b3395acf6dd0f3204c2a5d4b402e", "cadbeabccc87c5cf1cf7d2cf7782eb34a58fb2811c79e1d0a3cc60099557f4e0", BLOCK_1_GENERATOR, 0, Collections.emptyList());
        BLOCK_2 = buildBlock(BLOCK_2_ID, BLOCK_2_HEIGHT, 5, BLOCK_2_TIMESTAMP, 2069655134915376442L, 0, 200000000, 207, "18fa6d968fcc1c7f8e173be45492da816d7251a8401354d25c4f75f27c50ae99", "02dfb5187e88edab", 23058430050L, -3540343645446911906L, "dd7899249f0adf0d7d6f05055f7c6396a4a8a9bd1d189bd5e2eed647f8dfcc0b", "dd7899249f0adf0d7d6f05055f7c6396a4a8a9bd1d189bd5e2eed647f8dfcc0b", "4b415617a8d85f7fcac17d2e9a1628ebabf336285acdfcb8a4c4a7e2ba34fc0f0e54cd88d66aaa5f926bc02b49bc42b5ae52870ba4ac802b8276d1c264bec3f4", "3ab5313461e4b81c8b7af02d73861235a4e10a91a400b05ca01a3c1fdd83ca7e", BLOCK_2_GENERATOR, 1, Collections.emptyList());
        BLOCK_3 = buildBlock(BLOCK_3_ID, BLOCK_3_HEIGHT, 6, BLOCK_3_TIMESTAMP, -6746699668324916965L, 0, 0, 0, "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", "02dfb518ae37f5ac", 23058430050L, 2729391131122928659L, "facad4c1e0a7d407e0665393253eaf8e9f1e1e7b26e035687939897eaec9efe3", "facad4c1e0a7d407e0665393253eaf8e9f1e1e7b26e035687939897eaec9efe3", "f35393c0ff9721c84123075988a278cfdc596e2686772c4e6bd82751ecf06902a942f214c5afb56ea311a8d48dcdd2a44258ee03764c3e25ad1796f7d646185e", "1b613faf65e85ea257289156c62ec7d45684759ebceca59e46f8c94961b7a09e", BLOCK_3_GENERATOR, 4, Collections.emptyList());
        BLOCKS = Arrays.asList(GENESIS_BLOCK, BLOCK_0, BLOCK_1, BLOCK_2, BLOCK_3);
        LAST_BLOCK = BLOCKS.stream().max(Comparator.comparing(Block::getHeight)).get();
        NEW_BLOCK = buildBlock(-1603399584319711244L, LAST_BLOCK.getHeight() + 1, 3, LAST_BLOCK.getTimestamp() + 60, LAST_BLOCK.getId(), 0, 0, 0, "76a5fa85156953c4edaef2fa9718bcc355c7650a525401b556622d913662fe73", "460ea19d66a03d", 139844025, 0, "26d0567afcd911004d5e2beb6835a087859d3fc9ef838f2c437ebf3f8f8faf0e", "2c4937fd091efcb856dc20541c685ca483bca781419cfbc45a3d00eefbd0dd018ae99e030ce9f84ff483fba5855e108684da8bbc471938b6707edd488331d0f5", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", 983818929690189948L, 0);
    }

    private WalletKeysInfo createWalletKeysInfo(String passPhrase) {
        ApolloFbWallet apolloWallet = new ApolloFbWallet();
        apolloWallet.addAplKey(AccountGeneratorUtil.generateApl());
        apolloWallet.addEthKey(AccountHelper.generateNewEthAccount());

        WalletKeysInfo walletKeyInfo = new WalletKeysInfo(apolloWallet, null == passPhrase ? UUID.randomUUID().toString() : passPhrase);
        return walletKeyInfo;
    }

}