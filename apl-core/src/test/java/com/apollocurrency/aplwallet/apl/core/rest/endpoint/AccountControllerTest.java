package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.AccountDTO;
import com.apollocurrency.aplwallet.api.dto.Status2FA;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.account.model.PublicKey;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;
import com.apollocurrency.aplwallet.apl.core.app.TwoFactorAuthDetails;
import com.apollocurrency.aplwallet.apl.core.http.TwoFactorAuthParameters;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.rest.RestParametersParser;
import com.apollocurrency.aplwallet.apl.core.rest.converter.*;
import com.apollocurrency.aplwallet.apl.core.rest.service.AccountBalanceService;
import com.apollocurrency.aplwallet.apl.core.rest.utils.Account2FAHelper;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

import static com.apollocurrency.aplwallet.apl.data.BlockTestData.*;
import static org.jboss.resteasy.mock.MockHttpRequest.post;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountControllerTest extends AbstractEndpointTest{

    private static final String PASSPHRASE = "123456";
    public static final String PUBLIC_KEY_HEX = "e03f00485cabc82491d05297acd9d140f62d61d86f16ba4bcf2a922482a4617d";
    public static final String ACCOUNT_RS = "APL-5MRD-NBKX-X5EJ-3UP2M";
    public static final long ACCOUNT_ID = 1838236804542746347L;
    public static final String QR_CODE_URL = "https://url.google.com/qrcode";
    public static final String SECRET = "SuperSecret";
    public static final int CODE_2FA = 123456;

    public static final long ASSET_ID = 8180990979457659735L;
    public static final long CURRENCY_ID = 784842454721729391L;

    public Account account;
    public AccountDTO accountDTO;
    public AccountAsset accountAsset;
    public AccountCurrency accountCurrency;

    private AccountController endpoint;

    private AccountConverter accountConverter = mock(AccountConverter.class);
    private AccountService accountService = mock(AccountService.class);
    private AccountAssetService accountAssetService = mock(AccountAssetService.class);
    private AccountCurrencyService accountCurrencyService = mock(AccountCurrencyService.class);
    private AccountCurrencyConverter accountCurrencyConverter = mock(AccountCurrencyConverter.class);
    private AccountAssetConverter accountAssetConverter = mock(AccountAssetConverter.class);
    private TransactionConverter transactionConverter = new TransactionConverter(blockchain, new UnconfirmedTransactionConverter());
    private AccountBlockConverter accountBlockConverter = new AccountBlockConverter(blockchain, transactionConverter, mock(PhasingPollService.class));
    private AccountBalanceService accountBalanceService = mock(AccountBalanceService.class);

    private Account2FAHelper account2FAHelper = mock(Account2FAHelper.class);

    private Block GENESIS_BLOCK, LAST_BLOCK, NEW_BLOCK;
    private Block BLOCK_0, BLOCK_1, BLOCK_2, BLOCK_3, BLOCK_4, BLOCK_5, BLOCK_6, BLOCK_7, BLOCK_8, BLOCK_9, BLOCK_10, BLOCK_11, BLOCK_12, BLOCK_13;
    private List<Block> BLOCKS;

    @BeforeEach
    void setUp() {
        super.setUp();

        endpoint = new AccountController(
                blockchain,
                account2FAHelper,
                accountService,
                accountAssetService,
                accountCurrencyService,
                accountAssetConverter,
                accountCurrencyConverter,
                accountConverter,
                accountBlockConverter,
                new WalletKeysConverter(),
                new Account2FADetailsConverter(),
                new Account2FAConverter(),
                accountBalanceService
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

        doReturn(CURRENT_HEIGHT).when(accountService).getBlockchainHeight();

        setupBlocks();
    }

    private void setupBlocks(){
        GENESIS_BLOCK = buildBlock(GENESIS_BLOCK_ID     , GENESIS_BLOCK_HEIGHT  , -1, GENESIS_BLOCK_TIMESTAMP   , 0                    ,              0             , 0             ,0        , "0000000000000000000000000000000000000000000000000000000000000000","00"              , 5124095     , 8235640967557025109L   ,  "bc26bb638c9991f88fa52365591e00e22d3e9f9ad721ca4fe1683c8795a037e5",  "bc26bb638c9991f88fa52365591e00e22d3e9f9ad721ca4fe1683c8795a037e5"    , "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000", "0000000000000000000000000000000000000000000000000000000000000000", GENESIS_BLOCK_GENERATOR ,0, Collections.emptyList());
        BLOCK_0 =       buildBlock( BLOCK_0_ID          , BLOCK_0_HEIGHT        ,  3, BLOCK_0_TIMESTAMP         , 9108206803338182346L ,              0             , 100000000     , 1255    , "37f76b234414e64d33b71db739bd05d2cf3a1f7b344a88009b21c89143a00cd0","026543d9a8161629", 9331842     ,-1868632362992335764L   ,  "002bc5d6612e35e00e0a8141382eab45c20243d9dad4823348bfe85147b95acf",  "002bc5d6612e35e00e0a8141382eab45c20243d9dad4823348bfe85147b95acf"    , "e920b526c9200ae5e9757049b3b16fcb050b416587b167cb9d5ca0dc71ec970df48c37ce310b6d20b9972951e9844fa817f0ff14399d9e0f82fde807d0957c31", "cabec48dd4d9667e562234245d06098f3f51f8dc9881d1959496fd73d7266282", BLOCK_0_GENERATOR       ,0, Collections.emptyList());
        BLOCK_1 =       buildBlock( BLOCK_1_ID          , BLOCK_1_HEIGHT        ,  3, BLOCK_1_TIMESTAMP         , -3475222224033883190L,              0             , 100000000     , 1257    , "2cba9a6884de01ff23723887e565cbde21a3f5a0a70e276f3633645a97ed14c6","026601a7a1c313ca", 7069966     , 5841487969085496907L   ,  "fbf795ff1d4138f11ea3d38842aa319f8a21589eb46ea8cfc71850f8b55508ef",  "fbf795ff1d4138f11ea3d38842aa319f8a21589eb46ea8cfc71850f8b55508ef"    , "978b50eb629296b450f5298b61601685cbe965d4995b03707332fdc335a0e708a453bd7969bd9d336fbafcacd89073bf55c3b3395acf6dd0f3204c2a5d4b402e", "cadbeabccc87c5cf1cf7d2cf7782eb34a58fb2811c79e1d0a3cc60099557f4e0", BLOCK_1_GENERATOR       ,0, Collections.emptyList());
        BLOCK_2 =       buildBlock( BLOCK_2_ID          , BLOCK_2_HEIGHT        ,  5, BLOCK_2_TIMESTAMP         , 2069655134915376442L ,              0             , 200000000     , 207     , "18fa6d968fcc1c7f8e173be45492da816d7251a8401354d25c4f75f27c50ae99","02dfb5187e88edab", 23058430050L,-3540343645446911906L   ,  "dd7899249f0adf0d7d6f05055f7c6396a4a8a9bd1d189bd5e2eed647f8dfcc0b",  "dd7899249f0adf0d7d6f05055f7c6396a4a8a9bd1d189bd5e2eed647f8dfcc0b"   , "4b415617a8d85f7fcac17d2e9a1628ebabf336285acdfcb8a4c4a7e2ba34fc0f0e54cd88d66aaa5f926bc02b49bc42b5ae52870ba4ac802b8276d1c264bec3f4", "3ab5313461e4b81c8b7af02d73861235a4e10a91a400b05ca01a3c1fdd83ca7e", BLOCK_2_GENERATOR       ,1, Collections.emptyList());
        BLOCK_3 =       buildBlock( BLOCK_3_ID          , BLOCK_3_HEIGHT        ,  6, BLOCK_3_TIMESTAMP         , -6746699668324916965L,              0             , 0             , 0       , "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855","02dfb518ae37f5ac", 23058430050L, 2729391131122928659L   ,  "facad4c1e0a7d407e0665393253eaf8e9f1e1e7b26e035687939897eaec9efe3",  "facad4c1e0a7d407e0665393253eaf8e9f1e1e7b26e035687939897eaec9efe3"    , "f35393c0ff9721c84123075988a278cfdc596e2686772c4e6bd82751ecf06902a942f214c5afb56ea311a8d48dcdd2a44258ee03764c3e25ad1796f7d646185e", "1b613faf65e85ea257289156c62ec7d45684759ebceca59e46f8c94961b7a09e", BLOCK_3_GENERATOR       ,4, Collections.emptyList());
        BLOCK_4 =       buildBlock( BLOCK_4_ID          , BLOCK_4_HEIGHT        ,  6, BLOCK_4_TIMESTAMP         , -3540343645446911906L,              0             , 0             , 0       , "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855","02dfb518dde6fdad", 23058430050L, 1842732555539684628L   ,  "1fc63f083c3a49042c43c22a7e4d92aadac95c78d06ed21b8f9f0efd7c23b2a1",  "1fc63f083c3a49042c43c22a7e4d92aadac95c78d06ed21b8f9f0efd7c23b2a1"    , "8cb8795d7a320e693e64ea67b47348f2f1099c3e7163311b59135aaff1a78a00542b9d4713928c265666997a4ce63b0c07585ce04464f8dfb2253f21f91bf22e", "5e485346362cdece52dada076459abf88a0ae128cac6870e108257a88543f09f", BLOCK_4_GENERATOR       ,3, Collections.emptyList());
        BLOCK_5 =       buildBlock( BLOCK_5_ID          , BLOCK_5_HEIGHT        ,  4, BLOCK_5_TIMESTAMP         , 2729391131122928659L ,              0             , 200000000     , 207     , "9a8d7e4f2e83dc49351f9c3d72fabc5ecdc75f6eccc2b90f147ff5ec7d5068b2","02dfb5190d9605ae", 23058430050L,-5580266015477525080L   ,  "a042a2accbb2600530a4df46db4eba105ac73f4491923fb1c34a6b9dd2619634",  "a042a2accbb2600530a4df46db4eba105ac73f4491923fb1c34a6b9dd2619634"    , "ee4e2ccd12b36ade6318b47246ddcad237a153da36ab9ea2498373a4687c35072f2a9d49925520b588cb16d0e5663f3d10e3adeee97dcbbb4137470e521b347c", "130cafd7c5bee025885d0c6b58b2ddaaed71d2fa48423f552eb5828a423cc94b", BLOCK_5_GENERATOR       ,0, Collections.emptyList());
        BLOCK_6 =       buildBlock( BLOCK_6_ID          , BLOCK_6_HEIGHT        ,  6, BLOCK_6_TIMESTAMP         , 1842732555539684628L ,              0             , 0             ,0        , "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855","02dfb5193d450daf", 23058430050L, 6438949995368593549L   ,  "20feb26c8c34c22d55de747e3964acb3bc864326736949876d2b0594d15e87dd",  "20feb26c8c34c22d55de747e3964acb3bc864326736949876d2b0594d15e87dd"    , "a22f758567f0bd559ce2d821399da4f9ffdc4a694057d8b37045d2a9222be405f4311938e88a0b56418cbadcbea47dadabfc16e58f74e5dcd7a975d95dc17766", "149dfdfc7eb39219330d620a14fb0c2f02369abbda562bc4ab068e90c3cf11a4", BLOCK_6_GENERATOR       ,7, Collections.emptyList());
        BLOCK_7 =       buildBlock( BLOCK_7_ID          , BLOCK_7_HEIGHT        ,  4, BLOCK_7_TIMESTAMP         , -5580266015477525080L,              0             , 200000000     , 207     , "8bdf98fbc4cfcf0b66dfaa688ce7ef9063f8b1748ee238c23e8209f071cfcee7","02dfb5196cf415b0", 23058430050L, 7551185434952726924L   ,  "5b1bf463f202ec0d4ab42a9634976ed47b77c462d1de25e3fea3e8eaa8add8f6",  "5b1bf463f202ec0d4ab42a9634976ed47b77c462d1de25e3fea3e8eaa8add8f6"    , "992eacb8ac3bcbb7dbdbfcb637318adab190d4843b00da8961fd36ef60718f0f5acca4662cfdcf8447cc511d5e36ab4c321c185382f3577f0106c2bfb9f80ee6", "a81547db9fe98eb224d3cdc120f7305d3b829f162beb3bf719750e0cf48dbe9d", BLOCK_7_GENERATOR       ,0, Collections.emptyList());
        BLOCK_8 =       buildBlock( BLOCK_8_ID          , BLOCK_8_HEIGHT        ,  6, BLOCK_8_TIMESTAMP         , 6438949995368593549L ,              0             , 0             ,0        , "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855","02dfb5199ca31db1", 23058430050L, 8306616486060836520L   ,  "1435d4b603b52d04dd0f8228f36dbd6f01e627a59370fa3e6a0f58a75b372621",  "1435d4b603b52d04dd0f8228f36dbd6f01e627a59370fa3e6a0f58a75b372621"    , "5a8acc3cc947b76d42fa78938ed9ece33b91c5ca0bb7a1af6c92ec525e8bb6092babf03aee10bd965123fceb5afad63969e78991d8c6b2a6b4fc79cff8fe150d", "8db872e0e7be5b59fb68ef26d84bfeb9df04f6a5b6f701fd1c88578bfcf48a84", BLOCK_8_GENERATOR       ,6, Collections.emptyList());
        BLOCK_9  =      buildBlock( BLOCK_9_ID          , BLOCK_9_HEIGHT        ,  6, BLOCK_9_TIMESTAMP         , 7551185434952726924L ,              0             , 0             ,0        , "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855","02dfb519cc5225b2", 23058430050L,-6206981717632723220L   ,  "5f1382ab768b8b000637d8a59d7415fd8d4b6d4edc00ca0a08aacf9caf8c9a06",  "5f1382ab768b8b000637d8a59d7415fd8d4b6d4edc00ca0a08aacf9caf8c9a06"    , "6832f74bd4abe2a4b95755ff9e989133079e5215ae2111e590ea489353ce28078d094db3db077124ac541be9f4f7f09f5a36aac83c8c151dae0f09eb378033e1", "8cf9752b2533cb6849ad83b275c40f7e61b204ac023f775847a60c2f1a9d3d79", BLOCK_9_GENERATOR       ,9, Collections.emptyList());
        BLOCK_10 =      buildBlock( BLOCK_10_ID         , BLOCK_10_HEIGHT       ,  4, BLOCK_10_TIMESTAMP        , 8306616486060836520L ,              0             , 200000000     ,207      , "550dfe6da8732c1977c7545675f8dc163995aaba5533306b7a1f1b9364190dd3","02dfb519fc012db3", 23058430050L,-4166853316012435358L   ,  "df545469ed5a9405e0ff6efcdf468e61564776568c8b227f776f24c47206af46",  "df545469ed5a9405e0ff6efcdf468e61564776568c8b227f776f24c47206af46"    , "3d1c22000eb41599cb12dfbfaa3980353fa84cdf99145d1fcc92886551044a0c0b388c539efa48414c21251e493e468d97a2df12be24e9a33dec4521fdb6c2eb", "a8460f09af074773186c58688eb29215a81d5b0b10fc9e5fc5275b2f39fd93bb", BLOCK_10_GENERATOR      ,0, Collections.emptyList());
        BLOCK_11 =      buildBlock( BLOCK_11_ID         , BLOCK_11_HEIGHT       ,  6, BLOCK_11_TIMESTAMP        , -6206981717632723220L,              0             , 0             ,0        , "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855","02dfb51a2bb035b4", 23058430050L, 433871417191886464L    ,  "82e59d851fdf0d01ca1ee20df906009cd66885cc63e8314ebde80dc5e38987fa",  "82e59d851fdf0d01ca1ee20df906009cd66885cc63e8314ebde80dc5e38987fa"    , "202acda4d57f2a24212d265053241a07608de29a6dd8252994cf8be197765d02a585c676aca15e7f43a57d7747173d51435d9f2820da637ca8bc9cd1e536d761", "ec562889035fdca9d59d9bdca460992c01c5286278104287a989834eeffcb83e", BLOCK_11_GENERATOR      ,9, Collections.emptyList());
        BLOCK_12 =      buildBlock( BLOCK_12_ID         , BLOCK_12_HEIGHT       ,  5, BLOCK_12_TIMESTAMP        ,-3194395162061405253L ,              12000000000L  ,23000000000L   ,414      , "bb831a55863aabd3d2622a1692a4c03ba9eb14839902e029a702c58aeea6a935","3d46b0302ef95c"  , 7686143350L , BLOCK_13_ID                       ,  "d60150d67b47f37a90ca0b0c7a0151af1c2d9a69687f3eef75f42d7b5f12c191",  "d60150d67b47f37a90ca0b0c7a0151af1c2d9a69687f3eef75f42d7b5f12c191"    , "d2c6b60abaf85e17f65f339879fda8de5346415908a9cbb9a21b3c6d24bd1d0454222fb8962ad2aec679da0d8fb7e835b76a35301c33e925b48245a9d24954de", "4555a1d9a7c2226b9a5797e56d245485cb94fdb2495fc8ca31c3297e597c7b68", BLOCK_12_GENERATOR      ,2, Collections.emptyList());
        BLOCK_13 =      buildBlock(	BLOCK_13_ID         , BLOCK_13_HEIGHT       ,  3, BLOCK_13_TIMESTAMP        ,-420771891665807004L  ,              0             ,1000000000	    ,2668     , "6459caa1311e29fa9c60bed5752f161a5e82b77328cac949cb7afbaccacfbb8e","3de7206ceaebce"	 , 168574215	 ,0                       ,  "dc3b7c24f1e6caba84e39ff7b8f4040be4c614b16b7e697364cedecdd072b6df",  "dc3b7c24f1e6caba84e39ff7b8f4040be4c614b16b7e697364cedecdd072b6df"    , "866847568d2518e1c1c6f97ee014b6f15e4197e5ff9041ab449d9087aba343060e746dc56dbc34966d42f6fd326dc5c4b741ae330bd5fa56539022bd75643cd6", "cf8dc4e015626b309ca7518a390e3e1e7b058a83428287ff39dc49b1518df50c", BLOCK_13_GENERATOR   ,0, Collections.emptyList());
        BLOCKS = Arrays.asList(GENESIS_BLOCK, BLOCK_0, BLOCK_1, BLOCK_2, BLOCK_3, BLOCK_4, BLOCK_5, BLOCK_6, BLOCK_7, BLOCK_8, BLOCK_9, BLOCK_10, BLOCK_11, BLOCK_12, BLOCK_13);
        LAST_BLOCK = BLOCKS.stream().max(Comparator.comparing(Block::getHeight)).get();
        NEW_BLOCK = buildBlock(-1603399584319711244L, LAST_BLOCK.getHeight() + 1, 3, LAST_BLOCK.getTimestamp() + 60, LAST_BLOCK.getId(), 0, 0, 0, "76a5fa85156953c4edaef2fa9718bcc355c7650a525401b556622d913662fe73", "460ea19d66a03d",	139844025, 0, "26d0567afcd911004d5e2beb6835a087859d3fc9ef838f2c437ebf3f8f8faf0e",	"2c4937fd091efcb856dc20541c685ca483bca781419cfbc45a3d00eefbd0dd018ae99e030ce9f84ff483fba5855e108684da8bbc471938b6707edd488331d0f5", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", 983818929690189948L, 0);
    }

    @AfterEach
    void tearDown() {

    }

    @Test
    void getAccount_whenCallWithoutMandatoryParameter_thenGetError_2003() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/account");

        checkMandatoryParameterMissingErrorCode(response, 2003);

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
        MockHttpResponse response = sendGetRequest("/accounts/account?account="+ACCOUNT_ID);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:"+result.get("newErrorCode"));
        assertEquals(Long.toUnsignedString(ACCOUNT_ID), result.get("account"));
        assertEquals(ACCOUNT_RS, result.get("accountRS"));
    }

    @Test
    void createAccount_whenCallWithoutMandatoryParameter_thenGetError_2003() throws URISyntaxException, IOException {
        MockHttpResponse response = sendPostRequest("/accounts/account", "wrong=param");

        checkMandatoryParameterMissingErrorCode(response, 2003);
    }

    @Test
    void enable2FA_withoutMandatoryParameters_thenGetError_2002() throws URISyntaxException, IOException {
        when(account2FAHelper.parse2FARequestParams(null, null,null)).thenCallRealMethod();
        MockHttpResponse response = sendPostRequest("/accounts/enable2FA", "wrong=value");

        checkMandatoryParameterMissingErrorCode(response, 2002);
    }

    @Test
    void enable2FA_withBothSecretPhraseAndPassPhrase_thenGetError_2010() throws URISyntaxException, IOException {
        when(account2FAHelper.parse2FARequestParams(null, PASSPHRASE, SECRET)).thenCallRealMethod();
        MockHttpResponse response = sendPostRequest("/accounts/enable2FA", "passphrase="+PASSPHRASE+"&secretPhrase="+SECRET);

        checkMandatoryParameterMissingErrorCode(response, 2010);
    }

    @Test
    void enable2FA_withoutMandatoryParameter_Account_thenGetError_2003() throws URISyntaxException, IOException {
        when(account2FAHelper.parse2FARequestParams(null, PASSPHRASE, null)).thenCallRealMethod();
        MockHttpResponse response = sendPostRequest("/accounts/enable2FA", "passphrase="+PASSPHRASE);

        checkMandatoryParameterMissingErrorCode(response, 2003);
    }

    @Test
    void enable2FA_withPathPhraseAndAccount() throws URISyntaxException, IOException {
        TwoFactorAuthParameters params2FA = new TwoFactorAuthParameters(ACCOUNT_ID, PASSPHRASE, null);
        TwoFactorAuthDetails authDetails = new TwoFactorAuthDetails(QR_CODE_URL, SECRET, Status2FA.OK);

        doReturn(params2FA).when(account2FAHelper).parse2FARequestParams(ACCOUNT_RS, PASSPHRASE, null);
        doReturn(authDetails).when(account2FAHelper).enable2FA(params2FA);
        MockHttpResponse response = sendPostRequest("/accounts/enable2FA", "passphrase="+PASSPHRASE+"&account="+ACCOUNT_RS);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:"+result.get("newErrorCode"));

        assertEquals(Long.toUnsignedString(ACCOUNT_ID), result.get("account"));
        assertEquals(ACCOUNT_RS, result.get("accountRS"));
        assertEquals(QR_CODE_URL, result.get("qrCodeUrl"));
        assertEquals(SECRET, result.get("secret"));
    }

    @Test
    void enable2FA_withSecretPhrase() throws URISyntaxException, IOException {
        TwoFactorAuthParameters params2FA = new TwoFactorAuthParameters(ACCOUNT_ID, null, SECRET);
        TwoFactorAuthDetails authDetails = new TwoFactorAuthDetails(QR_CODE_URL, SECRET, Status2FA.OK);

        doReturn(params2FA).when(account2FAHelper).parse2FARequestParams(null, null, SECRET);
        doReturn(authDetails).when(account2FAHelper).enable2FA(params2FA);
        MockHttpResponse response = sendPostRequest("/accounts/enable2FA", "secretPhrase="+SECRET);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:"+result.get("newErrorCode"));

        assertEquals(Long.toUnsignedString(ACCOUNT_ID), result.get("account"));
        assertEquals(ACCOUNT_RS, result.get("accountRS"));
        assertEquals(QR_CODE_URL, result.get("qrCodeUrl"));
        assertEquals(SECRET, result.get("secret"));
    }

    @Test
    void disable2FA_withoutRequestAttribute_thenGetError_1000() throws URISyntaxException, IOException {
        check2FA_withoutRequestAttribute_thenGetError_1000("/accounts/disable2FA");
    }

    @Test
    void disable2FA_withoutMandatoryParameters_thenGetError_2002() throws URISyntaxException, IOException {
        check2FA_withoutMandatoryParameters_thenGetError_2002("/accounts/disable2FA");
    }

    @Test
    void disable2FA_withBothSecretPhraseAndPassPhrase_thenGetError_2010() throws URISyntaxException, IOException {
        check2FA_withBothSecretPhraseAndPassPhrase_thenGetError_2010("/accounts/disable2FA");
    }

    @Test
    void disable2FA_withoutMandatoryParameter_Code2FA_thenGetError_2003() throws URISyntaxException, IOException {
        check2FA_withoutMandatoryParameter_Code2FA_thenGetError_2003("/accounts/disable2FA");
    }

    @Test
    void disable2FA_withPathPhraseAndAccountAndCode2FA() throws URISyntaxException, IOException {
        String uri = "/accounts/disable2FA";
        TwoFactorAuthParameters twoFactorAuthParameters = new TwoFactorAuthParameters(ACCOUNT_ID, PASSPHRASE, null);
        twoFactorAuthParameters.setCode2FA(CODE_2FA);


        doReturn(Status2FA.OK).when(account2FAHelper).disable2FA(twoFactorAuthParameters, CODE_2FA);
        check2FA_withPathPhraseAndAccountAndCode2FA(uri, twoFactorAuthParameters);
    }

    @Test
    void disable2FA_withSecretPhraseAndCode2FA() throws URISyntaxException, IOException {
        String uri = "/accounts/disable2FA";
        TwoFactorAuthParameters twoFactorAuthParameters = new TwoFactorAuthParameters(ACCOUNT_ID, null, SECRET);
        twoFactorAuthParameters.setCode2FA(CODE_2FA);

        doReturn(Status2FA.OK).when(account2FAHelper).disable2FA(twoFactorAuthParameters, CODE_2FA);
        check2FA_withSecretPhraseAndCode2FA(uri, twoFactorAuthParameters);
    }

    @Test
    void confirm2FA_withoutRequestAttribute_thenGetError_1000() throws URISyntaxException, IOException {
        check2FA_withoutRequestAttribute_thenGetError_1000("/accounts/confirm2FA");
    }

    @Test
    void confirm2FA_withoutMandatoryParameters_thenGetError_2002() throws URISyntaxException, IOException {
        check2FA_withoutMandatoryParameters_thenGetError_2002("/accounts/confirm2FA");
    }

    @Test
    void confirm2FA_withBothSecretPhraseAndPassPhrase_thenGetError_2010() throws URISyntaxException, IOException {
        check2FA_withBothSecretPhraseAndPassPhrase_thenGetError_2010("/accounts/confirm2FA");
    }

    @Test
    void confirm2FA_withoutMandatoryParameter_Code2FA_thenGetError_2003() throws URISyntaxException, IOException {
        check2FA_withoutMandatoryParameter_Code2FA_thenGetError_2003("/accounts/confirm2FA");
    }

    @Test
    void confirm2FA_withPathPhraseAndAccountAndCode2FA() throws URISyntaxException, IOException {
        String uri = "/accounts/confirm2FA";
        TwoFactorAuthParameters twoFactorAuthParameters = new TwoFactorAuthParameters(ACCOUNT_ID, PASSPHRASE, null);
        twoFactorAuthParameters.setCode2FA(CODE_2FA);


        doReturn(Status2FA.OK).when(account2FAHelper).confirm2FA(twoFactorAuthParameters, CODE_2FA);
        check2FA_withPathPhraseAndAccountAndCode2FA(uri, twoFactorAuthParameters);
    }

    @Test
    void confirm2FA_withSecretPhraseAndCode2FA() throws URISyntaxException, IOException {
        String uri = "/accounts/confirm2FA";
        TwoFactorAuthParameters twoFactorAuthParameters = new TwoFactorAuthParameters(ACCOUNT_ID, null, SECRET);
        twoFactorAuthParameters.setCode2FA(CODE_2FA);

        doReturn(Status2FA.OK).when(account2FAHelper).confirm2FA(twoFactorAuthParameters, CODE_2FA);
        check2FA_withSecretPhraseAndCode2FA(uri, twoFactorAuthParameters);
    }

    @Test
    void deleteKey_withoutRequestAttribute_thenGetError_1000() throws URISyntaxException, IOException {
        check2FA_withoutRequestAttribute_thenGetError_1000("/accounts/deleteKey");
    }

    @Test
    void deleteKey_withoutMandatoryParameters_thenGetError_2002() throws URISyntaxException, IOException {
        check2FA_withoutMandatoryParameters_thenGetError_2002("/accounts/deleteKey");
    }

    @Test
    void deleteKey_withoutMandatoryParameter_Code2FA_thenGetError_2003() throws URISyntaxException, IOException {
        check2FA_withoutMandatoryParameter_Code2FA_thenGetError_2003("/accounts/deleteKey");
    }

    @Test
    void deleteKey_withPathPhraseAndAccountAndCode2FA() throws URISyntaxException, IOException {
        String uri = "/accounts/deleteKey";
        TwoFactorAuthParameters twoFactorAuthParameters = new TwoFactorAuthParameters(ACCOUNT_ID, PASSPHRASE, null);
        twoFactorAuthParameters.setCode2FA(CODE_2FA);

        doReturn(KeyStoreService.Status.OK).when(account2FAHelper).deleteAccount(twoFactorAuthParameters);
        check2FA_withPathPhraseAndAccountAndCode2FA(uri, twoFactorAuthParameters);
    }

    @Test
    void deleteKey_withSecretPhraseAndCode2FA() throws URISyntaxException, IOException {
        String uri = "/accounts/deleteKey";
        TwoFactorAuthParameters twoFactorAuthParameters = new TwoFactorAuthParameters(ACCOUNT_ID, null, SECRET);
        twoFactorAuthParameters.setCode2FA(CODE_2FA);

        doReturn(KeyStoreService.Status.OK).when(account2FAHelper).deleteAccount(twoFactorAuthParameters);
        check2FA_withSecretPhraseAndCode2FA(uri, twoFactorAuthParameters);
    }

    @Test
    void exportKey_withoutMandatoryParameters_thenGetError_2002() throws URISyntaxException, IOException {
        doCallRealMethod().when(account2FAHelper).parse2FARequestParams(null, null,null);

        MockHttpResponse response = sendPostRequest("/accounts/exportKey","wrong=value");

        checkMandatoryParameterMissingErrorCode(response, 2002);
    }

    @Test
    void exportKey_withPathPhraseAndAccountAndCode2FA() throws URISyntaxException, IOException {
        String uri = "/accounts/exportKey";
        byte[] secretBytes = SECRET.getBytes();
        TwoFactorAuthParameters twoFactorAuthParameters = new TwoFactorAuthParameters(ACCOUNT_ID, PASSPHRASE, null);
        twoFactorAuthParameters.setCode2FA(CODE_2FA);
        doReturn(twoFactorAuthParameters).when(account2FAHelper).parse2FARequestParams(ACCOUNT_RS, PASSPHRASE, null);


        doReturn(secretBytes).when(account2FAHelper).findAplSecretBytes(twoFactorAuthParameters);
        check2FA_withPathPhraseAndAccountAndCode2FA(uri, twoFactorAuthParameters);
    }

    @Test
    void getAccountAssetCount_whenCallWithoutMandatoryParameter_thenGetError_2003() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/account/assetCount");

        checkMandatoryParameterMissingErrorCode(response, 2003);
    }

    @Test
    void getAccountAssetCount_whenCallwithWrongHeight_thenGetError_2004() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/account/assetCount?account="+ACCOUNT_ID+"&height="+(CURRENT_HEIGHT+10));

        checkMandatoryParameterMissingErrorCode(response, 2004);
    }

    @Test
    void getAccountAssetCount() throws URISyntaxException, IOException {
        int count = 38;

        doReturn(count).when(accountAssetService).getAccountAssetCount(ACCOUNT_ID, CURRENT_HEIGHT);
        MockHttpResponse response = sendGetRequest("/accounts/account/assetCount?account="+ACCOUNT_ID+"&height="+CURRENT_HEIGHT);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:"+result.get("newErrorCode"));

        assertEquals(count, result.get("numberOfAssets"));
    }

    @Test
    void getAccountAssets_whenCallWithoutMandatoryParameter_thenGetError_2003() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/account/assets");

        checkMandatoryParameterMissingErrorCode(response, 2003);
    }

    @Test
    void getAccountAssets_whenCallwithWrongHeight_thenGetError_2004() throws URISyntaxException, IOException {
        doReturn(CURRENT_HEIGHT).when(accountService).getBlockchainHeight();
        MockHttpResponse response = sendGetRequest("/accounts/account/assets?account="+ACCOUNT_ID+"&height="+(CURRENT_HEIGHT+10));

        checkMandatoryParameterMissingErrorCode(response, 2004);
    }

    @Test
    void getAccountAssets_whenCallwithWrongAsset_thenGetError_InternalError_1000() throws URISyntaxException, IOException {
        doReturn(CURRENT_HEIGHT).when(accountService).getBlockchainHeight();
        MockHttpResponse response = sendGetRequest("/accounts/account/assets?account="+ACCOUNT_ID+"&asset=AS123&height="+(CURRENT_HEIGHT+10));

        checkMandatoryParameterMissingErrorCode(response, 1000);
    }

    @Test
    void getAccountAssets_getList() throws URISyntaxException, IOException {
        endpoint.setAccountAssetConverter(new AccountAssetConverter());
        doReturn(List.of(accountAsset)).when(accountAssetService).getAssetAccounts(ACCOUNT_ID, CURRENT_HEIGHT, 0, -1);

        MockHttpResponse response = sendGetRequest("/accounts/account/assets?account="+ACCOUNT_ID+"&height="+CURRENT_HEIGHT);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);

        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:"+result.get("newErrorCode"));

        JsonNode root = mapper.readTree(content);
        assertTrue(root.get("accountAssets").isArray());
        assertEquals(Long.toUnsignedString(ASSET_ID), root.withArray("accountAssets").get(0).get("asset").asText());
    }

    @Test
    void getAccountAssets_getOne() throws URISyntaxException, IOException {
        endpoint.setAccountAssetConverter(new AccountAssetConverter());
        doReturn(accountAsset).when(accountAssetService).getAsset(ACCOUNT_ID, ASSET_ID, CURRENT_HEIGHT);

        MockHttpResponse response = sendGetRequest(
                "/accounts/account/assets?account="+ACCOUNT_ID+"&asset="+ASSET_ID+"&height="+CURRENT_HEIGHT);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);

        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:"+result.get("newErrorCode"));
        assertEquals(Long.toUnsignedString(ASSET_ID), result.get("asset"));
    }

    @Test
    void getAccountCurrencyCount_whenCallWithoutMandatoryParameter_thenGetError_2003() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/account/currencyCount");

        checkMandatoryParameterMissingErrorCode(response, 2003);
    }

    @Test
    void getAccountCurrencyCount_whenCallwithWrongHeight_thenGetError_2004() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/account/currencyCount?account="+ACCOUNT_ID+"&height="+(CURRENT_HEIGHT+10));

        checkMandatoryParameterMissingErrorCode(response, 2004);
    }

    @Test
    void getAccountCurrencyCount() throws URISyntaxException, IOException {
        int count = 58;

        doReturn(count).when(accountCurrencyService).getAccountCurrencyCount(ACCOUNT_ID, CURRENT_HEIGHT);
        MockHttpResponse response = sendGetRequest("/accounts/account/currencyCount?account="+ACCOUNT_ID+"&height="+CURRENT_HEIGHT);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:"+result.get("newErrorCode"));

        assertEquals(count, result.get("numberOfCurrencies"));
    }

    @Test
    void getAccountCurrencies_whenCallWithoutMandatoryParameter_thenGetError_2003() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/account/currencies");

        checkMandatoryParameterMissingErrorCode(response, 2003);
    }

    @Test
    void getAccountCurrencies_whenCallwithWrongHeight_thenGetError_2004() throws URISyntaxException, IOException {
        doReturn(CURRENT_HEIGHT).when(accountService).getBlockchainHeight();
        MockHttpResponse response = sendGetRequest("/accounts/account/currencies?account="+ACCOUNT_ID+"&height="+(CURRENT_HEIGHT+10));

        checkMandatoryParameterMissingErrorCode(response, 2004);
    }

    @Test
    void getAccountCurrencies_whenCallwithWrongCurrencyId_thenGetError_InternalError_1000() throws URISyntaxException, IOException {
        doReturn(CURRENT_HEIGHT).when(accountService).getBlockchainHeight();
        MockHttpResponse response = sendGetRequest("/accounts/account/currencies?account="+ACCOUNT_ID+"&currency=AS123&height="+(CURRENT_HEIGHT+10));

        checkMandatoryParameterMissingErrorCode(response, 1000);
    }

    @Test
    void getAccountCurrencies_getList() throws URISyntaxException, IOException {
        endpoint.setAccountCurrencyConverter(new AccountCurrencyConverter());
        doReturn(List.of(accountCurrency)).when(accountCurrencyService).getCurrencies(ACCOUNT_ID, CURRENT_HEIGHT, 0, -1);

        MockHttpResponse response = sendGetRequest("/accounts/account/currencies?account="+ACCOUNT_ID+"&height="+CURRENT_HEIGHT);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);

        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:"+result.get("newErrorCode"));

        JsonNode root = mapper.readTree(content);
        assertTrue(root.get("accountCurrencies").isArray());
        assertEquals(Long.toUnsignedString(CURRENCY_ID), root.withArray("accountCurrencies").get(0).get("currency").asText());
    }

    @Test
    void getAccountCurrencies_getOne_byCurrencyId() throws URISyntaxException, IOException {
        endpoint.setAccountCurrencyConverter(new AccountCurrencyConverter());
        doReturn(accountCurrency).when(accountCurrencyService).getAccountCurrency(ACCOUNT_ID, CURRENCY_ID, CURRENT_HEIGHT);

        MockHttpResponse response = sendGetRequest(
                "/accounts/account/currencies?account="+ACCOUNT_ID+"&currency="+CURRENCY_ID+"&height="+CURRENT_HEIGHT);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);

        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:"+result.get("newErrorCode"));
        assertEquals(Long.toUnsignedString(CURRENCY_ID), result.get("currency"));
    }

    @Test
    void getAccountBlockCount_whenCallWithoutMandatoryParameter_thenGetError_2003() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/account/blockCount");

        checkMandatoryParameterMissingErrorCode(response, 2003);
    }

    @Test
    void getAccountBlockCount_whenCallwithWrongAccount_thenGetError_2005() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/account/blockCount?account=0");

        checkMandatoryParameterMissingErrorCode(response, 2005);
    }

    @Test
    void getAccountBlockCount() throws URISyntaxException, IOException {
        int count = 10000;
        doReturn(count).when(blockchain).getBlockCount(ACCOUNT_ID);
        MockHttpResponse response = sendGetRequest("/accounts/account/blockCount?account="+ACCOUNT_ID);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:"+result.get("newErrorCode"));

        assertEquals(count, result.get("numberOfBlocks"));
    }

    @Test
    void getAccountBlockIds_whenCallWithoutMandatoryParameter_thenGetError_2003() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/account/blockIds");

        checkMandatoryParameterMissingErrorCode(response, 2003);
    }

    @Test
    void getAccountBlockIds_whenCallwithWrongAccount_thenGetError_2005() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/account/blockIds?account=0");

        checkMandatoryParameterMissingErrorCode(response, 2005);
    }

    @Test
    void getAccountBlockIds() throws URISyntaxException, IOException {
        int timestamp = (int) (System.currentTimeMillis()/1000);
        int from = 0;
        int to = 100;

        doReturn(BLOCKS).when(accountService).getAccountBlocks(ACCOUNT_ID, timestamp, from, to);

        MockHttpResponse response = sendGetRequest("/accounts/account/blockIds?account="+ACCOUNT_ID
                +"&timestamp="+timestamp
                +"&firstIndex="+from
                +"&lastIndex="+to
        );

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:"+result.get("newErrorCode"));

        JsonNode root = mapper.readTree(content);
        assertTrue(root.get("blockIds").isArray());
        assertEquals(BLOCKS.size(), root.withArray("blockIds").size());

    }

    @Test
    void getAccountBlocks_whenCallWithoutMandatoryParameter_thenGetError_2003() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/account/blocks");

        checkMandatoryParameterMissingErrorCode(response, 2003);
    }

    @Test
    void getAccountBlocks_whenCallwithWrongAccount_thenGetError_2005() throws URISyntaxException, IOException {
        MockHttpResponse response = sendGetRequest("/accounts/account/blocks?account=0");

        checkMandatoryParameterMissingErrorCode(response, 2005);
    }

    @Test
    void getAccountBlocks() throws URISyntaxException, IOException {
        int timestamp = (int) (System.currentTimeMillis()/1000);
        int from = 0;
        int to = 100;

        doReturn(BLOCKS).when(accountService).getAccountBlocks(ACCOUNT_ID, timestamp, from, to);

        MockHttpResponse response = sendGetRequest("/accounts/account/blocks?account="+ACCOUNT_ID
                +"&timestamp="+timestamp
                +"&firstIndex="+from
                +"&lastIndex="+to
        );

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:"+result.get("newErrorCode"));

        JsonNode root = mapper.readTree(content);
        assertTrue(root.get("blocks").isArray());
        assertEquals(BLOCKS.size(), root.withArray("blocks").size());
        assertEquals(Long.toUnsignedString(BLOCK_0.getGeneratorId()), root.withArray("blocks").get(1).get("generator").asText());
    }

    private void check2FA_withoutRequestAttribute_thenGetError_1000(String uri) throws URISyntaxException, IOException {
        MockHttpResponse response = sendPostRequest(uri,"wrong=value");

        checkMandatoryParameterMissingErrorCode(response, 1000);
    }

    private void check2FA_withoutMandatoryParameters_thenGetError_2002(String uri) throws URISyntaxException, IOException {
        TwoFactorAuthParameters twoFactorAuthParameters = new TwoFactorAuthParameters(0L, null, null);
        twoFactorAuthParameters.setCode2FA(CODE_2FA);
        doCallRealMethod().when(account2FAHelper).verify2FA(null, null,null, CODE_2FA);
        doCallRealMethod().when(account2FAHelper).validate2FAParameters(twoFactorAuthParameters);
        doCallRealMethod().when(account2FAHelper).parse2FARequestParams("0", null, null);

        MockHttpRequest request = post(uri);
        request.setAttribute(RestParametersParser.TWO_FCTOR_AUTH_ATTRIBUTE, twoFactorAuthParameters );

        MockHttpResponse response = sendPostRequest(request,"wrong=value");

        checkMandatoryParameterMissingErrorCode(response, 2002);
    }

    private void check2FA_withBothSecretPhraseAndPassPhrase_thenGetError_2010(String uri) throws URISyntaxException, IOException {
        TwoFactorAuthParameters twoFactorAuthParameters = new TwoFactorAuthParameters(0L, PASSPHRASE, SECRET);
        twoFactorAuthParameters.setCode2FA(CODE_2FA);

        doCallRealMethod().when(account2FAHelper).validate2FAParameters(twoFactorAuthParameters);
        doCallRealMethod().when(account2FAHelper).parse2FARequestParams("0", PASSPHRASE, SECRET);

        MockHttpRequest request = post(uri);
        request.setAttribute(RestParametersParser.TWO_FCTOR_AUTH_ATTRIBUTE, twoFactorAuthParameters );

        MockHttpResponse response = sendPostRequest(request,"passphrase="+PASSPHRASE+"&secretPhrase="+SECRET+"&code2FA="+CODE_2FA);

        checkMandatoryParameterMissingErrorCode(response, 2010);
    }

    private void check2FA_withoutMandatoryParameter_Code2FA_thenGetError_2003(String uri) throws URISyntaxException, IOException {
        TwoFactorAuthParameters twoFactorAuthParameters = new TwoFactorAuthParameters(0L, null, SECRET);
        doCallRealMethod().when(account2FAHelper).validate2FAParameters(twoFactorAuthParameters);

        MockHttpRequest request = post(uri);
        request.setAttribute(RestParametersParser.TWO_FCTOR_AUTH_ATTRIBUTE, twoFactorAuthParameters );

        MockHttpResponse response = sendPostRequest(request,"secretPhrase="+SECRET);

        checkMandatoryParameterMissingErrorCode(response, 2003);
    }

    private void check2FA_withPathPhraseAndAccountAndCode2FA(String uri, TwoFactorAuthParameters twoFactorAuthParameters) throws URISyntaxException, IOException {
        doCallRealMethod().when(account2FAHelper).validate2FAParameters(twoFactorAuthParameters);

        MockHttpRequest request = post(uri);
        request.setAttribute(RestParametersParser.TWO_FCTOR_AUTH_ATTRIBUTE, twoFactorAuthParameters );
        MockHttpResponse response = sendPostRequest(request, "passphrase="+PASSPHRASE+"&account="+ACCOUNT_RS+"&code2FA="+CODE_2FA);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:"+result.get("newErrorCode"));

        assertEquals(Long.toUnsignedString(ACCOUNT_ID), result.get("account"));
        assertEquals(ACCOUNT_RS, result.get("accountRS"));
    }

    private void check2FA_withSecretPhraseAndCode2FA(String uri, TwoFactorAuthParameters twoFactorAuthParameters) throws URISyntaxException, IOException {
        doCallRealMethod().when(account2FAHelper).validate2FAParameters(twoFactorAuthParameters);

        MockHttpRequest request = post(uri);
        request.setAttribute(RestParametersParser.TWO_FCTOR_AUTH_ATTRIBUTE, twoFactorAuthParameters );

        MockHttpResponse response = sendPostRequest(request,"secretPhrase="+SECRET+"&code2FA="+CODE_2FA);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String content = response.getContentAsString();
        print(content);
        Map result = mapper.readValue(content, Map.class);
        assertFalse(result.containsKey("newErrorCode"), "Unexpected error code:"+result.get("newErrorCode"));

        assertEquals(Long.toUnsignedString(ACCOUNT_ID), result.get("account"));
        assertEquals(ACCOUNT_RS, result.get("accountRS"));
    }

}