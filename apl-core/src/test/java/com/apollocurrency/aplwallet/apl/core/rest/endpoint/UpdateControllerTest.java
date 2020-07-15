package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.EcBlockData;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.core.http.ElGamalEncryptor;
import com.apollocurrency.aplwallet.apl.core.model.ApolloFbWallet;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.rest.exception.LegacyParameterExceptionMapper;
import com.apollocurrency.aplwallet.apl.core.rest.provider.ByteArrayConverterProvider;
import com.apollocurrency.aplwallet.apl.core.rest.provider.PlatformSpecConverterProvider;
import com.apollocurrency.aplwallet.apl.core.rest.utils.AccountParametersParser;
import com.apollocurrency.aplwallet.apl.core.service.appdata.KeyStoreService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.FeeCalculator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.update.CertificateMemoryStore;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.update.UpdateV2Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.update.UpdateV2Transaction;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.udpater.intfce.Level;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.util.env.Arch;
import com.apollocurrency.aplwallet.apl.util.env.OS;
import com.apollocurrency.aplwallet.apl.util.env.PlatformSpec;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@EnableWeld
    // enable weld only to have an ability of creation Transaction since it require dynamic CDI injection TODO should be removed after refactoring
class UpdateControllerTest extends AbstractEndpointTest {
    TransactionCreator transactionCreator;
    @Mock
    HttpServletRequest req;
    @Mock
    TransactionValidator validator;
    @Mock
    TimeService timeService;
    @Mock
    TransactionProcessor processor;
    @Mock
    ElGamalEncryptor elGamal = mock(ElGamalEncryptor.class);
    @Mock
    AccountService accountService = mock(AccountService.class);
    @Mock
    KeyStoreService keystoreService = mock(KeyStoreService.class);
    @Mock
    BlockchainConfig blockchainConfig;
    UpdateV2Transaction v2Transaction = new UpdateV2Transaction(mock(CertificateMemoryStore.class));

    UpdateV2Attachment attachment = new UpdateV2Attachment("https://test.com", Level.CRITICAL, new Version("1.23.4"), "https://con.com", BigInteger.ONE, Convert.parseHexString("111100ff"), Set.of(new PlatformSpec(OS.WINDOWS, Arch.X86_64), new PlatformSpec(OS.NO_OS, Arch.ARM_64)));
    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from().addBeans(MockBean.of(v2Transaction, UpdateV2Transaction.class), MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class)).build();

    @Override
    @BeforeEach
    void setUp() {
        super.setUp();
        transactionCreator = new TransactionCreator(validator, new PropertiesHolder(), timeService, new FeeCalculator(blockchainConfig), blockchain, processor);
        dispatcher.getProviderFactory()
            .register(ByteArrayConverterProvider.class)
            .register(LegacyParameterExceptionMapper.class)
            .register(PlatformSpecConverterProvider.class);
        UpdateController updateController = new UpdateController(new AccountParametersParser(accountService, blockchain, keystoreService, elGamal), transactionCreator);
        dispatcher.getRegistry().addSingletonResource(updateController);
        dispatcher.getDefaultContextObjects().put(HttpServletRequest.class, req);
    }

    @Test
    void testSendUpdateSuccessful() throws URISyntaxException, UnsupportedEncodingException, AplException.ValidationException {
        when(elGamal.elGamalDecrypt(SECRET)).thenReturn(SECRET);
        Account sender = new Account(ACCOUNT_ID_WITH_SECRET, 10000 * Constants.ONE_APL, 10000 * Constants.ONE_APL, 0, 0, CURRENT_HEIGHT);
        sender.setPublicKey(new PublicKey(sender.getId(), null, 0));
        doReturn(sender).when(accountService).getAccount(Convert.parseHexString(PUBLIC_KEY_SECRET));
        EcBlockData ecBlockData = new EcBlockData(121, 100_000);
        doReturn(ecBlockData).when(blockchain).getECBlock(0);
        doAnswer(invocation -> {
            String argument = invocation.getArgument(0);
            if ("secretPhrase".equals(argument)) {
                return SECRET;
            }
            return "";
        }).when(req).getParameter(anyString());

        MockHttpResponse response = sendPostRequest("/updates", "secretPhrase=" + SECRET + "&manifestUrl=https://test.com&level=CRITICAL&platformSpec=WINDOWS-X86_32,NoOS-ARM" +
            "&version=1.23.4&cn=https://cn.com&serialNumber=1&signature=111100ff");
        String json = response.getContentAsString();

        JSONAssert.assertEquals("{\"timestamp\": 0, \"attachment\" : {\"level\": 0, \"manifestUrl\":\"https://test.com\", \"platforms\": [{\"platform\": 1, \"architecture\": 0},{\"platform\": -1, \"architecture\": 3}]}, \"type\" : 8, \"subtype\": 3}", json, JSONCompareMode.LENIENT);
        verify(processor).broadcast(any(Transaction.class));
    }

    @Test
    void testSendUpdateSuccessful_usingVault() throws URISyntaxException, UnsupportedEncodingException, AplException.ValidationException {
        when(elGamal.elGamalDecrypt(SECRET)).thenReturn(SECRET);
        Account sender = new Account(ACCOUNT_ID_WITH_SECRET, 10000 * Constants.ONE_APL, 10000 * Constants.ONE_APL, 0, 0, CURRENT_HEIGHT);
        sender.setPublicKey(new PublicKey(sender.getId(), null, 0));
        ApolloFbWallet wallet = mock(ApolloFbWallet.class);
        doReturn(Convert.toHexString(Crypto.getKeySeed(Convert.toBytes(SECRET)))).when(wallet).getAplKeySecret();
        doReturn(wallet).when(keystoreService).getSecretStore(SECRET, ACCOUNT_ID_WITH_SECRET);
        doReturn(sender).when(accountService).getAccount(Convert.parseHexString(PUBLIC_KEY_SECRET));
        EcBlockData ecBlockData = new EcBlockData(121, 100_000);
        doReturn(ecBlockData).when(blockchain).getECBlock(0);
        doAnswer(invocation -> {
            String argument = invocation.getArgument(0);
            if ("passphrase".equals(argument)) {
                return SECRET;
            } else if ("account".equals(argument)) {
                return "" + ACCOUNT_ID_WITH_SECRET;
            }
            return "";
        }).when(req).getParameter(anyString());

        MockHttpResponse response = sendPostRequest("/updates", "passphrase=" + SECRET + "&account=" + ACCOUNT_ID_WITH_SECRET + "&manifestUrl=https://test.com&level=CRITICAL&platformSpec=WINDOWS-X86_64,NoOS-ARM64" +
            "&version=1.23.4&cn=https://cn.com&serialNumber=1&signature=111100ff");
        String json = response.getContentAsString();

        JSONAssert.assertEquals("{\"timestamp\": 0, \"attachment\" : {\"level\": 0, \"manifestUrl\":\"https://test.com\", \"platforms\": [{\"platform\": 1, \"architecture\": 1},{\"platform\": -1, \"architecture\": 2}]}, \"type\" : 8, \"subtype\": 3}", json, JSONCompareMode.LENIENT);
        verify(processor).broadcast(any(Transaction.class));
    }

    @Test
    void testSendUpdate_missingSecretPhrase() throws URISyntaxException, UnsupportedEncodingException, AplException.ValidationException {
        Account sender = new Account(ACCOUNT_ID_WITH_SECRET, 10000 * Constants.ONE_APL, 10000 * Constants.ONE_APL, 0, 0, CURRENT_HEIGHT);
        sender.setPublicKey(new PublicKey(sender.getId(), null, 0));

        MockHttpResponse response = sendPostRequest("/updates", "manifestUrl=https://test.com&level=CRITICAL&platformSpec=WINDOWS-AMD64,NoOS-ARM" +
            "&version=1.23.4&cn=https://cn.com&serialNumber=1&signature=111100ff");
        String json = response.getContentAsString();

        JSONAssert.assertEquals("{\"newErrorCode\": 2002, \"errorDescription\" : \"At least one of [secretPhrase,publicKey,passphrase] must be specified.\"}", json, JSONCompareMode.LENIENT);
        verify(processor, never()).broadcast(any(Transaction.class));
    }

    @Test
    void testSendUpdate_missingAccount() throws URISyntaxException, UnsupportedEncodingException, AplException.ValidationException {
        when(elGamal.elGamalDecrypt(SECRET)).thenReturn(SECRET);
        doAnswer(invocation -> {
            String argument = invocation.getArgument(0);
            if ("passphrase".equals(argument)) {
                return SECRET;
            }
            return "";
        }).when(req).getParameter(anyString());

        MockHttpResponse response = sendPostRequest("/updates", "passphrase=" + SECRET + "&manifestUrl=https://test.com&level=CRITICAL&platformSpec=WINDOWS-AMD64,NoOS-ARM" +
            "&version=1.23.4&cn=https://cn.com&serialNumber=1&signature=111100ff");
        String json = response.getContentAsString();

        JSONAssert.assertEquals("{\"newErrorCode\": 2002, \"errorDescription\" : \"At least one of [secretPhrase,publicKey,passphrase] must be specified.\"}", json, JSONCompareMode.LENIENT);
        verify(processor, never()).broadcast(any(Transaction.class));
    }
}