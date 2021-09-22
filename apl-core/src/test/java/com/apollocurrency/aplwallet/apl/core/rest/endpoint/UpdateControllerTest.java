package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.core.model.EcBlockData;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.model.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.rest.converter.UnconfirmedTransactionConverter;
import com.apollocurrency.aplwallet.apl.core.rest.converter.UnconfirmedTransactionConverterCreator;
import com.apollocurrency.aplwallet.apl.core.rest.exception.LegacyParameterExceptionMapper;
import com.apollocurrency.aplwallet.apl.core.rest.utils.AccountParametersParser;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionSignerImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.transaction.CachedTransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.FeeCalculator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.core.transaction.types.update.UpdateV2TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.api.converter.ByteArrayConverterProvider;
import com.apollocurrency.aplwallet.apl.util.api.converter.PlatformSpecConverterProvider;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.service.ElGamalEncryptor;
import com.apollocurrency.aplwallet.vault.model.ApolloFbWallet;
import com.apollocurrency.aplwallet.vault.service.KMSService;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateControllerTest extends AbstractEndpointTest {
    private final TransactionSignerImpl transactionSigner = mock(TransactionSignerImpl.class);
    BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    Chain chain = mock(Chain.class);

    {
        doReturn(chain).when(blockchainConfig).getChain();
    }

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
    ElGamalEncryptor elGamal;
    @Mock
    AccountService accountService;
    @Mock
    KMSService KMSService = mock(KMSService.class);

    @Mock
    UnconfirmedTransactionConverterCreator creator;

    UpdateV2TransactionType v2Transaction;

    @Override
    @BeforeEach
    void setUp() {
        super.setUp();
        v2Transaction = new UpdateV2TransactionType(blockchainConfig, accountService);
        UnconfirmedTransactionConverter converter = new UnconfirmedTransactionConverter(mock(PrunableLoadingService.class));
        when(creator.create(false)).thenReturn(converter);
        CachedTransactionTypeFactory txTypeFactory = new CachedTransactionTypeFactory(List.of(v2Transaction));
        transactionCreator = new TransactionCreator(validator, new PropertiesHolder(), timeService,
            new FeeCalculator(mock(PrunableLoadingService.class), blockchainConfig), blockchain, processor, txTypeFactory,
            new TransactionBuilderFactory(txTypeFactory, blockchainConfig), transactionSigner, blockchainConfig);
        dispatcher.getProviderFactory()
            .register(ByteArrayConverterProvider.class)
            .register(LegacyParameterExceptionMapper.class)
            .register(PlatformSpecConverterProvider.class);
        UpdateController updateController = new UpdateController(new AccountParametersParser(accountService, elGamal, KMSService), transactionCreator, creator, blockchainConfig);
        dispatcher.getRegistry().addSingletonResource(updateController);
        dispatcher.getDefaultContextObjects().put(HttpServletRequest.class, req);
        Convert2.init("TEST", 0);
    }

    @AfterAll
    static void afterAllTests() {
        Convert2.init("APL", 0);
    }

    @Test
    void testSendUpdateSuccessful() throws URISyntaxException, UnsupportedEncodingException, AplException.ValidationException {
        long ONE_APL = 100000000l;
        when(blockchainConfig.getOneAPL()).thenReturn(ONE_APL);
        when(elGamal.elGamalDecrypt(SECRET)).thenReturn(SECRET);
        Account sender = new Account(ACCOUNT_ID_WITH_SECRET, 10000 * ONE_APL, 10000 * ONE_APL, 0, 0, CURRENT_HEIGHT);
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
        mockSigning();

        MockHttpResponse response = sendPostRequest("/updates", "secretPhrase=" + SECRET + "&manifestUrl=https://test11.com&level=CRITICAL&platformSpec=WINDOWS-X86_32,NoOS-ARM" +
            "&version=1.23.4&cn=https://cn345.com&serialNumber=1&signature=111100ff");
        String json = response.getContentAsString();
//        System.out.println("json = \n" + json);

        JSONAssert.assertEquals(json, "{\"timestamp\": 0, \"signature\": \"6f4b6612125fb3a0daecd2799dfd6c9c299424fd920f9b308110a2c1fbd8f443\", \"attachment\" : {\"level\": 0, \"manifestUrl\":\"https://test11.com\", \"platforms\": [{\"platform\": 1, \"architecture\": 0},{\"platform\": -1, \"architecture\": 3}]}, \"type\" : 8, \"subtype\": 3}", json, JSONCompareMode.LENIENT);
        verify(processor).broadcast(any(Transaction.class));
    }

    @Test
    void testSendUpdateSuccessful_usingVault() throws URISyntaxException, UnsupportedEncodingException, AplException.ValidationException {
        long ONE_APL = 100000000l;
        when(blockchainConfig.getOneAPL()).thenReturn(ONE_APL);
        when(elGamal.elGamalDecrypt(SECRET)).thenReturn(SECRET);
        Account sender = new Account(ACCOUNT_ID_WITH_SECRET, 10000 * ONE_APL, 10000 * ONE_APL, 0, 0, CURRENT_HEIGHT);
        sender.setPublicKey(new PublicKey(sender.getId(), null, 0));
        ApolloFbWallet wallet = mock(ApolloFbWallet.class);
        doReturn(Convert.toHexString(Convert.toBytes(SECRET))).when(wallet).getAplKeySecret();
        doReturn(Convert.parseHexString(wallet.getAplKeySecret())).when(KMSService).getAplSecretBytes(ACCOUNT_ID_WITH_SECRET, SECRET);
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
       mockSigning();

        MockHttpResponse response = sendPostRequest("/updates", "passphrase=" + SECRET + "&account=" + ACCOUNT_ID_WITH_SECRET + "&manifestUrl=https://test11.com&level=CRITICAL&platformSpec=WINDOWS-X86_64,NoOS-ARM64" +
            "&version=1.23.4&cn=https://cn345.com&serialNumber=1&signature=111100ff");
        String json = response.getContentAsString();
//        System.out.println("json = \n" + json);

        JSONAssert.assertEquals("{\"requestProcessingTime\":0,\"type\":8,\"subtype\":3,\"phased\":false,\"timestamp\":0,\"deadline\":1440,\"senderPublicKey\":\"" + PUBLIC_KEY_SECRET + "\",\"amountATM\":\"0\",\"feeATM\":\"100000000\",\"attachment\":{\"serialNumber\":\"1\",\"level\":0,\"signature\":\"111100ff\",\"version.UpdateV2\":1,\"manifestUrl\":\"https://test11.com\",\"cn\":\"https://cn345.com\",\"version\":\"1.23.4\",\"platforms\":[{\"platform\":-1,\"architecture\":2},{\"platform\":1,\"architecture\":1}]},\"sender\":\"" + Long.toUnsignedString(ACCOUNT_ID_WITH_SECRET) +"\",\"senderRS\":\"TEST-" + Crypto.rsEncode(AbstractEndpointTest.ACCOUNT_ID_WITH_SECRET)+ "\",\"height\":2147483647,\"version\":1,\"ecBlockId\":\"121\",\"ecBlockHeight\":100000}", json, JSONCompareMode.LENIENT);
        verify(processor).broadcast(any(Transaction.class));
    }

    @Test
    void testSendUpdate_missingSecretPhrase() throws URISyntaxException, UnsupportedEncodingException, AplException.ValidationException {
        Account sender = new Account(ACCOUNT_ID_WITH_SECRET, 10000 * blockchainConfig.getOneAPL(), 10000 * blockchainConfig.getOneAPL(), 0, 0, CURRENT_HEIGHT);
        sender.setPublicKey(new PublicKey(sender.getId(), null, 0));

        MockHttpResponse response = sendPostRequest("/updates", "manifestUrl=https://test11.com&level=CRITICAL&platformSpec=WINDOWS-AMD64,NoOS-ARM" +
            "&version=1.23.4&cn=https://cn345.com&serialNumber=1&signature=111100ff");
        String json = response.getContentAsString();
//        System.out.println("json = \n" + json);

        JSONAssert.assertEquals("{\"newErrorCode\": 2002, \"errorDescription\" : \"At least one of [secretPhrase,publicKey,passphrase] must be specified\"}", json, JSONCompareMode.LENIENT);
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

        JSONAssert.assertEquals("{\"newErrorCode\": 2002, \"errorDescription\" : \"At least one of [secretPhrase,publicKey,passphrase] must be specified\"}", json, JSONCompareMode.LENIENT);
        verify(processor, never()).broadcast(any(Transaction.class));
    }

    private void mockSigning() throws AplException.NotValidException {
        String transactionUnsignedBytes = "6f4b6612125fb3a0daecd2799dfd6c9c299424fd920f9b308110a2c1fbd8f4436f4b6612125fb3a0daecd2799dfd6c9c299424fd920f9b308110a2c1fbd8f4436f4b6612125fb3a0daecd2799dfd6c9c299424fd920f9b308110a2c1fbd8f443";
        PayloadResult signedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
        signedTxBytes.getBuffer().write(Convert.parseHexString(transactionUnsignedBytes));
        doAnswer(invocation-> {
            Signature sig = mock(Signature.class);
            doReturn(Convert.parseHexString("6f4b6612125fb3a0daecd2799dfd6c9c299424fd920f9b308110a2c1fbd8f443")).when(sig).bytes();
            lenient().doReturn("6f4b6612125fb3a0daecd2799dfd6c9c299424fd920f9b308110a2c1fbd8f443").when(sig).getHexString();
            ((TransactionImpl) invocation.getArgument(0)).sign(sig, signedTxBytes);
            return null;
        }).when(transactionSigner).sign(any(Transaction.class), any(byte[].class));
    }
}